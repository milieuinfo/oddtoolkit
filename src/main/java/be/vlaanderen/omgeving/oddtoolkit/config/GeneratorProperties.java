package be.vlaanderen.omgeving.oddtoolkit.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Properties to configure which adapters are used by each generator and generator-specific settings.
 */
@Getter
@Setter
public class GeneratorProperties {
  // Map generatorName -> map of generator-specific properties (adapters, styles, etc.)
  private Map<String, Map<String, Object>> generators = new HashMap<>();

  @SuppressWarnings("unchecked")
  public List<String> adaptersFor(String generatorName) {
    Map<String, Object> cfg = generators.get(generatorName);
    if (cfg == null) return new ArrayList<>();
    Object a = cfg.get("adapters");
    if (a instanceof List) {
      try {
        return (List<String>) a;
      } catch (ClassCastException e) {
        return new ArrayList<>();
      }
    }
    return new ArrayList<>();
  }

  /**
   * Return the raw config map for the generator (may contain arbitrary entries like 'styles').
   */
  public Map<String, Object> configFor(String generatorName) {
    return generators.get(generatorName);
  }

}
