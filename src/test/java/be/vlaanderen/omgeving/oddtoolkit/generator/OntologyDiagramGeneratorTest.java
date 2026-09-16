package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.Assertions.assertThat;

import be.vlaanderen.omgeving.oddtoolkit.config.OddtoolkitBootstrap;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Mechanism test for the ontology-diagram generator, based on a small purpose-built ontology
 * (src/test/resources/examples/ontology-diagram) rather than the RIE-IEPR example ontology, so
 * domain-model changes there cannot silently break the test.
 */
public class OntologyDiagramGeneratorTest {

  private static final Path OUTPUT =
      Path.of("target/test-cache/ontology-diagram/ontology-diagram.mmd");
  private static final Path FILTERED_OUTPUT =
      Path.of("target/test-cache/ontology-diagram/ontology-diagram-filtered.mmd");

  @Test
  void rendersClassesSubclassesAndObjectPropertyRelations() throws Exception {
    OddtoolkitBootstrap.bootstrap(
            new String[]{"--config-file=src/test/resources/examples/ontology-diagram/config.yml"})
        .get("ontology-diagram")
        .orElseThrow()
        .run();

    assertThat(Files.exists(OUTPUT)).isTrue();
    String content = Files.readString(OUTPUT);

    // Plain flowchart nodes, named with the prefix declared in the ontology (vann for the local ns)
    assertThat(content).contains("flowchart LR");
    assertThat(content).contains("mini_Installatie[\"mini.Installatie\"]\n");
    assertThat(content).contains("mini_Emissiepunt[\"mini.Emissiepunt\"]");
    assertThat(content).contains("mini_Proces[\"mini.Proces\"]");
    assertThat(content).contains("ssn_System[\"ssn.System\"]");
    assertThat(content).contains("pplan_Plan[\"pplan.Plan\"]");
    assertThat(content).contains("sosa_Observation[\"sosa.Observation\"]");
    assertThat(content).contains("sosa_Procedure[\"sosa.Procedure\"]");
    // A class-like range that is not itself a declared class becomes its own node
    assertThat(content).contains("qudt_Unit[\"qudt.Unit\"]");

    // Dotted arrows for rdfs:subClassOf between named classes, colored red via linkStyle
    assertThat(content).contains("mini_Installatie -.-> ssn_System");
    assertThat(content).contains("mini_Emissiepunt -.-> ssn_System");
    assertThat(content).contains("mini_Proces -.-> pplan_Plan");
    assertThat(content).contains("mini_Proces -.-> sosa_Procedure");
    assertThat(content).contains("mini_Observatie -.-> sosa_Observation");
    // The five subclass edges, in emission order, are the ones styled red
    assertThat(content).contains("linkStyle 0,1,3,5,6 stroke:#c0392b,stroke-width:2px");

    // Labeled arrows for object property relations, without cardinality or attributes
    assertThat(content).contains("mini_Installatie -->|heeftOnderdeel| mini_Emissiepunt");
    assertThat(content).contains("mini_Proces -->|implementeert| mini_Installatie");
    assertThat(content).contains("mini_Observatie -->|heeftEenheid| qudt_Unit");
    assertThat(content).doesNotContain("(1..*)");
    assertThat(content).doesNotContain("(0..1)");

    // Data properties are not rendered
    assertThat(content).doesNotContain("geldigVan");
    assertThat(content).doesNotContain("benaming");

    // Application-model artifacts (ontology.extra-properties) stay out of the ontology diagram
    assertThat(content).doesNotContain("uuid");

    // Per-namespace styling
    assertThat(content).contains("classDef ns_ssn fill:#4bde2a,stroke:#000000");
    assertThat(content).contains("ssn_System[\"ssn.System\"]:::ns_ssn");
  }

  @Test
  void namespaceFilterKeepsOwnNamespaceAndAllowedNamespaces() throws Exception {
    OddtoolkitBootstrap.bootstrap(
            new String[]{"--config-file=src/test/resources/examples/ontology-diagram/config-filtered.yml"})
        .get("ontology-diagram")
        .orElseThrow()
        .run();

    assertThat(Files.exists(FILTERED_OUTPUT)).isTrue();
    String content = Files.readString(FILTERED_OUTPUT);

    assertThat(content).contains("mini_Installatie[\"mini.Installatie\"]");
    assertThat(content).contains("ssn_System[\"ssn.System\"]");
    assertThat(content).contains("qudt_Unit[\"qudt.Unit\"]");
    assertThat(content).contains("mini_Installatie -.-> ssn_System");
    assertThat(content).doesNotContain("pplan_Plan");
    assertThat(content).doesNotContain("sosa_Observation");
    assertThat(content).doesNotContain("mini_Proces -.-> sosa_Procedure");
  }

  @Test
  void derivesPrefixesFromNamespaceUrisWhenNotDeclared() {
    assertThat(OntologyDiagramGenerator.derivePrefix("http://qudt.org/schema/qudt/")).isEqualTo("qudt");
    assertThat(OntologyDiagramGenerator.derivePrefix("http://purl.org/net/p-plan#")).isEqualTo("p_plan");
    assertThat(OntologyDiagramGenerator.derivePrefix("https://data.example.be/ns/csor#")).isEqualTo("csor");
    assertThat(OntologyDiagramGenerator.derivePrefix("http://data.europa.eu/ux2/nace2.1/"))
        .isEqualTo("nace2_1");
    assertThat(OntologyDiagramGenerator.derivePrefix("http://www.w3.org/2001/XMLSchema#"))
        .isEqualTo("XMLSchema");
    assertThat(OntologyDiagramGenerator.derivePrefix("https://example.org/2024/")).isEqualTo("_2024");
  }
}
