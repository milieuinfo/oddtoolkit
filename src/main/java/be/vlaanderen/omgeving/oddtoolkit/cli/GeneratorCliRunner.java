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
 *   java -jar oddtoolkit.jar --generator=class-diagram --output=/tmp/output
 *   java -jar oddtoolkit.jar --generator=sql --config-file=custom-config.yml
 *   java -jar oddtoolkit.jar --help
 *
 * Supports:
 * - Generator selection via --generator flag
 * - Configuration file loading (YAML/JSON)
 * - Custom property overrides via --key=value
 * - Environment variable interpolation via ODD_* prefixed variables
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
    System.out.println("""
        ODD Toolkit - Ontology-Driven Development Generator
        
        Usage: java -jar oddtoolkit.jar [OPTIONS]
        
        Options:
          --generator=NAME              Name of the generator to execute
                                        Use --generator=all to execute all registered generators
                                        Available: all, class, class-diagram, er-diagram, sql, shacl, java, typescript
          
          --config-file=PATH            Path to configuration file (YAML or JSON)
                                        Example: --config-file=config.yml
          
          --output=PATH                 Output directory for generated files
                                        Example: --output=/tmp/output
          
          --ontology-file=PATH          Path to ontology file (overrides config file)
                                        Example: --ontology-file=ontology.ttl
          
          --concepts-file=PATH          Path to concepts file (overrides config file)
                                        Example: --concepts-file=concepts.ttl
          
          --help, -h                    Show this help message
        
        Examples:
          # Generate class diagram with default configuration
          java -jar oddtoolkit.jar --generator=class-diagram
          
          # Generate all registered outputs
          java -jar oddtoolkit.jar --generator=all

          # Generate SQL with custom configuration file
          java -jar oddtoolkit.jar --generator=sql --config-file=myconfig.yml
          
          # Generate with custom output directory
          java -jar oddtoolkit.jar --generator=class-diagram --output=/home/user/output
        
        Environment Variables:
          Configuration values can be set via ODD_* prefixed environment variables.
          Example: ODD_GENERATOR_NAME=sql
          
        Configuration Files:
          Supported formats: YAML (.yml, .yaml) and JSON (.json)
          Configuration precedence (highest to lowest):
            1. Command-line arguments (--key=value)
            2. Environment variables (ODD_KEY)
            3. Configuration file
            4. Default values
        """);
  }
}
