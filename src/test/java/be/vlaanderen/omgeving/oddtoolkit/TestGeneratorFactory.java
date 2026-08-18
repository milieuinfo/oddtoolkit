package be.vlaanderen.omgeving.oddtoolkit;

import be.vlaanderen.omgeving.oddtoolkit.config.GeneratorRegistry;
import be.vlaanderen.omgeving.oddtoolkit.config.OddtoolkitBootstrap;
import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TestGeneratorFactory {

  private static final GeneratorRegistry REGISTRY = OddtoolkitBootstrap.bootstrap(
      new String[]{"--config-file=src/test/resources/application.yml"});

  private static final Map<String, GeneratorRegistry> REGISTRIES_BY_ONTOLOGY = new ConcurrentHashMap<>();

  private TestGeneratorFactory() {
  }

  @SuppressWarnings("unchecked")
  public static <T extends BaseGenerator> T generator(String name, Class<T> type) {
    return (T) REGISTRY.get(name)
        .orElseThrow(() -> new IllegalStateException("Missing generator in test registry: " + name));
  }

  /**
   * Bootstraps (once per path, then cached) a registry that uses the standard test
   * configuration but with the ontology file swapped out. Use this for tests that exercise a
   * generic generator mechanism with a small, purpose-built ontology, so they stay decoupled
   * from the (evolving) RIE-IEPR example ontology used by {@link #generator(String, Class)}.
   */
  @SuppressWarnings("unchecked")
  public static <T extends BaseGenerator> T generator(String name, Class<T> type, String ontologyFilePath) {
    GeneratorRegistry registry = REGISTRIES_BY_ONTOLOGY.computeIfAbsent(ontologyFilePath, path ->
        OddtoolkitBootstrap.bootstrap(new String[]{
            "--config-file=src/test/resources/application.yml",
            "--ontology-file=" + path
        }));
    return (T) registry.get(name)
        .orElseThrow(() -> new IllegalStateException("Missing generator in test registry: " + name));
  }
}

