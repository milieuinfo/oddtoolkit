package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-load.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyLoadAdapter extends AbstractAdapter<OntologyInfo> {

  public OntologyLoadAdapter() {
    super(OntologyInfo.class);
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // Load the ontology from the source
    Model model = ModelFactory.createDefaultModel();
    model.read(info.getConfig().getOntologyFilePath());
    info.setModel(model);
    return info;
  }
}
