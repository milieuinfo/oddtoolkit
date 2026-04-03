package be.vlaanderen.omgeving.oddtoolkit.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

@Getter
@Setter
public class UriTemplate extends AbstractInfo {

  private static final String HYDRA_NS = "http://www.w3.org/ns/hydra/core#";

  private String template;
  private Map<String, String> variables;

  public UriTemplate(Scope scope, Resource resource) {
    super(scope, resource);
  }

  public void initializeFromResource(Resource resource) {
    super.initializeFromResource(resource);
    variables = new HashMap<>();

    Property hydraTemplate = resource.getModel().getProperty(HYDRA_NS + "template");
    Statement templateStatement = resource.getProperty(hydraTemplate);
    if (templateStatement != null) {
      this.template = templateStatement.getString();
    }

    Property hydraMapping = resource.getModel().getProperty(HYDRA_NS + "mapping");
    Property hydraVariable = resource.getModel().getProperty(HYDRA_NS + "variable");
    Property hydraProperty = resource.getModel().getProperty(HYDRA_NS + "property");
    resource.listProperties(hydraMapping).forEachRemaining(stmt -> mapVariable(stmt, hydraVariable, hydraProperty));
  }

  private void mapVariable(Statement statement, Property hydraVariable, Property hydraProperty) {
    if (!statement.getObject().isResource()) {
      return;
    }

    Resource mappingResource = statement.getObject().asResource();
    Statement variableStatement = mappingResource.getProperty(hydraVariable);
    Statement propertyStatement = mappingResource.getProperty(hydraProperty);
    if (variableStatement == null || propertyStatement == null
        || !propertyStatement.getObject().isResource()) {
      return;
    }

    String propertyUri = propertyStatement.getObject().asResource().getURI();
    if (propertyUri != null) {
      variables.put(variableStatement.getString(), propertyUri);
    }
  }
}
