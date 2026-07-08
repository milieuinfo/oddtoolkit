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

    assertThat(hasInputVarBlock).contains("@ManyToMany").contains("proces_proces_variabele");
    assertThat(hasOutputVarBlock).contains("@ManyToMany").contains("proces_proces_variabele");
  }

  private static String blockBetween(String source, String startMarker, String endMarker) {
    int start = source.indexOf(startMarker);
    int end = source.indexOf(endMarker, start);
    assertThat(start).as("marker '%s' should be present", startMarker).isNotEqualTo(-1);
    assertThat(end).as("marker '%s' should be present after '%s'", endMarker, startMarker).isNotEqualTo(-1);
    return source.substring(start, end);
  }
}
