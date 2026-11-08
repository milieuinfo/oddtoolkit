package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.Assertions.assertThat;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class JavaGeneratorTest {
  private final JavaGenerator generator = TestGeneratorFactory.generator("java", JavaGenerator.class);

  @Test
  void testGenerator() throws Exception {
    generator.run();
  }

  @Test
  void rendersManyToManyAnnotationsForEveryPropertySharingAJoinTable() throws Exception {
    generator.run();

    String proces = Files.readString(Path.of("target/test-cache/java/Proces.java"));

    // hasInputVar and hasOutputVar both target ProcesVariabele and share one join table;
    // both fields must independently get @ManyToMany/@JoinTable, not just the first one.
    String hasInputVarBlock = blockBetween(proces, "hasInputVar\">hasInputVar</a>", "heeftInvoer;");
    String hasOutputVarBlock = blockBetween(proces, "hasOutputVar\">hasOutputVar</a>", "heeftUitvoer;");

    assertThat(hasInputVarBlock).contains("@ManyToMany").contains("proces_procesvariabele");
    assertThat(hasOutputVarBlock).contains("@ManyToMany").contains("proces_procesvariabele");
  }

  @Test
  void jsonLdOptionsAreOffByDefault() throws Exception {
    generator.run();

    Path output = Path.of("target/test-cache/java");
    String exploitatie = Files.readString(output.resolve("Exploitatie.java"));
    assertThat(exploitatie).contains("private LocalDateTime aangemaaktOp;")
        .doesNotContain("OffsetDateTime")
        .doesNotContain("getRdfType")
        .doesNotContain("@JsonSerialize")
        .doesNotContain("IriReferentie");
    assertThat(Files.readString(output.resolve("ExterneIdentificator.java")))
        .doesNotContain("@JsonIgnore")
        .doesNotContain("JsonLd");
    assertThat(Files.readString(output.resolve("Status.java")))
        .doesNotContain("@JsonValue")
        .doesNotContain("fromUri");
    assertThat(output.resolve("IriReferentie.java")).doesNotExist();
    assertThat(output.resolve("IriReferentieSerializer.java")).doesNotExist();
  }

  private static String blockBetween(String source, String startMarker, String endMarker) {
    int start = source.indexOf(startMarker);
    int end = source.indexOf(endMarker, start);
    assertThat(start).as("marker '%s' should be present", startMarker).isNotEqualTo(-1);
    assertThat(end).as("marker '%s' should be present after '%s'", endMarker, startMarker).isNotEqualTo(-1);
    return source.substring(start, end);
  }
}
