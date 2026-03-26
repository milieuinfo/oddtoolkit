package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

@ConditionalOnConfigProperty(prefix = "adapters", name = "concept-scheme-load.enabled", havingValue = "true", matchIfMissing = true)
public class ConceptSchemeLoadAdapter extends AbstractAdapter<ConceptSchemeInfo> {

  public ConceptSchemeLoadAdapter() {
    super(ConceptSchemeInfo.class);
  }

  @Override
  public ConceptSchemeInfo adapt(ConceptSchemeInfo info) {
    // Load the concept scheme from the source
    Model model = ModelFactory.createDefaultModel();
    model.read(info.getConfig().getConceptsFilePath());
    info.setModel(model);
    return info;
  }
}
