package be.vlaanderen.omgeving.oddtoolkit.model;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;

public class ClassConceptInfo extends ConceptInfo {

  public ClassConceptInfo(Scope scope, Resource resource) {
    super(scope, resource);
  }

  @Override
  protected void initializeFromResource(Resource resource) {
    super.initializeFromResource(resource);
    if (resource.hasProperty(OWL2.equivalentClass)) {
      resource.listProperties(OWL2.equivalentClass).forEachRemaining(stmt -> {
        if (stmt.getObject().isResource()) {
          getEquivalents().add(stmt.getObject().asResource().getURI());
        }
      });
    }
  }
}
