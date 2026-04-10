package be.vlaanderen.omgeving.oddtoolkit.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

@Getter
@Setter
public class PropertyInfo extends AbstractInfo {
  private String inverseOf;
  private boolean isIdentifier;
  private Cardinality cardinalityTo;
  private Cardinality cardinalityFrom;
  private List<String> range;

  public PropertyInfo(Scope scope, Resource resource, Resource parent) {
    super(scope, resource);
    cardinalityTo = new Cardinality();
    cardinalityFrom = new Cardinality();
    range = new ArrayList<>();
    if (parent != null) {
      initializeProperty(parent);
    }
  }

  public PropertyInfo(Scope scope, Resource resource) {
    this(scope, resource, null);
  }

  protected void initializeProperty(Resource resource) {
    if (getComment() == null && resource.hasProperty(RDFS.comment)) {
      setComment(resource.getProperty(RDFS.comment).getString());
    }

    if (isPropertyResource(resource)) {
      setUri(resource.getURI());
      setName(resource.getLocalName());
      addRangeValues(resource, RDFS.range);
    }

    updateCardinality(resource);
    addRestrictedRangeValues(resource);

    if (getUri() == null) {
      throw new IllegalArgumentException("Property URI cannot be null for resource: \n" + this);
    }
  }

  private boolean isPropertyResource(Resource resource) {
    return resource.hasProperty(RDF.type, RDF.Property)
        || resource.hasProperty(RDF.type, OWL2.ObjectProperty)
        || resource.hasProperty(RDF.type, OWL2.DatatypeProperty);
  }

  private void updateCardinality(Resource resource) {
    Property onClass = resource.getModel().createProperty(OWL.NS + "onClass");
    Property onDataRange = resource.getModel().createProperty(OWL.NS + "onDataRange");
    boolean qualifiedRestriction = resource.hasProperty(OWL2.onClass)
        || resource.hasProperty(OWL2.onDataRange)
        || resource.hasProperty(onClass)
        || resource.hasProperty(onDataRange);

    if (!qualifiedRestriction) {
      // Unqualified cardinality constraints apply to the full property.
      applyMaxConstraint(resource, OWL2.maxCardinality);
      applyMinConstraint(resource, OWL2.minCardinality);
      applyExactConstraint(resource, OWL2.cardinality);
    }

    // Qualified constraints are class/data-range scoped. For a property-level model we only
    // safely derive lower bounds; upper bounds are not globally constrained by maxQualified.
    applyMinConstraint(resource, OWL2.minQualifiedCardinality);
    applyMinConstraint(resource, OWL2.qualifiedCardinality);
  }

  private void applyMinConstraint(Resource resource, Property property) {
    if (resource.hasProperty(property)) {
      int value = resource.getProperty(property).getInt();
      if (cardinalityTo.min == null || value > cardinalityTo.min) {
        cardinalityTo.min = value;
      }
    }
  }

  private void applyMaxConstraint(Resource resource, Property property) {
    if (resource.hasProperty(property)) {
      int value = resource.getProperty(property).getInt();
      if (cardinalityTo.max == null || value < cardinalityTo.max) {
        cardinalityTo.max = value;
      }
    }
  }

  private void applyExactConstraint(Resource resource, Property property) {
    if (resource.hasProperty(property)) {
      int exactCardinality = resource.getProperty(property).getInt();
      cardinalityTo.min = exactCardinality;
      cardinalityTo.max = exactCardinality;
    }
  }

  private void addRestrictedRangeValues(Resource resource) {
    // Qualified restriction shape: owl:onClass / owl:onDataRange
    Property onClass = resource.getModel().createProperty(OWL.NS + "onClass");
    Property onDataRange = resource.getModel().createProperty(OWL.NS + "onDataRange");
    addRangeValues(resource, OWL2.onClass);
    addRangeValues(resource, OWL2.onDataRange);
    addRangeValues(resource, onClass);
    addRangeValues(resource, onDataRange);

    if (resource.hasProperty(OWL2.someValuesFrom)) {
      resource.listProperties(OWL2.someValuesFrom).forEachRemaining(this::addRequiredRangeValue);
      return;
    }
    addRangeValues(resource, OWL2.allValuesFrom);
  }

  private void addRangeValues(Resource resource, Property property) {
    resource.listProperties(property).forEachRemaining(this::addRangeValueIfPresent);
  }

  private void addRangeValueIfPresent(Statement statement) {
    if (statement.getObject().isResource()) {
      String uri = statement.getObject().asResource().getURI();
      addRangeIfAbsent(uri);
    }
  }

  private void addRequiredRangeValue(Statement statement) {
    if (!statement.getObject().isResource()) {
      // Literal values are not valid range resources; skip silently
      return;
    }
    Resource object = statement.getObject().asResource();
    if (object.isURIResource()) {
      addRangeIfAbsent(object.getURI());
    } else if (object.canAs(RDFList.class)) {
      // Turtle RDF collection syntax: owl:someValuesFrom (:A :B) — a direct rdf:List, no unionOf wrapper
      addRdfListRangeValues(object);
    } else {
      // Blank node: try to extract members from owl:unionOf (anonymous union class expression)
      addUnionRangeValues(object);
    }
  }

  private void addRangeIfAbsent(String uri) {
    if (uri != null && !range.contains(uri)) {
      range.add(uri);
    }
  }

  /**
   * Extracts URI members from a direct RDF list (Turtle collection syntax, e.g. {@code (:A :B)})
   * and adds them to the range. Non-URI members are silently skipped.
   */
  private void addRdfListRangeValues(Resource listNode) {
    listNode.as(RDFList.class).iterator().forEachRemaining((RDFNode member) -> {
      if (member.isResource() && member.asResource().isURIResource()) {
        addRangeIfAbsent(member.asResource().getURI());
      }
    });
  }

  /**
   * Extracts URI members from an anonymous owl:unionOf class expression and adds them to the range.
   * Non-URI members (e.g. nested blank nodes) are silently skipped.
   */
  private void addUnionRangeValues(Resource blankNode) {
    Statement unionStmt = blankNode.getProperty(OWL.unionOf);
    if (unionStmt == null || !unionStmt.getObject().isResource()) {
      // Not a union expression; nothing useful to extract
      return;
    }
    Resource listNode = unionStmt.getResource();
    if (!listNode.canAs(RDFList.class)) {
      return;
    }
    listNode.as(RDFList.class).iterator().forEachRemaining((RDFNode member) -> {
      if (member.isResource() && member.asResource().isURIResource()) {
        addRangeIfAbsent(member.asResource().getURI());
      }
    });
  }

  @Getter
  @Setter
  public static class Cardinality {

    private Integer min;
    private Integer max;
  }
}
