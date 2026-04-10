package be.vlaanderen.omgeving.oddtoolkit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

class PropertyInfoCardinalityTest {

  @Test
  void initializePropertySupportsQualifiedMinCardinality() {
    Model model = ModelFactory.createDefaultModel();

    Resource property = model.createResource("https://example.org/p");
    Resource restriction = model.createResource();
    restriction.addProperty(RDF.type, OWL2.Restriction);
    restriction.addProperty(OWL2.onProperty, property);
    restriction.addProperty(OWL2.onClass, model.createResource("https://example.org/Observatie"));
    restriction.addLiteral(OWL2.minQualifiedCardinality, 1);
    restriction.addLiteral(OWL2.maxQualifiedCardinality, 3);

    PropertyInfo info = new PropertyInfo(Scope.ONTOLOGY, property, restriction);

    assertEquals(1, info.getCardinalityTo().getMin());
    assertEquals(null, info.getCardinalityTo().getMax());
    assertTrue(info.getRange().contains("https://example.org/Observatie"));
  }

  @Test
  void qualifiedExactCardinalitySetsOnlyLowerBoundAtPropertyLevel() {
    Model model = ModelFactory.createDefaultModel();

    Resource property = model.createResource("https://example.org/p");
    Resource restriction = model.createResource();
    restriction.addProperty(RDF.type, OWL2.Restriction);
    restriction.addProperty(OWL2.onProperty, property);
    restriction.addProperty(OWL2.onClass, model.createResource("https://example.org/Exploitatie"));
    restriction.addLiteral(OWL2.minQualifiedCardinality, 1);
    restriction.addLiteral(OWL2.maxQualifiedCardinality, 5);
    restriction.addLiteral(OWL2.qualifiedCardinality, 2);

    PropertyInfo info = new PropertyInfo(Scope.ONTOLOGY, property, restriction);

    assertEquals(2, info.getCardinalityTo().getMin());
    assertEquals(null, info.getCardinalityTo().getMax());
  }

  @Test
  void rangeValuesAreDeduplicatedForQualifiedAndInferredEquivalentRestrictions() {
    Model model = ModelFactory.createDefaultModel();

    Resource property = model.createResource("https://example.org/p");
    Resource restriction = model.createResource();
    Resource exploitatie = model.createResource("https://example.org/Exploitatie");

    restriction.addProperty(RDF.type, OWL2.Restriction);
    restriction.addProperty(OWL2.onProperty, property);
    restriction.addProperty(OWL2.onClass, exploitatie);
    restriction.addProperty(OWL2.someValuesFrom, exploitatie);

    PropertyInfo info = new PropertyInfo(Scope.ONTOLOGY, property, restriction);

    assertTrue(info.getRange().contains("https://example.org/Exploitatie"));
  }
}
