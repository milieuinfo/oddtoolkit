package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

class SchemaGeneratorJoinColumnNamePatternTest {

  private final JavaGenerator generator = TestGeneratorFactory.generator("java", JavaGenerator.class);

  @Test
  void resolveJoinColumnNameUsesSourcePrefixByDefault() {
    String result = generator.resolveJoinColumnName("source_{column}", "exploitatie", "activiteit", "uuid");

    assertEquals("source_uuid", result);
  }

  @Test
  void resolveJoinColumnNameUsesTargetPrefixByDefault() {
    String result = generator.resolveJoinColumnName("target_{column}", "exploitatie", "activiteit", "uuid");

    assertEquals("target_uuid", result);
  }

  @Test
  void resolveJoinColumnNameExpandsSourceTablePlaceholder() {
    String result = generator.resolveJoinColumnName("{source_table}_{column}", "exploitatie", "activiteit", "uuid");

    assertEquals("exploitatie_uuid", result);
  }

  @Test
  void resolveJoinColumnNameExpandsTargetTablePlaceholder() {
    String result = generator.resolveJoinColumnName("{target_table}_{column}", "exploitatie", "activiteit", "uuid");

    assertEquals("activiteit_uuid", result);
  }

  @Test
  void resolveJoinColumnNameSupportsOverridingColumnNameEntirely() {
    // {source_table}_id ignores the original column name and always produces <table>_id
    String result = generator.resolveJoinColumnName("{source_table}_id", "exploitatie", "activiteit", "uuid");

    assertEquals("exploitatie_id", result);
  }

  @Test
  void resolveJoinColumnNameConvertsToSnakeCase() {
    String result = generator.resolveJoinColumnName("{source_table}_{column}", "ExploitatieLocatie", "Activiteit", "localId");

    assertEquals("exploitatie_locatie_local_id", result);
  }

  @Test
  void resolveJoinColumnNameFallsBackToSourceColumnWhenPatternIsNull() {
    String result = generator.resolveJoinColumnName(null, "exploitatie", "activiteit", "uuid");

    assertEquals("source_uuid", result);
  }

  @Test
  void resolveJoinColumnNameFallsBackToSourceColumnWhenPatternIsBlank() {
    String result = generator.resolveJoinColumnName("  ", "exploitatie", "activiteit", "uuid");

    assertEquals("source_uuid", result);
  }
}

