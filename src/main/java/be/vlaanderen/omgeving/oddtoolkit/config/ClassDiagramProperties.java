package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;
/**
 * Typed configuration for the class-diagram generator.
 */
@Getter
@Setter
@ConfigPrefix("generators.class-diagram")
public class ClassDiagramProperties {
  private String outputFile;
  private boolean filterInterfaces = true;
}
