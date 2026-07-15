package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

@AdapterDependency({
    ConceptSchemeLoadAdapter.class
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "concept-scheme-extract.enabled", havingValue = "true", matchIfMissing = true)
public class ConceptSchemeExtractAdapter extends AbstractAdapter<ConceptSchemeInfo> {

  public ConceptSchemeExtractAdapter() {
    super(ConceptSchemeInfo.class);
  }

  @Override
  public ConceptSchemeInfo adapt(ConceptSchemeInfo info) {
    // If no model is loaded, return early
    if (info.getModel() == null) {
      return info;
    }

    // Extract the concepts
    ResIterator conceptIterator = info.getModel().listResourcesWithProperty(
        RDF.type,
        SKOS.Concept
    );
    List<ClassConceptInfo> classConcepts = new ArrayList<>();
    List<PropertyConceptInfo> propertyConcepts = new ArrayList<>();
    conceptIterator.forEachRemaining(concept -> {
      if (concept.hasProperty(OWL2.equivalentClass)) {
        classConcepts.add(new ClassConceptInfo(Scope.CONCEPTS, concept));
      }
      if (concept.hasProperty(OWL2.equivalentProperty)) {
        propertyConcepts.add(new PropertyConceptInfo(Scope.CONCEPTS, concept));
      } else {
        // Check if owl:equivalentClass references a property URI (e.g. sosa:madeBySensor)
        concept.listProperties(OWL2.equivalentClass).forEachRemaining(stmt -> {
          if (stmt.getObject().isResource()) {
            String propURI = stmt.getObject().asResource().getURI();
            if (propURI != null) {
              propertyConcepts.add(new PropertyConceptInfo(Scope.CONCEPTS, concept));
            }
          }
        });
      }
    });

    info.setClassConcepts(classConcepts);
    info.setPropertyConcepts(propertyConcepts);
    return null;
  }
}
