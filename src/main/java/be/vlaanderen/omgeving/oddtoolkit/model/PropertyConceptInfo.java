package be.vlaanderen.omgeving.oddtoolkit.model;

import lombok.Getter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;

@Getter
public class PropertyConceptInfo extends ConceptInfo {

  public PropertyConceptInfo(Scope scope, Resource resource) {
    super(scope, resource);
  }

  @Override
  protected void initializeFromResource(Resource resource) {
    super.initializeFromResource(resource);
    if (resource.hasProperty(OWL2.equivalentProperty)) {
      resource.listProperties(OWL2.equivalentProperty).forEachRemaining(stmt -> {
        if (stmt.getObject().isResource()) {
          getEquivalents().add(stmt.getObject().asResource().getURI());
        }
      });
    }
    if (resource.hasProperty(OWL2.equivalentClass)) {
      resource.listProperties(OWL2.equivalentClass).forEachRemaining(stmt -> {
        if (stmt.getObject().isResource()) {
          getEquivalents().add(stmt.getObject().asResource().getURI());
        }
      });
    }
  }
}
