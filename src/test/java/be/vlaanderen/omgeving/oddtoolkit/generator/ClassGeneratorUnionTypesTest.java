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
 */
public class ClassGeneratorUnionTypesTest {
  private final ClassGenerator generator = TestGeneratorFactory.generator("typescript",
      TypescriptGenerator.class);

  @Test
  void aangifteHasUnionTypeForSubjectProperty() {
    generator.run();

    // Find the Aangifte class
    ClassGenerator.Clazz aangifte = generator.getClasses().stream()
        .filter(c -> "Aangifte".equals(c.getName()))
        .findFirst()
        .orElse(null);

    assertNotNull(aangifte, "Aangifte class should exist");

    // Find the onderwerp (subject) attribute
    ClassGenerator.Attribute onderwerp = aangifte.getAttributes().stream()
        .filter(a -> "onderwerp".equals(a.getName()))
        .findFirst()
        .orElse(null);

    assertNotNull(onderwerp, "onderwerp attribute should exist");
    assertTrue(onderwerp.isUnionType(), "onderwerp should be a union type");

    // Verify that rangeClasses contains both Exploitatie and Observatie
    List<ClassGenerator.Clazz> rangeClasses = onderwerp.getRangeClasses();
    assertNotNull(rangeClasses, "rangeClasses should not be null");
    assertTrue(rangeClasses.size() >= 2, "rangeClasses should contain at least 2 types");

    List<String> rangeClassNames = rangeClasses.stream()
        .map(ClassGenerator.Clazz::getName)
        .toList();

    assertTrue(rangeClassNames.contains("Exploitatie"), "rangeClasses should contain Exploitatie");
    assertTrue(rangeClassNames.contains("Observatie"), "rangeClasses should contain Observatie");
  }
}
