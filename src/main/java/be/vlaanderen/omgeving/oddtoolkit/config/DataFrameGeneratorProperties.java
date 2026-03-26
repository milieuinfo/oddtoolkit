package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigPrefix("generators.data-frame-generator")
public class DataFrameGeneratorProperties {
  private String outputFile;
}
