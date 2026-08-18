package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for union types in TypeScript generation.
 * Verifies that union types are correctly rendered in generated TypeScript code.
 *
 * <p>Uses a small, purpose-built ontology (src/test/resources/examples/union-types.ttl) rather
 * than the RIE-IEPR example ontology, so this mechanism test doesn't break whenever the example
 * domain model evolves.
 */
public class TypescriptGeneratorUnionTypesTest {
  private final TypescriptGenerator generator = TestGeneratorFactory.generator("typescript",
      TypescriptGenerator.class, "src/test/resources/examples/union-types.ttl");

  @Test
  void meetpuntModelHasUnionTypeInTypescript() throws IOException {
    generator.run();

    // Read the generated meetpunt.model.ts file
    Path meetpuntPath = Paths.get("target/test-cache/typescript/meetpunt.model.ts");
    String content = Files.readString(meetpuntPath);

    assertNotNull(content, "meetpunt.model.ts should be generated");

    // Verify that the union type is in the generated TypeScript code (order-independent)
    boolean hasExpectedUnion = content.contains("(Filter | Sensor)[]")
        || content.contains("(Sensor | Filter)[]");
    assertTrue(hasExpectedUnion,
        "Generated TypeScript should contain a union type for Filter and Sensor");

    // Verify that both types are imported
    assertTrue(content.contains("import { Filter }"),
        "Generated TypeScript should import Filter");
    assertTrue(content.contains("import { Sensor }"),
        "Generated TypeScript should import Sensor");

    // Verify that the decorator uses one of the union members for JSON marshalling
    boolean hasExpectedDecorator = content.contains("@jsonArrayMember(() => Filter")
        || content.contains("@jsonArrayMember(() => Sensor");
    assertTrue(hasExpectedDecorator,
        "Generated TypeScript should use a union member in @jsonArrayMember decorator");
  }
}
