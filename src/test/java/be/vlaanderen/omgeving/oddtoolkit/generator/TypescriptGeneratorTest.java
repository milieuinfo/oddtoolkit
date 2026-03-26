package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

/**
 * Test for TypescriptGenerator.
 * Generates TypeScript classes, interfaces and enums from the ontology.
 */
public class TypescriptGeneratorTest {
  private final TypescriptGenerator generator = TestGeneratorFactory.generator("typescript",
      TypescriptGenerator.class);

  @Test
  void testGenerator() {
    generator.run();
  }
}
