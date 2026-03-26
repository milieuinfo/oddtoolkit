package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

@AdapterDependency({
    OntologyClassExtractAdapter.class,
    ConceptSchemeLoadAdapter.class,
    ConceptSchemeExtractAdapter.class
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "concept-class-extract.enabled", havingValue = "true", matchIfMissing = true)
public class ConceptClassExtractAdapter extends AbstractAdapter<OntologyInfo> {

  private final OntologyInfo ontologyInfo;

  public ConceptClassExtractAdapter(OntologyInfo ontologyInfo) {
    super(OntologyInfo.class);
    this.ontologyInfo = ontologyInfo;
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    ConceptSchemeInfo conceptSchemeInfo = ontologyInfo.getConcepts();
    // Add all concept classes that are not already present in the ontology info
    if (conceptSchemeInfo.getClassConcepts() == null) {
      return info;
    }
    conceptSchemeInfo.getClassConcepts().forEach(concept -> {
      if (info.getClassByUri(concept.getUri()) == null) {
        ClassInfo classInfo = new ClassInfo(Scope.ONTOLOGY, concept.getResource());
        classInfo.setUri(concept.getEquivalents().getFirst());
        extractProperties(info, classInfo, conceptSchemeInfo);
        info.addClass(classInfo);
      }
    });
    return info;
  }

  private void extractProperties(OntologyInfo info, ClassInfo classInfo,
      ConceptSchemeInfo conceptSchemeInfo) {
    // Extract all properties from the inferred model that have as
    // a domain the class and add them to the class info (if not already present)

    // Create resource from classinfo URI
    Resource objectResource = info.getInferredModel().createResource(classInfo.getUri());
    info.getInferredModel().listStatements(null, RDFS.domain, objectResource)
        .forEachRemaining(statement -> {
          // Extract the property and add it to the class info
          Property property = statement.getSubject().as(Property.class);
          if (classInfo.getProperties().stream().noneMatch(p -> p.getUri().equals(property.getURI()))) {
            PropertyInfo propertyInfo = new PropertyInfo(classInfo.getScope(), property);
            propertyInfo.getCardinalityTo().setMax(1);
            propertyInfo.getCardinalityTo().setMin(0);
            classInfo.getProperties().add(propertyInfo);
          }
        });
    // Filter properties, only properties that have an equivalent concept property should be included
    classInfo.getProperties().removeIf(propertyInfo -> conceptSchemeInfo.getPropertyConcepts().stream()
        .noneMatch(conceptProperty -> conceptProperty.getEquivalents().contains(propertyInfo.getUri())));
  }
}
