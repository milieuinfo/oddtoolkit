package be.vlaanderen.omgeving.oddtoolkit.cli;

import be.vlaanderen.omgeving.oddtoolkit.config.CliConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.GeneratorRegistry;
import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CLI runner for executing generators from command line.
 *
 * Usage:
 *   java -jar oddtoolkit.jar --generator=class-diagram --config-file=config.yml
 *   java -jar oddtoolkit.jar --generator=sql --config-file=custom-config.yml
 *   java -jar oddtoolkit.jar --help
 *
 * Supports:
 * - Generator selection via --generator flag
 * - Configuration file loading (YAML/JSON)
 * - Ontology and concept scheme path overrides via --ontology-file / --concepts-file
 *
 * Output locations are configured per generator via generators.&lt;name&gt;.output-file
 * or generators.&lt;name&gt;.output-directory in the configuration file.
 */
public class GeneratorCliRunner {

  private static final Logger logger = LoggerFactory.getLogger(GeneratorCliRunner.class);
  private final GeneratorRegistry generatorRegistry;

  public GeneratorCliRunner(GeneratorRegistry generatorRegistry) {
    this.generatorRegistry = generatorRegistry;
  }

  public void run(String... args) throws Exception {
    CliConfiguration cliConfig = CliConfiguration.fromArgs(args);

    if (cliConfig.isHelpRequested()) {
      printHelp();
      return;
    }

    if (!cliConfig.isValid()) {
      System.err.println("No generator specified. Use --generator=NAME or --generator=all.");
      System.err.println();
      printHelp();
      return;
    }

    if (cliConfig.isAllGeneratorsRequested()) {
      executeAllGenerators();
      return;
    }

    executeGenerator(cliConfig.getGeneratorName().trim());
  }

  private void executeAllGenerators() throws Exception {
    List<String> generatorNames = generatorRegistry.getAvailableGenerators().stream()
        .sorted()
        .toList();

    if (generatorNames.isEmpty()) {
      logger.warn("No generators are registered; nothing to execute.");
      return;
    }

    logger.info("Executing all {} generators: {}", generatorNames.size(), String.join(", ", generatorNames));
    for (String generatorName : generatorNames) {
      executeGenerator(generatorName);
    }
  }

  private void executeGenerator(String generatorName) throws Exception {
    BaseGenerator generator = generatorRegistry.get(generatorName)
        .orElseThrow(() -> new IllegalArgumentException(
            "Generator '" + generatorName + "' is not available. Available: "
                + String.join(", ", generatorRegistry.getAvailableGenerators().stream().sorted().toList())));

    logger.info("Executing generator '{}'", generatorName);
    generator.generate();
  }

  /**
   * Print CLI help message.
   */
  private void printHelp() {
    String available = String.join(", ", generatorRegistry.getAvailableGenerators().stream()
        .sorted()
        .toList());

    System.out.println("""
        ODD Toolkit - Ontology-Driven Development Generator

        Usage: java -jar oddtoolkit.jar --generator=NAME [OPTIONS]

        Options:
          --generator=NAME              Name of the generator to execute
                                        Use --generator=all to execute every registered generator
                                        Available: all, %s

          --config-file=PATH            Path to configuration file (YAML or JSON)
                                        Example: --config-file=config.yml

          --ontology-file=PATH          Path to ontology file (overrides config file)
                                        Example: --ontology-file=ontology.ttl

          --concepts-file=PATH          Path to concept scheme file (overrides config file)
                                        Example: --concepts-file=concepts.ttl

          --help, -h                    Show this help message

        Examples:
          # Generate a class diagram
          java -jar oddtoolkit.jar --generator=class-diagram --config-file=config.yml

          # Generate every registered output
          java -jar oddtoolkit.jar --generator=all --config-file=config.yml

          # Override the ontology without editing the configuration file
          java -jar oddtoolkit.jar --generator=sql --config-file=config.yml \\
              --ontology-file=my-ontology.ttl

        Output locations:
          Each generator writes to the path configured under its own
          generators.<name>.output-file or generators.<name>.output-directory key.
          Generators that support it write to stdout when no output path is set.

        Configuration Files:
          Supported formats: YAML (.yml, .yaml) and JSON (.json)
          Configuration precedence (highest to lowest):
            1. Command-line arguments (--ontology-file, --concepts-file)
            2. Configuration file
            3. Default values
        """.formatted(available));
  }
}
