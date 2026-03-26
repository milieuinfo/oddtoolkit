package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Typed configuration for the er-diagram generator.
 */
@Getter
@Setter
@ConfigPrefix("generators.er-diagram")
public class ERDiagramProperties {
  private String outputFile;
}
