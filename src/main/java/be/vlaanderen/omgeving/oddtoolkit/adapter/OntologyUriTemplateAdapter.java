package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import be.vlaanderen.omgeving.oddtoolkit.model.UriTemplate;

@AdapterDependency({
    OntologyClassExtractAdapter.class,
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-uri-template.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyUriTemplateAdapter extends AbstractAdapter<OntologyInfo> {

  public OntologyUriTemplateAdapter() {
    super(OntologyInfo.class);
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // For each class search for the Hydra search template
    info.getClasses().stream()
        .filter(c -> c.getScope().equals(Scope.ONTOLOGY))
        .filter(c -> c.getResource().hasProperty(info.getModel().getProperty("http://www.w3.org/ns/hydra/core#search")))
        .forEach(this::extractUriTemplate);
    return info;
  }

  private void extractUriTemplate(ClassInfo classInfo) {
    // Extract the template and variable mappings
    var searchProperty = classInfo.getResource().getProperty(
        classInfo.getResource().getModel().getProperty("http://www.w3.org/ns/hydra/core#search"));
    if (searchProperty == null) {
      return;
    }
    var searchResource = searchProperty.getResource();
    UriTemplate uriTemplate = new UriTemplate(Scope.ONTOLOGY, searchResource);
    classInfo.setUriTemplate(uriTemplate);
  }
}
