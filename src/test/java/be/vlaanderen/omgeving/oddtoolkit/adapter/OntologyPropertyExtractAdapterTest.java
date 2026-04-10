package be.vlaanderen.omgeving.oddtoolkit.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

class OntologyPropertyExtractAdapterTest {

  @Test
  void adaptCombinesMultipleRestrictionsForSameProperty() {
    Model model = ModelFactory.createDefaultModel();

    String classUri = "https://example.org/Thing";
    String propertyUri = "https://example.org/subject";

    Resource clazz = model.createResource(classUri);
    Resource property = model.createResource(propertyUri);

    Resource restrictionMin = model.createResource();
    restrictionMin.addProperty(RDF.type, OWL2.Restriction);
    restrictionMin.addProperty(OWL2.onProperty, property);
    restrictionMin.addLiteral(OWL2.minCardinality, 1);
    restrictionMin.addProperty(OWL2.someValuesFrom, model.createResource("https://example.org/Exploitatie"));

    Resource restrictionMax = model.createResource();
    restrictionMax.addProperty(RDF.type, OWL2.Restriction);
    restrictionMax.addProperty(OWL2.onProperty, property);
    restrictionMax.addLiteral(OWL2.maxCardinality, 2);
    restrictionMax.addProperty(OWL2.someValuesFrom, model.createResource("https://example.org/Observatie"));

    clazz.addProperty(RDFS.subClassOf, restrictionMin);
    clazz.addProperty(RDFS.subClassOf, restrictionMax);

    ClassInfo classInfo = new ClassInfo(Scope.ONTOLOGY, clazz);

    OntologyInfo ontologyInfo = new OntologyInfo(new OntologyConfiguration());
    ontologyInfo.setModel(model);
    ontologyInfo.setInferredModel(ModelFactory.createRDFSModel(model));
    ontologyInfo.setClasses(List.of(classInfo));

    new OntologyPropertyExtractAdapter().adapt(ontologyInfo);

    assertEquals(1, classInfo.getProperties().size());
    PropertyInfo merged = classInfo.getPropertyByUri(propertyUri);
    assertNotNull(merged);
    assertEquals(1, merged.getCardinalityTo().getMin());
    assertEquals(2, merged.getCardinalityTo().getMax());
    assertTrue(merged.getRange().contains("https://example.org/Exploitatie"));
    assertTrue(merged.getRange().contains("https://example.org/Observatie"));
  }

  @Test
  void adaptCombinesSomeValuesFromAndQualifiedOnClassForSameProperty() {
    Model model = ModelFactory.createDefaultModel();

    String classUri = "https://example.org/Aangifte";
    String propertyUri = "http://purl.org/dc/terms/subject";

    Resource clazz = model.createResource(classUri);
    Resource property = model.createResource(propertyUri);

    Resource observationRestriction = model.createResource();
    observationRestriction.addProperty(RDF.type, OWL2.Restriction);
    observationRestriction.addProperty(OWL2.onProperty, property);
    observationRestriction.addProperty(OWL2.someValuesFrom,
        model.createResource("https://example.org/Observatie"));

    Resource exploitatieRestriction = model.createResource();
    exploitatieRestriction.addProperty(RDF.type, OWL2.Restriction);
    exploitatieRestriction.addProperty(OWL2.onProperty, property);
    exploitatieRestriction.addProperty(OWL2.onClass,
        model.createResource("https://example.org/Exploitatie"));
    exploitatieRestriction.addLiteral(OWL2.qualifiedCardinality, 1);

    clazz.addProperty(RDFS.subClassOf, observationRestriction);
    clazz.addProperty(RDFS.subClassOf, exploitatieRestriction);

    ClassInfo classInfo = new ClassInfo(Scope.ONTOLOGY, clazz);

    OntologyInfo ontologyInfo = new OntologyInfo(new OntologyConfiguration());
    ontologyInfo.setModel(model);
    ontologyInfo.setInferredModel(ModelFactory.createRDFSModel(model));
    ontologyInfo.setClasses(List.of(classInfo));

    new OntologyPropertyExtractAdapter().adapt(ontologyInfo);

    PropertyInfo merged = classInfo.getPropertyByUri(propertyUri);
    assertNotNull(merged);
    assertEquals(1, merged.getCardinalityTo().getMin());
    assertEquals(null, merged.getCardinalityTo().getMax());
    assertTrue(merged.getRange().contains("https://example.org/Observatie"));
    assertTrue(merged.getRange().contains("https://example.org/Exploitatie"));
  }
}
