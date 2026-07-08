package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.TestGeneratorFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link ODCSGenerator}.
 * Verifies that the generator produces valid ODCS (Open Data Contract Standard) contracts
 * with correct structure, metadata, and JSON Schema definitions.
 */
public class ODCSGeneratorTest {

  private final ODCSGenerator generator = TestGeneratorFactory.generator("odcs",
      ODCSGenerator.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testGeneratorRuns() {
    generator.run();
  }

  @Test
  void testGeneratesValidODCS() throws Exception {
    generator.run();

    // Verify the output file exists
    String outputFile = "target/test-cache/odcs/contract.json";
    assertThat(new File(outputFile)).exists();

    // Read and validate the JSON structure
    String content = Files.readString(Paths.get(outputFile));
    JsonNode root = objectMapper.readTree(content);

    // Verify ODCS schema reference
    assertThat(root.has("$schema")).isTrue();
    assertThat(root.get("$schema").asText())
        .contains("odcs-spec");

    // Verify info section
    assertThat(root.has("info")).isTrue();
    JsonNode info = root.get("info");
    assertThat(info.has("name")).isTrue();
    assertThat(info.has("version")).isTrue();
    assertThat(info.get("version").asText()).isEqualTo("1.0.0");

    // Verify schemas section
    assertThat(root.has("schemas")).isTrue();
    JsonNode schemas = root.get("schemas");
    assertThat(schemas.size()).isGreaterThan(0);

    // Verify at least one class schema is generated
    boolean hasClassSchema = false;
    for (JsonNode schema : schemas) {
      if (schema.has("$schema") && schema.has("properties")) {
        hasClassSchema = true;
        break;
      }
    }
    assertThat(hasClassSchema)
        .as("At least one class schema with properties should be generated")
        .isTrue();
  }

  @Test
  void testODCSContainsMetadata() throws Exception {
    generator.run();

    String outputFile = "target/test-cache/odcs/contract.json";
    String content = Files.readString(Paths.get(outputFile));
    JsonNode root = objectMapper.readTree(content);

    JsonNode info = root.get("info");

    // Verify metadata fields
    assertThat(info.has("name")).isTrue();
    assertThat(info.get("name").asText()).isEqualTo("RIE-IEPR Data Contract");

    assertThat(info.has("description")).isTrue();
    assertThat(info.get("description").asText())
        .isEqualTo("Open Data Contract for RIE-IEPR ontology");

    assertThat(info.has("issued")).isTrue();
    assertThat(info.has("updated")).isTrue();

    assertThat(info.has("status")).isTrue();
    assertThat(info.get("status").asText()).isEqualTo("PUBLISHED");
  }

  @Test
  void testODCSContainsSchemas() throws Exception {
    generator.run();

    String outputFile = "target/test-cache/odcs/contract.json";
    String content = Files.readString(Paths.get(outputFile));
    JsonNode root = objectMapper.readTree(content);

    JsonNode schemas = root.get("schemas");
    assertThat(schemas.isObject()).isTrue();
    assertThat(schemas.size()).isGreaterThan(0);

    // Verify schema structure
    for (JsonNode schema : schemas) {
      assertThat(schema.has("type")).isTrue();
      assertThat(schema.get("type").asText()).isEqualTo("object");

      assertThat(schema.has("title")).isTrue();
      assertThat(schema.has("$id")).isTrue();
    }
  }

  @Test
  void testODCSPropertiesHaveTypes() throws Exception {
    generator.run();

    String outputFile = "target/test-cache/odcs/contract.json";
    String content = Files.readString(Paths.get(outputFile));
    JsonNode root = objectMapper.readTree(content);

    JsonNode schemas = root.get("schemas");

    // Find a schema with properties
    boolean foundProperties = false;
    for (JsonNode schema : schemas) {
      if (schema.has("properties")) {
        JsonNode properties = schema.get("properties");
        for (JsonNode property : properties) {
          // Each property should have a type
          assertThat(property.has("type")).isTrue();
          assertThat(property.get("type").asText())
              .isIn("string", "integer", "number", "boolean", "object");
          foundProperties = true;
        }
      }
    }

    assertThat(foundProperties)
        .as("At least one property with a type should be found")
        .isTrue();
  }

  @Test
  void testValidJSON() throws Exception {
    generator.run();

    String outputFile = "target/test-cache/odcs/contract.json";
    String content = Files.readString(Paths.get(outputFile));
    JsonNode root = objectMapper.readTree(content);

    // This will throw an exception if the JSON is invalid
    assertThat(root).isNotNull();

    // Pretty printing should not throw an exception
    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    assertThat(prettyJson).isNotBlank();
  }

  @Test
  void testGeneratorName() {
    assertThat(generator.getName()).isEqualTo("odcs");
  }

  @Test
  void testGeneratorDescription() {
    assertThat(generator.getDescription())
        .contains("ODCS");
  }
}
    
