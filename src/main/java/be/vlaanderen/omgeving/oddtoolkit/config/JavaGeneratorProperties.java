package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigPrefix("generators.java-generator")
public class JavaGeneratorProperties {
  private String outputDirectory;
  private String packageName = "be.vlaanderen.omgeving.oddtoolkit.generated";
}
