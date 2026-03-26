package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

public class SQLGeneratorTest {

  private final SQLGenerator generator = TestGeneratorFactory.generator("sql", SQLGenerator.class);

  @Test
  void testRunGeneratesDiagram() {
    // run generator
    generator.run();
  }
}
