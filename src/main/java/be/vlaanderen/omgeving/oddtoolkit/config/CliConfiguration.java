package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Unified configuration resolved from the command line and a configuration file.
 *
 * Configuration precedence (highest to lowest):
 * 1. Command-line arguments (--ontology-file, --concepts-file)
 * 2. Configuration file (YAML/JSON)
 * 3. Default values
 */
@Getter
@Setter
public class CliConfiguration {

  private String generatorName;
  private String configFile;
  private String ontologyFilePath;
  private String conceptsFilePath;
  private boolean helpRequested = false;

  /**
   * Parse command-line arguments in the format:
   * --generator=class --config-file=config.yml
   *
   * @param args command-line arguments
   * @return parsed configuration
   */
  public static CliConfiguration fromArgs(String[] args) {
    CliConfiguration config = new CliConfiguration();

    for (String arg : args) {
      if (arg.equals("--help") || arg.equals("-h")) {
        config.setHelpRequested(true);
        continue;
      }

      if (!arg.startsWith("--")) {
        System.err.println("Ignoring unexpected argument: " + arg);
        continue;
      }

      String cleanArg = arg.substring(2);
      int separator = cleanArg.indexOf('=');
      if (separator < 0) {
        System.err.println("Unknown or malformed option: " + arg
            + " (options use --key=value; run --help)");
        continue;
      }

      String key = cleanArg.substring(0, separator);
      String value = cleanArg.substring(separator + 1).trim();

      switch (key) {
        case "generator" -> config.setGeneratorName(value);
        case "config-file" -> config.setConfigFile(value);
        case "ontology-file" -> config.setOntologyFilePath(value);
        case "concepts-file" -> config.setConceptsFilePath(value);
        default -> System.err.println("Unknown option: --" + key + " (run --help)");
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
        ", helpRequested=" + helpRequested +
        '}';
  }
}
