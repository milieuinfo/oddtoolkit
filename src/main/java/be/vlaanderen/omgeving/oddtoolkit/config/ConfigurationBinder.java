package be.vlaanderen.omgeving.oddtoolkit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binds configuration sections into typed POJOs using @ConfigPrefix.
 */
public final class ConfigurationBinder {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

  private ConfigurationBinder() {
  }

  public static <T> T bind(Map<String, Object> root, Class<T> type, T defaultValue) {
    ConfigPrefix prefix = type.getAnnotation(ConfigPrefix.class);
    if (prefix == null) {
      throw new IllegalArgumentException("Missing @ConfigPrefix on " + type.getName());
    }

    Map<String, Object> section = section(root, prefix.value());
    if (section == null || section.isEmpty()) {
      return defaultValue;
    }
    return MAPPER.convertValue(section, type);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> section(Map<String, Object> root, String dotPath) {
    if (root == null) {
      return null;
    }
    if (dotPath == null || dotPath.isBlank()) {
      return root;
    }

    Object current = root;
    for (String token : dotPath.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        return null;
      }
      current = map.get(token);
      if (current == null) {
        return null;
      }
    }

    if (!(current instanceof Map<?, ?> map)) {
      return null;
    }

    Map<String, Object> out = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (entry.getKey() != null) {
        out.put(entry.getKey().toString(), entry.getValue());
      }
    }
    return out;
  }
}

