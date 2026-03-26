package be.vlaanderen.omgeving.oddtoolkit;

import be.vlaanderen.omgeving.oddtoolkit.config.GeneratorRegistry;
import be.vlaanderen.omgeving.oddtoolkit.config.OddtoolkitBootstrap;
import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;

public final class TestGeneratorFactory {

  private static final GeneratorRegistry REGISTRY = OddtoolkitBootstrap.bootstrap(
      new String[]{"--config-file=src/test/resources/application.yml"});

  private TestGeneratorFactory() {
  }

  @SuppressWarnings("unchecked")
  public static <T extends BaseGenerator> T generator(String name, Class<T> type) {
    return (T) REGISTRY.get(name)
        .orElseThrow(() -> new IllegalStateException("Missing generator in test registry: " + name));
  }
}

