package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to register generators with the GeneratorRegistry.
 */
public record GeneratorRegistrationHelper(GeneratorRegistry registry, List<BaseGenerator> generators) {

  private static final Logger logger = LoggerFactory.getLogger(GeneratorRegistrationHelper.class);

  public void registerGenerators() {
    logger.info("Registering {} generators with GeneratorRegistry", generators.size());
    generators.forEach(generator -> registry.register(generator.getName(), generator));
    logger.info("Available generators: {}", String.join(", ", registry.getAvailableGenerators()));
  }
}
