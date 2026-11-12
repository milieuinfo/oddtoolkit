package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A join table of a *..* relation must key on both foreign key columns, also when identity tables and surrogate
 * keys are enabled: otherwise one side can only be linked once.
 */
class SchemaGeneratorJoinTablePrimaryKeyTest {

  private static final Pattern JOIN_TABLE = Pattern.compile(
      "-- Table type: JOIN\\n(?:-- [^\\n]*\\n)*CREATE TABLE (\\w+) \\((.*?)\\n\\);", Pattern.DOTALL);
  private static final Pattern FOREIGN_KEY_COLUMN = Pattern.compile(
      "-- Foreign key referencing [^\\n]*\\n\\s+(\\w+) ");
  private static final Pattern PRIMARY_KEY = Pattern.compile("PRIMARY KEY \\(([^)]*)\\)");

  private final SQLGenerator generator = TestGeneratorFactory.generator("sql", SQLGenerator.class);

  @AfterEach
  void resetConfig() {
    generator.getOntologyConfiguration().getSurrogateKeys().setEnabled(false);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void everyJoinTableKeysOnAllItsForeignKeyColumns(boolean surrogateKeys) throws Exception {
    Map<String, JoinTable> joinTables = generate(surrogateKeys);

    assertFalse(joinTables.isEmpty(), "expected join tables in the generated schema");
    joinTables.forEach((name, table) -> assertTrue(table.primaryKey().containsAll(table.foreignKeys()),
        name + ": primary key " + table.primaryKey() + " misses one of " + table.foreignKeys()));
  }

  @Test
  void exploitatieSysteemKeysOnBothSidesWithSurrogateKeys() throws Exception {
    JoinTable table = generate(true).get("exploitatie_systeem");

    assertEquals(List.of("exploitatie_id", "systeem_id"), table.foreignKeys());
    assertTrue(table.primaryKey().containsAll(List.of("exploitatie_id", "systeem_id")), table.primaryKey().toString());
  }

  private Map<String, JoinTable> generate(boolean surrogateKeys) throws Exception {
    generator.getOntologyConfiguration().getSurrogateKeys().setEnabled(surrogateKeys);
    generator.run();
    String sql = Files.readString(Path.of("target/test-cache/sql/schema.sql"));
    Map<String, JoinTable> joinTables = new LinkedHashMap<>();
    Matcher table = JOIN_TABLE.matcher(sql);
    while (table.find()) {
      List<String> foreignKeys = new ArrayList<>();
      Matcher foreignKey = FOREIGN_KEY_COLUMN.matcher(table.group(2));
      while (foreignKey.find()) {
        foreignKeys.add(foreignKey.group(1));
      }
      Matcher primaryKey = PRIMARY_KEY.matcher(table.group(2));
      List<String> primaryKeyColumns = primaryKey.find()
          ? List.of(primaryKey.group(1).split(",\\s*")) : List.of();
      joinTables.put(table.group(1), new JoinTable(foreignKeys, primaryKeyColumns));
    }
    return joinTables;
  }

  private record JoinTable(List<String> foreignKeys, List<String> primaryKey) {
  }
}
