package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

class SchemaGeneratorIdentifierResolutionTest {

  private static final String LOCAL_ID_URI =
      "https://data.riepr.omgeving.vlaanderen.be/ns/riepr#localId";

  private final JavaGenerator generator = TestGeneratorFactory.generator("java",
      JavaGenerator.class);

  @Test
  void runUsesOverridePropertyIdentifiersWhenBuildingSchemaTables() {
    assertThatCode(generator::run).doesNotThrowAnyException();

    assertThat(generator.getTables().stream()
        .flatMap(table -> table.getColumns().stream())
        .anyMatch(column -> LOCAL_ID_URI.equals(column.getUri()) && column.isPrimaryKey()))
        .isTrue();
  }
}

