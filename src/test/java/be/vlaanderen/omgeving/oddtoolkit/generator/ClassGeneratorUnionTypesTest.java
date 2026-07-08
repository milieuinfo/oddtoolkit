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
  void meetpuntHasUnionTypeForHasSubSystemProperty() {
    generator.run();

    // Find the Meetpunt class
    ClassGenerator.Clazz meetpunt = generator.getClasses().stream()
        .filter(c -> "Meetpunt".equals(c.getName()))
        .findFirst()
        .orElse(null);

    assertNotNull(meetpunt, "Meetpunt class should exist");

    // Find the heeftSubSysteem (hasSubSystem) attribute
    ClassGenerator.Attribute heeftSubSysteem = meetpunt.getAttributes().stream()
        .filter(a -> "heeftSubSysteem".equals(a.getName()))
        .findFirst()
        .orElse(null);

    assertNotNull(heeftSubSysteem, "heeftSubSysteem attribute should exist");
    assertTrue(heeftSubSysteem.isUnionType(), "heeftSubSysteem should be a union type");

    // Verify that rangeClasses contains both MeetInstrument and Filter
    List<ClassGenerator.Clazz> rangeClasses = heeftSubSysteem.getRangeClasses();
    assertNotNull(rangeClasses, "rangeClasses should not be null");
    assertTrue(rangeClasses.size() >= 2, "rangeClasses should contain at least 2 types");

    List<String> rangeClassNames = rangeClasses.stream()
        .map(ClassGenerator.Clazz::getName)
        .toList();

    assertTrue(rangeClassNames.contains("MeetInstrument"), "rangeClasses should contain MeetInstrument");
    assertTrue(rangeClassNames.contains("Filter"), "rangeClasses should contain Filter");
  }
}
