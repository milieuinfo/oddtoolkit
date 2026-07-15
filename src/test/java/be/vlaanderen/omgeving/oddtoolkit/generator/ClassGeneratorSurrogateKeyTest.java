package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClassGeneratorSurrogateKeyTest {

  private static final String CLASS_WITH_COMPOSITE_KEY = "Emissiepunt";

  private final ClassGenerator generator = TestGeneratorFactory.generator("class", ClassGenerator.class);

  @AfterEach
  void resetConfig() {
    generator.getOntologyConfiguration().getSurrogateKeys().setEnabled(false);
  }

  @Test
  void classRetainsCompositeKeyWhenSurrogateKeysAreDisabled() {
    generator.getOntologyConfiguration().getSurrogateKeys().setEnabled(false);
    generator.run();

    List<ClassGenerator.Attribute> primaryKeys = primaryKeysOf(generator, CLASS_WITH_COMPOSITE_KEY);

    assertTrue(primaryKeys.size() > 1,
        "Expected " + CLASS_WITH_COMPOSITE_KEY + " to have a composite key by default");
  }

  @Test
  void classGetsSingleSurrogateKeyWhenEnabled() {
    generator.getOntologyConfiguration().getSurrogateKeys().setEnabled(true);
    generator.run();

    ClassGenerator.Clazz clazz = generator.getClasses().stream()
        .filter(c -> CLASS_WITH_COMPOSITE_KEY.equals(c.getName()))
        .findFirst()
        .orElseThrow();

    List<ClassGenerator.Attribute> primaryKeys = clazz.getAttributes().stream()
        .filter(ClassGenerator.Attribute::isPrimaryKey)
        .toList();

    assertEquals(1, primaryKeys.size(),
        "Expected a single surrogate primary key to replace the composite key");
    assertEquals("id", primaryKeys.getFirst().getName());
    assertFalse(primaryKeys.getFirst().isNullable());

    // The original identifier attributes are still present, just no longer primary keys.
    boolean originalIdentifiersDemoted = clazz.getAttributes().stream()
        .filter(attribute -> !attribute.isPrimaryKey())
        .anyMatch(attribute -> "uuid".equals(attribute.getName()));
    assertTrue(originalIdentifiersDemoted,
        "Expected the original natural key attribute to remain as a regular attribute");
  }

  private List<ClassGenerator.Attribute> primaryKeysOf(ClassGenerator generator, String className) {
    return generator.getClasses().stream()
        .filter(c -> className.equals(c.getName()))
        .findFirst()
        .orElseThrow()
        .getAttributes().stream()
        .filter(ClassGenerator.Attribute::isPrimaryKey)
        .toList();
  }
}
