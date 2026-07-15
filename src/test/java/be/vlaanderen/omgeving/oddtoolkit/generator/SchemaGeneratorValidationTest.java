package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import be.vlaanderen.omgeving.oddtoolkit.generator.SchemaGenerator.Column;
import be.vlaanderen.omgeving.oddtoolkit.generator.SchemaGenerator.Table;
import org.junit.jupiter.api.Test;

class SchemaGeneratorValidationTest {

  private final JavaGenerator generator = TestGeneratorFactory.generator("java", JavaGenerator.class);

  @Test
  void validateSchemaRejectsTablesWithDuplicateColumnNames() {
    Table table = new Table();
    table.setName("test_duplicate_columns");
    Column first = new Column();
    first.setName("proces_id");
    Column second = new Column();
    second.setName("proces_id");
    table.addColumn(first);
    table.addColumn(second);

    generator.getTables().add(table);
    try {
      IllegalStateException exception = assertThrows(IllegalStateException.class,
          generator::validateSchema);

      assertTrue(exception.getMessage().contains("test_duplicate_columns"));
      assertTrue(exception.getMessage().contains("proces_id"));
    } finally {
      generator.getTables().remove(table);
    }
  }

  @Test
  void validateSchemaAcceptsTablesWithUniqueColumnNames() {
    Table table = new Table();
    table.setName("test_unique_columns");
    Column first = new Column();
    first.setName("proces_id");
    Column second = new Column();
    second.setName("volgt_op_proces_id");
    table.addColumn(first);
    table.addColumn(second);

    generator.getTables().add(table);
    try {
      generator.validateSchema();
    } finally {
      generator.getTables().remove(table);
    }
  }
}
