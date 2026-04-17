package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.ShaclGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates SHACL shapes from the ontology.
 */
public class ShaclGenerator extends BaseGenerator {

  private static final String SH = "http://www.w3.org/ns/shacl#";
  private final ShaclGeneratorProperties shaclGeneratorProperties;
  private final Logger logger = LoggerFactory.getLogger(ShaclGenerator.class);

  /**
   * Represents the unique signature of a property shape for duplicate detection.
   */
  private record PropertyShapeSignature(String path, ValueKind valueKind, Integer minCount,
                                        Integer maxCount) {

  }

  /**
   * Represents the value constraint kind (class, datatype, or union).
   */
  private interface ValueKind {

  }

  private record SingleValue(String uri, boolean isDatatype) implements ValueKind {

  }

  private static class OrValue implements ValueKind {

    private final List<MemberValue> members;

    public OrValue(List<MemberValue> members) {
      // Sort members to ensure consistent comparison
      this.members = members.stream()
          .sorted(Comparator.comparing(m -> m.uri))
          .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OrValue orValue = (OrValue) o;
      return Objects.equals(members, orValue.members);
    }

    @Override
    public int hashCode() {
      return Objects.hash(members);
    }
  }

  private record MemberValue(String uri, boolean isDatatype) {

  }

  public ShaclGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      ShaclGeneratorProperties shaclGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
    this.shaclGeneratorProperties = shaclGeneratorProperties;
  }

  @Override
  public String getName() {
    return "shacl";
  }

  @Override
  public String getDescription() {
    return "Generates SHACL shapes from the ontology";
  }

  @Override
  public void run() {
    super.run();
    Model shacl = generateShacl();
    if (getOutputFile() != null) {
      logger.info("Writing SHACL shapes to {}", getOutputFile());
      saveToFile(getOutputFile(), shacl);
    } else {
      shacl.write(System.out, "TURTLE");
    }
  }

  protected void saveToFile(String outputFile, Model shacl) {
    try {
      Path outputPath = java.nio.file.Paths.get(outputFile);
      Files.createDirectories(outputPath.getParent());
      shacl.write(new FileOutputStream(outputFile), "TURTLE");
    } catch (Exception e) {
      logger.error("Error writing SHACL shapes to file: {}", e.getMessage(), e);
    }
  }

  protected String getOutputFile() {
    return shaclGeneratorProperties.getOutputFile();
  }

  /**
   * Generates SHACL shapes from the ontology model. Uses the raw model (inferredModel is not
   * consulted here; callers may substitute it via {@link OntologyInfo#setModel(Model)}).
   */
  private Model generateShacl() {
    Model ontology = ontologyInfo.getModel();
    if (ontology == null) {
      return ModelFactory.createDefaultModel();
    }

    Model shacl = ModelFactory.createDefaultModel();
    shacl.setNsPrefix("sh", SH);
    shacl.setNsPrefix("owl", OWL.NS);
    shacl.setNsPrefix("rdfs", RDFS.getURI());
    // Copy all prefixes from the ontology in key order for deterministic output
    ontology.getNsPrefixMap().entrySet().stream()
        .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo)))
        .forEach(entry -> shacl.setNsPrefix(entry.getKey(), entry.getValue()));

    List<Resource> classes = ontology.listResourcesWithProperty(RDF.type, OWL.Class)
        .toList()
        .stream()
        .filter(Resource::isURIResource)
        .sorted(Comparator.comparing(Resource::getURI, Comparator.nullsLast(String::compareTo)))
        .toList();
    classes.forEach(cls -> generateNodeShape(cls, ontology, shacl));

    return shacl;
  }

  private Property shaclProp(String local, Model m) {
    return m.createProperty(SH + local);
  }

  private boolean isDatatype(Resource res) {
    return res != null && res.isURIResource() && res.getURI()
        .startsWith("http://www.w3.org/2001/XMLSchema#");
  }

  private RDFNode createPath(Resource prop, Model shacl) {
    // Handle anonymous nodes with owl:inverseOf
    if (prop.isAnon()) {
      Statement invStmt = prop.getProperty(OWL.inverseOf);
      if (invStmt != null && invStmt.getObject().isResource()) {
        Resource inv = invStmt.getResource();
        if (inv.isURIResource()) {
          Resource b = shacl.createResource();
          b.addProperty(shaclProp("inversePath", shacl), inv);
          return b;
        }
      }
    }
    // if prop has owl:inverseOf, return a blank node with sh:inversePath
    Statement invStmt = prop.getProperty(OWL.inverseOf);
    if (invStmt != null && invStmt.getObject().isResource()) {
      Resource inv = invStmt.getResource();
      Resource b = shacl.createResource();
      b.addProperty(shaclProp("inversePath", shacl), inv);
      return b;
    }
    if (prop.isURIResource()) {
      return shacl.createResource(prop.getURI());
    }
    return shacl.createResource();
  }

  /**
   * Compute the signature of a restriction for duplicate detection. Returns Optional.empty() if the
   * restriction is invalid or incomplete.
   */
  private Optional<PropertyShapeSignature> computeSignature(Resource restriction) {
    Resource onProp = restriction.getPropertyResourceValue(OWL.onProperty);
    if (onProp == null || !onProp.isURIResource()) {
      return Optional.empty();
    }

    String path = onProp.getURI();

    Resource some = restriction.getPropertyResourceValue(OWL.someValuesFrom);
    Resource all = restriction.getPropertyResourceValue(OWL.allValuesFrom);
    Resource valueNode = some != null ? some : all;

    ValueKind valueKind;
    if (valueNode != null && valueNode.hasProperty(OWL.unionOf)) {
      // Handle union
      Statement unionStmt = valueNode.getProperty(OWL.unionOf);
      Resource unionRes = unionStmt != null && unionStmt.getObject().isResource()
          ? unionStmt.getResource()
          : valueNode;

      if (unionRes.canAs(RDFList.class)) {
        RDFList list = unionRes.as(RDFList.class);
        List<MemberValue> members = new ArrayList<>();
        Iterator<RDFNode> it = list.iterator();
        while (it.hasNext()) {
          RDFNode member = it.next();
          if (member.isResource() && member.asResource().isURIResource()) {
            Resource r = member.asResource();
            members.add(new MemberValue(r.getURI(), isDatatype(r)));
          }
        }
        valueKind = new OrValue(members);
      } else {
        valueKind = new SingleValue("__none__", false);
      }
    } else if (valueNode != null && valueNode.isURIResource()) {
      // Single value
      valueKind = new SingleValue(valueNode.getURI(), isDatatype(valueNode));
    } else {
      // No value constraint
      valueKind = new SingleValue("__none__", false);
    }

    Integer min = intValue(restriction, OWL.cardinality);
    if (min == null) {
      min = intValue(restriction, OWL.minCardinality);
    }
    if (min == null && restriction.hasProperty(OWL.someValuesFrom)
        && !restriction.hasProperty(OWL.minCardinality)
        && !restriction.hasProperty(OWL.cardinality)) {
      min = 1;
    }

    Integer max = intValue(restriction, OWL.cardinality);
    if (max == null) {
      max = intValue(restriction, OWL.maxCardinality);
    }

    return Optional.of(new PropertyShapeSignature(path, valueKind, min, max));
  }

  private void addClassOrDatatype(Resource ps, Resource value, Model shacl) {
    if (isDatatype(value)) {
      ps.addProperty(shaclProp("datatype", shacl), value);
    } else {
      ps.addProperty(shaclProp("class", shacl), value);
    }
  }

  private void addMinCountIfNeeded(Resource restriction, Resource ps, Model shacl) {
    if (restriction.hasProperty(OWL.someValuesFrom)
        && !restriction.hasProperty(OWL.minCardinality)
        && !restriction.hasProperty(OWL.cardinality)) {
      ps.addLiteral(shaclProp("minCount", shacl), 1);
    }
  }

  private void addCardinality(Resource restriction, Resource ps, Model shacl) {
    Integer min = intValue(restriction, OWL.minCardinality);
    Integer max = intValue(restriction, OWL.maxCardinality);
    Integer exact = intValue(restriction, OWL.cardinality);

    if (min != null) {
      ps.addLiteral(shaclProp("minCount", shacl), min);
    }
    if (max != null) {
      ps.addLiteral(shaclProp("maxCount", shacl), max);
    }
    if (exact != null) {
      ps.addLiteral(shaclProp("minCount", shacl), exact);
      ps.addLiteral(shaclProp("maxCount", shacl), exact);
    }
  }

  private Integer intValue(Resource res, Property p) {
    Statement s = res.getProperty(p);
    if (s == null) {
      return null;
    }
    RDFNode node = s.getObject();
    if (node.isLiteral()) {
      try {
        return ((Literal) node).getInt();
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private RDFNode createOrList(Resource unionNode, Model shacl) {
    Resource nodeWithUnion = unionNode;
    Statement unionStmt = unionNode.getProperty(OWL.unionOf);
    if (unionStmt != null && unionStmt.getObject().isResource()) {
      nodeWithUnion = unionStmt.getResource();
    }
    // nodeWithUnion should be an RDFList or a node with rdf:first/rdf:rest
    if (nodeWithUnion.canAs(RDFList.class)) {
      RDFList list = nodeWithUnion.as(RDFList.class);
      List<Resource> members = new ArrayList<>();
      Iterator<RDFNode> it = list.iterator();
      while (it.hasNext()) {
        RDFNode member = it.next();
        if (member.isResource() && member.asResource().isURIResource()) {
          members.add(member.asResource());
        }
      }
      List<RDFNode> shapes = members.stream()
          .sorted(Comparator.comparing(Resource::getURI, Comparator.nullsLast(String::compareTo)))
          .map(resource -> {
            Resource ps = shacl.createResource();
            addClassOrDatatype(ps, resource, shacl);
            return (RDFNode) ps;
          })
          .toList();
      return shacl.createList(shapes.iterator());
    }
    return shacl.createResource();
  }

  private void generatePropertyShape(Resource restriction, Model shacl, Resource nodeShape) {
    Resource onProp = restriction.getPropertyResourceValue(OWL.onProperty);
    if (onProp == null) {
      return;
    }

    Resource ps = shacl.createResource();
    ps.addProperty(shaclProp("path", shacl), createPath(onProp, shacl));

    Resource some = restriction.getPropertyResourceValue(OWL.someValuesFrom);
    Resource all = restriction.getPropertyResourceValue(OWL.allValuesFrom);

    if (some != null) {
      if (some.hasProperty(OWL.unionOf)) {
        ps.addProperty(shaclProp("or", shacl), createOrList(some, shacl));
      } else {
        addClassOrDatatype(ps, some, shacl);
      }
    }

    if (all != null) {
      if (all.hasProperty(OWL.unionOf)) {
        ps.addProperty(shaclProp("or", shacl), createOrList(all, shacl));
      } else {
        addClassOrDatatype(ps, all, shacl);
      }
    }

    addMinCountIfNeeded(restriction, ps, shacl);
    addCardinality(restriction, ps, shacl);
    addComment(restriction, ps, shacl);

    nodeShape.addProperty(shaclProp("property", shacl), ps);
  }

  private void addComment(Resource from, Resource to, Model shacl) {
    Statement commentStmt = from.getProperty(RDFS.comment);
    if (commentStmt != null && commentStmt.getObject().isLiteral()) {
      to.addProperty(shacl.createProperty(SH + "description"), commentStmt.getLiteral());
    }
  }

  private void generateNodeShape(Resource cls, Model ontology, Model shacl) {
    Resource ns = shacl.createResource(cls.getURI() + "Shape");
    ns.addProperty(RDF.type, shacl.createResource(SH + "NodeShape"));
    ns.addProperty(shaclProp("targetClass", shacl), cls);

    Set<PropertyShapeSignature> seen = new HashSet<>();

    // find rdfs:subClassOf values that are OWL.Restriction
    List<Statement> restrictions = ontology.listStatements(cls, RDFS.subClassOf, (RDFNode) null)
        .toList()
        .stream()
        .filter(st -> st.getObject().isResource())
        .filter(st -> st.getObject().asResource().hasProperty(RDF.type, OWL.Restriction))
        .sorted(Comparator.comparing(
            st -> {
              Resource restriction = st.getObject().asResource();
              Resource onProp = restriction.getPropertyResourceValue(OWL.onProperty);
              return onProp != null && onProp.isURIResource() ? onProp.getURI() : "";
            },
            Comparator.nullsLast(String::compareTo)))
        .toList();

    restrictions.forEach(st -> {
      Resource restriction = st.getObject().asResource();
      Optional<PropertyShapeSignature> sig = computeSignature(restriction);
      if (sig.isPresent() && !seen.contains(sig.get())) {
        generatePropertyShape(restriction, shacl, ns);
        seen.add(sig.get());
      }
    });
  }
}
