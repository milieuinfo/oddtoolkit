package be.vlaanderen.omgeving.oddtoolkit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
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
}

