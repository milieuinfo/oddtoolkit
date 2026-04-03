package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class SQLGeneratorTest {

  private final SQLGenerator generator = TestGeneratorFactory.generator("sql", SQLGenerator.class);

  @Test
  void testRunGeneratesDiagram() throws Exception {
    assertEquals("{source_table}_{target_table}",
        generator.getSchemaGeneratorProperties().getJoinTableNamePattern());

    generator.run();

    String generatedSql = Files.readString(Path.of("target/test-cache/sql/schema.sql"));
    assertTrue(generatedSql.contains("CREATE TABLE proces_proces_volgt_op ("));
    assertTrue(generatedSql.contains("CREATE TABLE exploitant_contactpersoon ("));
  }
}
