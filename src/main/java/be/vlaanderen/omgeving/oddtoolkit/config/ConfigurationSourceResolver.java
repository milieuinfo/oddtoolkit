package be.vlaanderen.omgeving.oddtoolkit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolver for multi-source configuration.
 * Loads configuration from JSON/YAML or environment variables.
 */
public class ConfigurationSourceResolver {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationSourceResolver.class);
  private static final ObjectMapper jsonMapper = new ObjectMapper();
  private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  /**
   * Load configuration from a JSON or YAML file.
   *
   * @param filePath path to configuration file
   * @return parsed configuration map
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> loadFromFile(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
      return new HashMap<>();
    }

    File file = new File(filePath);
    if (!file.exists()) {
      logger.warn("Configuration file not found: {}", filePath);
      return new HashMap<>();
    }

    try {
      if (filePath.endsWith(".json")) {
        return jsonMapper.readValue(file, Map.class);
      } else if (filePath.endsWith(".yml") || filePath.endsWith(".yaml")) {
        return yamlMapper.readValue(file, Map.class);
      } else {
        logger.warn("Unsupported configuration file format: {}. Supported: .json, .yml, .yaml",
            filePath);
        return new HashMap<>();
      }
    } catch (IOException e) {
      logger.error("Failed to load configuration from file: {}", filePath, e);
      return new HashMap<>();
    }
  }

  /**
   * Merge multiple configuration maps with priority.
   * Later maps override earlier ones.
   *
   * @param configs configuration maps in order of priority
   * @return merged configuration
   */
  public static Map<String, Object> merge(Map<String, Object>... configs) {
    Map<String, Object> result = new HashMap<>();
    for (Map<String, Object> config : configs) {
      if (config != null) {
        result.putAll(config);
      }
    }
    return result;
  }

  /**
   * Get configuration value from environment or with default.
   *
   * @param envKey environment variable name
   * @param defaultValue default value if not found
   * @return configuration value
   */
  public static String getFromEnvOrDefault(String envKey, String defaultValue) {
    String value = System.getenv(envKey);
    return value != null ? value : defaultValue;
  }
}
