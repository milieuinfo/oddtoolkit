package be.vlaanderen.omgeving.oddtoolkit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigurationBinderTest {

  @Test
  void bindUsesPrefixAndKebabCaseKeys() {
    Map<String, Object> root = new LinkedHashMap<>();
    Map<String, Object> generators = new LinkedHashMap<>();
    Map<String, Object> javaGenerator = new LinkedHashMap<>();
    javaGenerator.put("output-directory", "target/gen");
    javaGenerator.put("package-name", "be.example.generated");
    generators.put("java-generator", javaGenerator);
    root.put("generators", generators);

    JavaGeneratorProperties defaults = new JavaGeneratorProperties();
    JavaGeneratorProperties bound = ConfigurationBinder.bind(root, JavaGeneratorProperties.class,
        defaults);

    assertEquals("target/gen", bound.getOutputDirectory());
    assertEquals("be.example.generated", bound.getPackageName());
  }

  @Test
  void bindReturnsDefaultsWhenSectionMissing() {
    JavaGeneratorProperties defaults = new JavaGeneratorProperties();
    defaults.setOutputDirectory("target/default");

    JavaGeneratorProperties bound = ConfigurationBinder.bind(Map.of(),
        JavaGeneratorProperties.class, defaults);

    assertTrue(bound == defaults);
    assertEquals("target/default", bound.getOutputDirectory());
  }

  @Test
  void bindSupportsOverridePropertyRangeIdentifierAndLegacyDatatype() {
    Map<String, Object> root = new LinkedHashMap<>();
    Map<String, Object> ontology = new LinkedHashMap<>();

    Map<String, Object> primaryOverride = new LinkedHashMap<>();
    primaryOverride.put("uri", "https://example.org/id");
    primaryOverride.put("name", "uuid");
    primaryOverride.put("comment", "Primary identifier");
    primaryOverride.put("range", "http://www.w3.org/2001/XMLSchema#string");
    primaryOverride.put("identifier", true);

    Map<String, Object> legacyOverride = new LinkedHashMap<>();
    legacyOverride.put("uri", "https://example.org/label");
    legacyOverride.put("datatype", "http://www.w3.org/2001/XMLSchema#string");

    ontology.put("override-properties", List.of(primaryOverride, legacyOverride));
    root.put("ontology", ontology);

    OntologyConfiguration bound = ConfigurationBinder.bind(root, OntologyConfiguration.class,
        new OntologyConfiguration());

    assertEquals(2, bound.getOverrideProperties().size());
    assertEquals("uuid", bound.getOverrideProperties().get(0).getName());
    assertEquals("Primary identifier", bound.getOverrideProperties().get(0).getComment());
    assertEquals("http://www.w3.org/2001/XMLSchema#string",
        bound.getOverrideProperties().get(0).getRange());
    assertTrue(bound.getOverrideProperties().get(0).getIdentifier());
    assertEquals("http://www.w3.org/2001/XMLSchema#string",
        bound.getOverrideProperties().get(1).getDatatype());
  }
}
