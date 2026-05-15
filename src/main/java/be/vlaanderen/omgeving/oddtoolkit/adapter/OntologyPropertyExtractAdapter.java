package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo.Cardinality;
import be.vlaanderen.omgeving.oddtoolkit.model.UriTemplate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AdapterDependency({
    OntologyClassExtractAdapter.class,
    OntologyUriTemplateAdapter.class
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-property-extract.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyPropertyExtractAdapter extends AbstractAdapter<OntologyInfo> {

  public static final Logger logger = LoggerFactory.getLogger(OntologyPropertyExtractAdapter.class);

  public OntologyPropertyExtractAdapter() {
    super(OntologyInfo.class);
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    List<ClassInfo> allClasses = info.getClasses(); // snapshot already returned by getClasses()
    for (ClassInfo ci : new ArrayList<>(allClasses)) {
      this.extractProperties(ci);
      this.determineIdentifiers(ci);
      this.determineInverseProperties(ci, info);
    }
    return info;
  }

  private void determineInverseProperties(ClassInfo classInfo, OntologyInfo ontologyInfo) {
    // Loop through the properties and find the URI of the inverse property (if any) and set it in the property info
    // Use the inferred model to find the inverse properties based on the owl:inverseOf property
    // Fall back to the base model when the OWL reasoner is disabled (inferredModel is null)
    Model inferredModel = ontologyInfo.getInferredModel() != null
        ? ontologyInfo.getInferredModel()
        : ontologyInfo.getModel();
    classInfo.getProperties().forEach(propertyInfo -> {
      Resource propertyResource = propertyInfo.getResource();
      if (propertyResource != null) {
        inferredModel.listStatements(propertyResource, OWL2.inverseOf, (Resource) null)
            .forEachRemaining(statement -> {
              Resource inversePropertyResource = statement.getObject().asResource();
              if (inversePropertyResource != null && inversePropertyResource.getURI() != null) {
                propertyInfo.setInverseOf(inversePropertyResource.getURI());
                logger.debug("Set inverse property {} for property {} in class {}", inversePropertyResource.getURI(), propertyInfo.getUri(), classInfo.getUri());
              }
            });
      }
    });
  }

  private void determineIdentifiers(ClassInfo classInfo) {
    UriTemplate uriTemplate = classInfo.getUriTemplate();
    if (uriTemplate != null) {
      // Extract identifiers from the URI template variable mappings
      uriTemplate.getVariables().forEach((variableName, propertyUri) -> {
        // Find the corresponding property in the class properties
        PropertyInfo propertyInfo = classInfo.getPropertyByUri(propertyUri);
        if (propertyInfo != null) {
          propertyInfo.setIdentifier(true);
          logger.debug("Marked property {} as identifier for class {}", propertyUri, classInfo.getUri());
        } else {
          // Add the property as an identifier if it is not already present in the class properties
          Resource propertyResource = classInfo.getResource().getModel().createResource(propertyUri);
          PropertyInfo newPropertyInfo = new PropertyInfo(classInfo.getScope(), propertyResource);
          newPropertyInfo.setIdentifier(true);
          newPropertyInfo.setCardinalityTo(new Cardinality());
          newPropertyInfo.getCardinalityTo().setMax(1);
          newPropertyInfo.getCardinalityTo().setMin(1);
          classInfo.getProperties().add(newPropertyInfo);
          logger.debug("Added property {} as identifier for class {}", propertyUri, classInfo.getUri());
        }
      });
    }
  }

  private void extractProperties(ClassInfo classInfo) {
    Resource classResource = classInfo.getResource();
    // Extract properties defined in the owl restrictions
    classResource.listProperties(RDFS.subClassOf).forEachRemaining(statement -> {
      Resource statementResource = statement.getObject().asResource();
      if (statementResource != null && statementResource.hasProperty(RDF.type, OWL2.Restriction)) {
        statementResource.listProperties(OWL2.onProperty)
            .forEachRemaining(propertyStatement -> {
              Resource propertyResource = propertyStatement.getObject().asResource();
              if (propertyResource != null && propertyResource.getURI() != null) {
                classInfo.getProperties()
                    .add(
                        new PropertyInfo(classInfo.getScope(), propertyResource, statementResource));
              }
            });
      }
    });
    // Extract properties as object or data properties within the ontology
    classResource.listProperties(RDF.type).forEachRemaining(statement -> {
      Resource statementResource = statement.getSubject().asResource();
      if (statementResource != null && statementResource.getURI() != null && (
          statementResource.hasProperty(RDF.type, OWL2.ObjectProperty)
              || statementResource.hasProperty(RDF.type, OWL2.DatatypeProperty))) {
        classInfo.getProperties()
            .add(new PropertyInfo(classInfo.getScope(), statementResource));
      }
    });

    combinePropertiesByUri(classInfo);
  }

  private void combinePropertiesByUri(ClassInfo classInfo) {
    Map<String, PropertyInfo> combinedByUri = new LinkedHashMap<>();
    for (PropertyInfo property : classInfo.getProperties()) {
      if (property == null || property.getUri() == null) {
        continue;
      }
      PropertyInfo existing = combinedByUri.get(property.getUri());
      if (existing == null) {
        combinedByUri.put(property.getUri(), property);
      } else {
        mergeProperty(existing, property);
      }
    }
    classInfo.setProperties(new ArrayList<>(combinedByUri.values()));
  }

  private void mergeProperty(PropertyInfo target, PropertyInfo source) {
    if (target.getName() == null && source.getName() != null) {
      target.setName(source.getName());
    }
    if (target.getComment() == null && source.getComment() != null) {
      target.setComment(source.getComment());
    }
    if (target.getInverseOf() == null && source.getInverseOf() != null) {
      target.setInverseOf(source.getInverseOf());
    }

    if (source.getRange() != null) {
      source.getRange().stream()
          .filter(rangeUri -> rangeUri != null && !target.getRange().contains(rangeUri))
          .forEach(target.getRange()::add);
    }

    if (target.getCardinalityTo() == null) {
      target.setCardinalityTo(new Cardinality());
    }
    if (source.getCardinalityTo() == null) {
      source.setCardinalityTo(new Cardinality());
    }

    // Merge cardinality constraints: strongest lower bound and strongest upper bound.
    Integer sourceMin = source.getCardinalityTo().getMin();
    Integer sourceMax = source.getCardinalityTo().getMax();
    Integer targetMin = target.getCardinalityTo().getMin();
    Integer targetMax = target.getCardinalityTo().getMax();

    if (sourceMin != null && (targetMin == null || sourceMin > targetMin)) {
      target.getCardinalityTo().setMin(sourceMin);
    }
    if (sourceMax != null && (targetMax == null || sourceMax < targetMax)) {
      target.getCardinalityTo().setMax(sourceMax);
    }

    target.setIdentifier(target.isIdentifier() || source.isIdentifier());
  }
}
