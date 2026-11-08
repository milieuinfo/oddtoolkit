package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.util.RdfFormat;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

@ConditionalOnConfigProperty(prefix = "adapters", name = "concept-scheme-load.enabled", havingValue = "true", matchIfMissing = true)
public class ConceptSchemeLoadAdapter extends AbstractAdapter<ConceptSchemeInfo> {

  public ConceptSchemeLoadAdapter() {
    super(ConceptSchemeInfo.class);
  }

  @Override
  public ConceptSchemeInfo adapt(ConceptSchemeInfo info) {
    // Load the concept scheme from the source (local path or URL)
    String path = info.getConfig().getConceptsFilePath();
    Model model = ModelFactory.createDefaultModel();
    RdfFormat.read(model, path);
    info.setModel(model);
    return info;
  }
}
