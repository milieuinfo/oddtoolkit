package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for union types support in ClassGenerator.
 * Verifies that properties with multiple range types (union types) are correctly identified
 * and stored in the rangeClasses list.
 *
 * <p>Uses a small, purpose-built ontology (src/test/resources/examples/union-types.ttl) rather
 * than the RIE-IEPR example ontology, so this mechanism test doesn't break whenever the example
 * domain model evolves.
 */
public class ClassGeneratorUnionTypesTest {
  private final ClassGenerator generator = TestGeneratorFactory.generator("typescript",
      TypescriptGenerator.class, "src/test/resources/examples/union-types.ttl");

  @Test
  void meetpuntHasUnionTypeForHeeftOnderdeelProperty() {
    generator.run();

    // Find the Meetpunt class
    ClassGenerator.Clazz meetpunt = generator.getClasses().stream()
        .filter(c -> "Meetpunt".equals(c.getName()))
        .findFirst()
        .orElse(null);

    assertNotNull(meetpunt, "Meetpunt class should exist");

    // Find the heeftOnderdeel attribute
    ClassGenerator.Attribute heeftOnderdeel = meetpunt.getAttributes().stream()
        .filter(a -> "heeftOnderdeel".equals(a.getName()))
        .findFirst()
        .orElse(null);

    assertNotNull(heeftOnderdeel, "heeftOnderdeel attribute should exist");
    assertTrue(heeftOnderdeel.isUnionType(), "heeftOnderdeel should be a union type");

    // Verify that rangeClasses contains both Filter and Sensor
    List<ClassGenerator.Clazz> rangeClasses = heeftOnderdeel.getRangeClasses();
    assertNotNull(rangeClasses, "rangeClasses should not be null");
    assertTrue(rangeClasses.size() >= 2, "rangeClasses should contain at least 2 types");

    List<String> rangeClassNames = rangeClasses.stream()
        .map(ClassGenerator.Clazz::getName)
        .toList();

    assertTrue(rangeClassNames.contains("Filter"), "rangeClasses should contain Filter");
    assertTrue(rangeClassNames.contains("Sensor"), "rangeClasses should contain Sensor");
  }
}
