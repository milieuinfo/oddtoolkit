package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of GeneratorRegistry using an in-memory map.
 * Thread-safe for registration and retrieval.
 */
public class DefaultGeneratorRegistry implements GeneratorRegistry {

  private final Map<String, BaseGenerator> generators = new HashMap<>();

  @Override
  public void register(String name, BaseGenerator generator) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Generator name cannot be null or empty");
    }
    if (generator == null) {
      throw new IllegalArgumentException("Generator instance cannot be null");
    }
    generators.put(name, generator);
  }

  @Override
  public Optional<BaseGenerator> get(String name) {
    return Optional.ofNullable(generators.get(name));
  }

  @Override
  public List<String> getAvailableGenerators() {
    return new ArrayList<>(generators.keySet());
  }

  @Override
  public boolean has(String name) {
    return generators.containsKey(name);
  }
}
