package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

public class JavaGeneratorTest {
  private final JavaGenerator generator = TestGeneratorFactory.generator("java", JavaGenerator.class);

  @Test
  void testGenerator() {
    generator.run();
  }
}
