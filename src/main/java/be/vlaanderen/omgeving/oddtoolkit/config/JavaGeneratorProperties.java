package be.vlaanderen.omgeving.oddtoolkit.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigPrefix("generators.java-generator")
public class JavaGeneratorProperties {
  private String outputDirectory;
  private String packageName = "be.vlaanderen.omgeving.oddtoolkit.generated";
  private boolean cleanupStaleFiles = false;
  // Jackson annotations so the POJOs serialize as JSON-LD (@type, typed literals, IRI references,
  // enum concept URIs). YAML key: json-ld
  private boolean jsonLd = false;
  // Java type for xsd:dateTime. YAML key: date-time-type (local | offset)
  private DateTimeType dateTimeType = DateTimeType.LOCAL;

  public enum DateTimeType {
    /** {@code java.time.LocalDateTime} (no time zone). */
    LOCAL,
    /** {@code java.time.OffsetDateTime} (timestamp with time zone). */
    OFFSET;

    @JsonCreator
    public static DateTimeType of(String value) {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
  }
}
