package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

public class DataFrameGeneratorTest {
  private final DataFrameGenerator generator = TestGeneratorFactory.generator("data-frame",
      DataFrameGenerator.class);

  @Test
  void testGenerator() {
    generator.run();
  }

}
