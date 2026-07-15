package be.vlaanderen.omgeving.oddtoolkit.config;

import java.util.List;
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

  // --- Markdown file inclusion settings ---

  /**
   * List of markdown file paths to include in the Bikeshed output.
   * Paths are resolved relative to the project root (working directory).
   * Files are included in the order specified.
   */
  private List<String> markdownFiles;

  /**
   * Alternative to {@link #markdownFiles}: a directory path whose .md files are
   * collected and sorted alphabetically for inclusion in the Bikeshed output.
   * When both {@code markdownFiles} and {@code markdownDirectory} are set,
   * only {@code markdownFiles} is used (with a warning).
   */
  private String markdownDirectory;

  /**
   * Section title for the combined markdown content section.
   * Defaults to "Additional Documentation".
   */
  private String markdownSectionTitle = "Additional Documentation";

  /**
   * When true, markdown content is inserted after the Classes section.
   * When false, it is appended at the very end of the document.
   * Defaults to true.
   */
  private Boolean markdownAppendAfterClasses = true;

  /**
   * When true, GFM-style pipe tables in markdown are converted to Bikeshed-compatible
   * {@code <table class="data">} HTML elements. This is necessary because Bikeshed's
   * native markdown shorthand does not fully support GFM tables.
   * Defaults to true.
   */
  private Boolean markdownConvertTables = true;
}

