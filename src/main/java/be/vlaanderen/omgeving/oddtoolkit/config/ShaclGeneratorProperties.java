package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigPrefix("generators.shacl-generator")
public class ShaclGeneratorProperties {
  private String outputFile;
}
