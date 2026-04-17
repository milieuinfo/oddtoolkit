package be.vlaanderen.omgeving.oddtoolkit.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for the Bikeshed documentation generator.
 * Maps to the {@code generators.bikeshed-generator} section in the YAML configuration.
 */
@Getter
@Setter
@ConfigPrefix("generators.bikeshed-generator")
public class BikeshedGeneratorProperties {

  /** Path where the generated .bs (Bikeshed source) file will be written. */
  private String outputFile;

  /** Specification title shown in the Bikeshed metadata block. Defaults to the ontology name. */
  private String title;

  /**
   * Specification status, e.g. {@code LS}, {@code ED}, {@code WD}, {@code CR}, {@code PR}, {@code REC}.
   * Defaults to {@code LS} (Living Standard / Editor's Draft).
   */
  private String status = "LS";

  /**
   * Short name / slug used by Bikeshed to build the TR URL, e.g. {@code my-ontology}.
   * Defaults to the local name of the ontology URI.
   */
  private String shortname;

  /** Editor name shown in the Bikeshed metadata block. */
  private String editorName;

  /** Editor e-mail shown in the Bikeshed metadata block. */
  private String editorEmail;

  /** Editor affiliation (organisation) shown in the Bikeshed metadata block. */
  private String editorAffiliation;

  /**
   * Optional abstract text inserted below the auto-generated abstract.
   * When omitted the ontology {@code rdfs:comment} is used.
   */
  private String abstractText;
}

