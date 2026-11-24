package be.vlaanderen.omgeving.oddtoolkit.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.Test;

class RdfPropertyValueTest {
  @Test
  void roundTripsTypedAndLanguageLiteralsAndIris() {
    Model model = ModelFactory.createDefaultModel();
    Resource subject = model.createResource("https://example.org/result/1");
    var predicate = model.createProperty("https://example.org/measurement");
    Statement[] statements = {
        model.createStatement(subject, predicate,
            model.createTypedLiteral("1.20", "http://www.w3.org/2001/XMLSchema#decimal")),
        model.createStatement(subject, predicate, model.createLiteral("waarde", "nl")),
        model.createStatement(subject, predicate, model.createResource("https://example.org/unit"))
    };
    for (Statement statement : statements) {
      RdfPropertyValue value = RdfPropertyValue.fromStatement(statement);
      assertThat(value.toStatement(subject)).isEqualTo(statement);
    }
  }

  @Test
  void rejectsUnsupportedOrInvalidValues() {
    Model model = ModelFactory.createDefaultModel();
    Statement blank = model.createStatement(model.createResource("https://example.org/s"),
        model.createProperty("https://example.org/p"), model.createResource());
    assertThatThrownBy(() -> RdfPropertyValue.fromStatement(blank))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RdfPropertyValue("https://example.org/p",
        RdfPropertyValue.ObjectKind.IRI, "https://example.org/o", null, "nl"))
        .isInstanceOf(IllegalArgumentException.class);
    RdfPropertyValue invalid = new RdfPropertyValue("https://example.org/p",
        RdfPropertyValue.ObjectKind.LITERAL, "not-a-number",
        "http://www.w3.org/2001/XMLSchema#decimal", null);
    assertThatThrownBy(() -> invalid.toStatement(model.createResource("https://example.org/s")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
