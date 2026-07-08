package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

class SchemaGeneratorJoinTableNamePatternTest {

  private final JavaGenerator generator = TestGeneratorFactory.generator("java", JavaGenerator.class);

  @Test
  void runtimeConfigurationLoadsJoinTablePatternFromYaml() {
    assertEquals("{source_table}_{target_table}",
        generator.getSchemaGeneratorProperties().getJoinTableNamePattern());
    assertEquals("{source_table}_id",
        generator.getSchemaGeneratorProperties().getJoinTableColumns().getSourceColumnNamePattern());
    assertEquals("{target_table}_id",
        generator.getSchemaGeneratorProperties().getJoinTableColumns().getTargetColumnNamePattern());
  }

  @Test
  void resolveJoinTableNameKeepsSourceAndTargetWhenSelfRelationIsNotAmbiguous() {
    generator.getSchemaGeneratorProperties().setJoinTableNamePattern(
        "rel_{source_table}_{target_table}");

    String joinTableName = generator.resolveJoinTableName("person", "person", "manages");

    assertEquals("rel_person_person", joinTableName);
  }

  @Test
  void resolveJoinTableNameAppendsRelationNameForAmbiguousSelfRelations() {
    generator.getSchemaGeneratorProperties().setJoinTableNamePattern(
        "rel_{source_table}_{target_table}");

    String joinTableName = generator.resolveJoinTableName("person", "person", "manages", true);

    assertEquals("rel_person_person_manages", joinTableName);
  }

  @Test
  void resolveJoinTableNameSupportsExplicitRelationNamePlaceholder() {
    generator.getSchemaGeneratorProperties().setJoinTableNamePattern(
        "rel_{source_table}_{relation_name}_{target_table}");

    String joinTableName = generator.resolveJoinTableName("person", "address", "livesAt");

    assertEquals("rel_person_lives_at_address", joinTableName);
  }

  @Test
  void resolveJoinTableNameNormalizesTrailingTargetSuffixInRelationName() {
    generator.getSchemaGeneratorProperties().setJoinTableNamePattern(
        "{source_table}_{target_table}");

    String joinTableName = generator.resolveJoinTableName("proces", "proces",
        "volgt_op_proces", true);

    assertEquals("proces_proces_volgt_op", joinTableName);
  }

  @Test
  void disambiguateSelfReferencingColumnNameQualifiesCollidingTargetColumn() {
    String sourceColumnName = generator.resolveJoinColumnName("{source_table}_id",
        "proces", "proces", "uuid");
    String targetColumnName = generator.resolveJoinColumnName("{target_table}_id",
        "proces", "proces", "uuid");

    String disambiguated = generator.disambiguateSelfReferencingColumnName(
        targetColumnName, sourceColumnName, "volgt_op_proces", "proces");

    assertEquals("proces_id", sourceColumnName);
    assertEquals("proces_id", targetColumnName);
    assertEquals("volgt_op_proces_id", disambiguated);
  }

  @Test
  void disambiguateSelfReferencingColumnNameLeavesNonCollidingNamesUntouched() {
    String disambiguated = generator.disambiguateSelfReferencingColumnName(
        "target_uuid", "source_uuid", "volgt_op_proces", "proces");

    assertEquals("target_uuid", disambiguated);
  }
}
