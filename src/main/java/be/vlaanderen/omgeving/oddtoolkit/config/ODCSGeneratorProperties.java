package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for the ODCS (Open Data Contract Standard) generator.
 * Allows customization of the generated ODCS output.
 * Maps to the {@code generators.odcs-generator} section in the YAML configuration.
 */
@Getter
@Setter
@ConfigPrefix("generators.odcs-generator")
public class ODCSGeneratorProperties {
  private String outputFile;
  private String contractName = "Data Contract";
  private String contractVersion = "1.0.0";
  private String contractDescription;
  private String ownerName;
  private String ownerEmail;
  private String contactName;
  private String contactEmail;
}


