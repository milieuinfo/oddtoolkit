package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.Assertions.assertThat;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link BikeshedGenerator}.
 * Runs the generator against the standard test-fixture ontology and verifies
 * the structure and content of the generated {@code .bs} source file.
 */
public class BikeshedGeneratorTest {

  private static final Path OUTPUT = Path.of("target/test-cache/bikeshed/ontology.bs");

  private final BikeshedGenerator generator =
      TestGeneratorFactory.generator("bikeshed", BikeshedGenerator.class);

  @Test
  void outputFileIsCreated() throws Exception {
    generator.run();
    assertThat(OUTPUT).exists().isNotEmptyFile();
  }

  @Test
  void metadataBlockIsPresent() throws Exception {
    generator.run();
    String content = Files.readString(OUTPUT);

    assertThat(content).contains("<pre class='metadata'>");
    assertThat(content).contains("</pre>");
    assertThat(content).contains("Title: RIE-IEPR Ontology");
    assertThat(content).contains("Status: LS");
    assertThat(content).contains("Editor: ODDToolkit");
  }

  @Test
  void introductionSectionIsPresent() throws Exception {
    generator.run();
    String content = Files.readString(OUTPUT);

    assertThat(content).contains("Introduction {#introduction}");
  }

  @Test
  void namespacesSectionIsPresent() throws Exception {
    generator.run();
    String content = Files.readString(OUTPUT);

    assertThat(content).contains("Namespaces {#namespaces}");
  }

  @Test
  void classesSectionContainsOntologyClasses() throws Exception {
    generator.run();
    String content = Files.readString(OUTPUT);

    assertThat(content).contains("Classes {#classes}");
    // The fixture ontology contains Exploitatie – verify it is documented
    assertThat(content).contains("<dfn>Exploitatie</dfn>");
  }

  @Test
  void classEntryContainsIriAndPropertyTable() throws Exception {
    generator.run();
    String content = Files.readString(OUTPUT);

    // Every class entry must include an IRI row and a property table header
    assertThat(content).contains("**IRI**");
    assertThat(content).contains("<table class=\"data\">");
    assertThat(content).contains("<th>Property</th>");
  }

  @Test
  void generatorNameIsCorrect() {
    assertThat(generator.getName()).isEqualTo("bikeshed");
  }

  @Test
  void generatorDescriptionIsInformative() {
    assertThat(generator.getDescription()).isNotBlank();
    assertThat(generator.getDescription().toLowerCase()).contains("bikeshed");
  }
}

