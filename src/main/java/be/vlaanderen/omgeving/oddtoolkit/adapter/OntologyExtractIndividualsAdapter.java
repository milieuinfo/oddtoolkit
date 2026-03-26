package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AdapterDependency({
    OntologyClassExtractAdapter.class,
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-individuals-extract.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyExtractIndividualsAdapter extends AbstractAdapter<OntologyInfo> {

  public static final Logger logger = LoggerFactory.getLogger(
      OntologyExtractIndividualsAdapter.class);

  public OntologyExtractIndividualsAdapter() {
    super(OntologyInfo.class);
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // Loop through the classes and find the individuals for each class
    info.getClasses().forEach(classInfo -> {
      // Get the individuals for the class based on the rdf:type property
      ResIterator individuals = info.getModel()
          .listResourcesWithProperty(RDF.type, info.getModel().createResource(classInfo.getUri()));
      individuals.forEachRemaining(individual -> {
        // Add the individual URI to the class info
        if (individual.getURI() != null) {
          classInfo.getIndividuals().add(individual);
          logger.debug("Added individual {} to class {}", individual.getURI(), classInfo.getUri());
        }
      });
    });
    return info;
  }
}
