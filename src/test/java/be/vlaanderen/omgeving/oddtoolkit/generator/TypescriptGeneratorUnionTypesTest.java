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
 */
public class TypescriptGeneratorUnionTypesTest {
  private final TypescriptGenerator generator = TestGeneratorFactory.generator("typescript",
      TypescriptGenerator.class);

  @Test
  void meetpuntModelHasUnionTypeInTypescript() throws IOException {
    generator.run();

    // Read the generated meetpunt.model.ts file
    Path meetpuntPath = Paths.get("target/test-cache/typescript/meetpunt.model.ts");
    String content = Files.readString(meetpuntPath);

    assertNotNull(content, "meetpunt.model.ts should be generated");

    // Verify that the union type is in the generated TypeScript code (order-independent)
    boolean hasExpectedUnion = content.contains("(MeetInstrument | Filter)[]")
        || content.contains("(Filter | MeetInstrument)[]");
    assertTrue(hasExpectedUnion,
        "Generated TypeScript should contain a union type for MeetInstrument and Filter");

    // Verify that both types are imported
    assertTrue(content.contains("import { MeetInstrument }"),
        "Generated TypeScript should import MeetInstrument");
    assertTrue(content.contains("import { Filter }"),
        "Generated TypeScript should import Filter");

    // Verify that the decorator uses one of the union members for JSON marshalling
    boolean hasExpectedDecorator = content.contains("@jsonArrayMember(() => MeetInstrument")
        || content.contains("@jsonArrayMember(() => Filter");
    assertTrue(hasExpectedDecorator,
        "Generated TypeScript should use a union member in @jsonArrayMember decorator");
  }
}
