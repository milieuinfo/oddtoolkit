package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigPrefix("generators.sql-generator")
public class SQLGeneratorProperties {
  private String outputFile;
}
