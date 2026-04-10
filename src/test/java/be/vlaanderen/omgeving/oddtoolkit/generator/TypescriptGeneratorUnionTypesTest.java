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
  void aangifteModelHasUnionTypeInTypescript() throws IOException {
    generator.run();

    // Read the generated aangifte.model.ts file
    Path aangiftePath = Paths.get("target/test-cache/typescript/aangifte.model.ts");
    String content = Files.readString(aangiftePath);

    assertNotNull(content, "aangifte.model.ts should be generated");

    // Verify that the union type is in the generated TypeScript code (order-independent)
    boolean hasExpectedUnion = content.contains("(Exploitatie | Observatie)[]")
        || content.contains("(Observatie | Exploitatie)[]");
    assertTrue(hasExpectedUnion,
        "Generated TypeScript should contain a union type for Exploitatie and Observatie");

    // Verify that both types are imported
    assertTrue(content.contains("import { Exploitatie }"),
        "Generated TypeScript should import Exploitatie");
    assertTrue(content.contains("import { Observatie }"),
        "Generated TypeScript should import Observatie");

    // Verify that the decorator uses one of the union members for JSON marshalling
    boolean hasExpectedDecorator = content.contains("@jsonArrayMember(() => Exploitatie")
        || content.contains("@jsonArrayMember(() => Observatie");
    assertTrue(hasExpectedDecorator,
        "Generated TypeScript should use a union member in @jsonArrayMember decorator");
  }
}
