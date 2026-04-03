package be.vlaanderen.omgeving.oddtoolkit.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
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
    if (resource.hasProperty(OWL2.maxCardinality)) {
      cardinalityTo.max = resource.getProperty(OWL2.maxCardinality).getInt();
    }
    if (resource.hasProperty(OWL2.minCardinality)) {
      cardinalityTo.min = resource.getProperty(OWL2.minCardinality).getInt();
    }
    if (resource.hasProperty(OWL2.cardinality)) {
      int exactCardinality = resource.getProperty(OWL2.cardinality).getInt();
      cardinalityTo.min = exactCardinality;
      cardinalityTo.max = exactCardinality;
    }
  }

  private void addRestrictedRangeValues(Resource resource) {
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
      if (uri != null) {
        range.add(uri);
      }
    }
  }

  private void addRequiredRangeValue(Statement statement) {
    if (!statement.getObject().isResource() || statement.getObject().asResource().getURI() == null) {
      throw new IllegalArgumentException(
          "Invalid range for property: " + getUri() + ". Range must be a resource with a valid URI. Found: "
              + statement.getObject());
    }
    range.add(statement.getObject().asResource().getURI());
  }

  @Getter
  @Setter
  public static class Cardinality {

    private Integer min;
    private Integer max;
  }
}
