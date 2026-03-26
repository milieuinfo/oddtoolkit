package be.vlaanderen.omgeving.oddtoolkit.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Typed configuration for general diagram generators.
 */
@Getter
@Setter
@ConfigPrefix("generators.diagram-generator")
public class DiagramGeneratorProperties {
  private String outputFile;
  private List<StyleEntry> styles;
  // Set to false to skip PNG rendering (useful for thin runtime without Playwright internals).
  private boolean exportPng = true;

  @Getter
  @Setter
  public static class StyleEntry {
    private String name;
    private String uri;
    private List<String> uris;
    private Map<String, Object> props;
  }
}
