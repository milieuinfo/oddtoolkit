package be.vlaanderen.omgeving.oddtoolkit.generator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ShaclGeneratorTest {
  private final ShaclGenerator generator = TestGeneratorFactory.generator("shacl", ShaclGenerator.class);

  @Test
  void testGenerator() throws IOException {
    generator.run();

    Path output = Path.of("target/test-cache/shacl/schema.ttl");
    assertTrue(Files.exists(output), "SHACL output file should exist");
    assertTrue(Files.size(output) > 0, "SHACL output file should not be empty");
    assertFalse(Files.readString(output).isBlank(), "SHACL output should contain triples");
  }
}
