package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.util.RdfFormat;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-load.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyLoadAdapter extends AbstractAdapter<OntologyInfo> {

  public OntologyLoadAdapter() {
    super(OntologyInfo.class);
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // Load the ontology from the source (local path or URL)
    String path = info.getConfig().getOntologyFilePath();
    Model model = ModelFactory.createDefaultModel();
    RdfFormat.read(model, path);
    info.setModel(model);
    return info;
  }
}
