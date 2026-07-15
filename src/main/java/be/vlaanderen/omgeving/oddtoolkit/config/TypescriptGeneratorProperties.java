package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigPrefix("generators.typescript-generator")
public class TypescriptGeneratorProperties {
  private String outputDirectory;
  private boolean cleanupStaleFiles = false;
}
