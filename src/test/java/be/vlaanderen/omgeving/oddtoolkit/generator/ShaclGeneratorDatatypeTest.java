package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.junit.jupiter.api.Test;

/** Een datatype buiten de XSD-namespace (bv. geo:wktLiteral) moet sh:datatype opleveren, geen sh:class. */
class ShaclGeneratorDatatypeTest {

  private final Model model = ModelFactory.createDefaultModel();

  @Test
  void xsdDatatypeIsDatatype() {
    assertTrue(ShaclGenerator.isDatatype(XSD.xstring.inModel(model)));
  }

  @Test
  void alsRdfsDatatypeGetypeerdeResourceIsDatatype() {
    Resource wkt = model.createResource("http://www.opengis.net/ont/geosparql#wktLiteral", RDFS.Datatype);
    assertTrue(ShaclGenerator.isDatatype(wkt));
  }

  @Test
  void klasseIsGeenDatatype() {
    Resource geometrie = model.createResource("http://www.opengis.net/ont/geosparql#Geometry", OWL2.Class);
    assertFalse(ShaclGenerator.isDatatype(geometrie));
  }

  @Test
  void blankNodeIsGeenDatatype() {
    assertFalse(ShaclGenerator.isDatatype(model.createResource()));
    assertFalse(ShaclGenerator.isDatatype(null));
  }

  @Test
  void ongetypeerdeIriBuitenXsdIsGeenDatatype() {
    assertFalse(ShaclGenerator.isDatatype(model.createResource("http://example.org/Iets")));
    assertFalse(ShaclGenerator.isDatatype(model.createResource("http://example.org/Klasse", RDF.Property)));
  }
}
