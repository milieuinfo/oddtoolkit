package be.vlaanderen.omgeving.oddtoolkit.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Unified configuration resolved from multiple sources.
 * Supports CLI arguments, environment variables, and configuration files.
 *
 * Configuration precedence (highest to lowest):
 * 1. Command-line arguments (--key=value)
 * 2. Environment variables (ODD_KEY)
 * 3. Configuration file (YAML/JSON)
 * 4. Default values
 */
@Getter
@Setter
public class CliConfiguration {

  private String generatorName;
  private String configFile;
  private String ontologyFilePath;
  private String conceptsFilePath;
  private String outputPath;
  private String outputFormat;
  private Map<String, String> customProperties = new HashMap<>();
  private boolean helpRequested = false;

  /**
   * Parse command-line arguments in the format:
   * --generator=class --config-file=config.yml --output=/tmp/output
   *
   * @param args command-line arguments
   * @return parsed configuration
   */
  public static CliConfiguration fromArgs(String[] args) {
    CliConfiguration config = new CliConfiguration();

    for (String arg : args) {
      if (arg.startsWith("--")) {
        String cleanArg = arg.substring(2);
        if (cleanArg.contains("=")) {
          String[] parts = cleanArg.split("=", 2);
          String key = parts[0];
          String value = parts[1].trim();

          switch (key) {
            case "generator" -> config.setGeneratorName(value);
            case "config-file" -> config.setConfigFile(value);
            case "ontology-file" -> config.setOntologyFilePath(value);
            case "concepts-file" -> config.setConceptsFilePath(value);
            case "output" -> config.setOutputPath(value);
            case "output-format" -> config.setOutputFormat(value);
            case "help", "h" -> config.setHelpRequested(true);
            default -> config.customProperties.put(key, value);
          }
        }
      } else if (arg.equals("--help") || arg.equals("-h")) {
        config.setHelpRequested(true);
      }
    }

    return config;
  }

  /**
   * Check if required configuration is present.
   *
   * @return true if generator name is specified
   */
  public boolean isValid() {
    return generatorName != null && !generatorName.trim().isEmpty();
  }

  /**
   * Get environment variable for configuration key.
   * Converts key to uppercase and replaces hyphens with underscores.
   * Example: --generator-name becomes ODD_GENERATOR_NAME
   *
   * @param key the configuration key
   * @return environment variable value or null
   */
  public static String getEnvValue(String key) {
    String envKey = "ODD_" + key.toUpperCase().replace("-", "_");
    return System.getenv(envKey);
  }

  /**
   * Check if the special "all generators" mode is requested.
   *
   * @return true if the generator name is "all" (case-insensitive)
   */
  public boolean isAllGeneratorsRequested() {
    return generatorName != null && "all".equalsIgnoreCase(generatorName.trim());
  }

  @Override
  public String toString() {
    return "CliConfiguration{" +
        "generatorName='" + generatorName + '\'' +
        ", configFile='" + configFile + '\'' +
        ", ontologyFilePath='" + ontologyFilePath + '\'' +
        ", conceptsFilePath='" + conceptsFilePath + '\'' +
        ", outputPath='" + outputPath + '\'' +
        ", outputFormat='" + outputFormat + '\'' +
        ", customProperties=" + customProperties +
        ", helpRequested=" + helpRequested +
        '}';
  }
}
