package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.BikeshedGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a <a href="https://tabatkins.github.io/bikeshed/">Bikeshed</a> specification source
 * file ({@code .bs}) that documents all classes and their properties defined in the ontology.
 *
 * <p>The resulting file can be processed with the {@code bikeshed} CLI tool or the online API at
 * {@code https://api.csswg.org/bikeshed/} to produce a W3C-style HTML specification.
 *
 * <p>Output is controlled by {@link BikeshedGeneratorProperties}; when no
 * {@code outputFile} is configured the generated content is written to stdout.
 */
public class BikeshedGenerator extends BaseGenerator {

  private static final Logger logger = LoggerFactory.getLogger(BikeshedGenerator.class);

  private final BikeshedGeneratorProperties properties;

  public BikeshedGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      BikeshedGeneratorProperties properties) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
    this.properties = properties;
  }

  @Override
  public String getName() {
    return "bikeshed";
  }

  @Override
  public String getDescription() {
    return "Generates a Bikeshed (.bs) specification document from the ontology";
  }

  @Override
  public void run() {
    super.run();
    String content = generateBikeshed();
    if (properties.getOutputFile() != null) {
      saveToFile(properties.getOutputFile(), content);
      logger.info("Bikeshed source written to {}", properties.getOutputFile());
    } else {
      System.out.println(content);
    }
  }


  private String generateBikeshed() {
    StringBuilder sb = new StringBuilder();
    appendMetadataBlock(sb);
    appendIntroduction(sb);
    appendNamespacesSection(sb);
    appendClassesSection(sb);
    return sb.toString();
  }

  private void appendMetadataBlock(StringBuilder sb) {
    String title = resolveTitle();
    String shortname = resolveShortname();

    sb.append("<pre class='metadata'>\n");
    sb.append("Title: ").append(title).append("\n");
    sb.append("Shortname: ").append(shortname).append("\n");
    sb.append("Status: ").append(properties.getStatus()).append("\n");
    sb.append("Level: 1\n");
    sb.append("URL: ").append(ontologyInfo.getUri()).append("\n");
    sb.append("Date: ").append(LocalDate.now()).append("\n");

    // Editor block – only emitted when at least the name is configured
    if (properties.getEditorName() != null && !properties.getEditorName().isBlank()) {
      sb.append("Editor: ").append(properties.getEditorName());
      if (properties.getEditorAffiliation() != null && !properties.getEditorAffiliation().isBlank()) {
        sb.append(", ").append(properties.getEditorAffiliation());
      }
      if (properties.getEditorEmail() != null && !properties.getEditorEmail().isBlank()) {
        sb.append(", ").append(properties.getEditorEmail());
      }
      sb.append("\n");
    }

    sb.append("Abstract: ").append(resolveAbstract()).append("\n");
    sb.append("</pre>\n\n");
  }

  /** Emits a brief introduction / boilerplate section. */
  private void appendIntroduction(StringBuilder sb) {
    sb.append("Introduction {#introduction}\n");
    sb.append("============\n\n");
    sb.append("This document is a machine-generated specification of the ontology ")
        .append("<a href=\"").append(ontologyInfo.getUri()).append("\">")
        .append(resolveTitle())
        .append("</a>.\n\n");
    sb.append("Generated on ").append(LocalDate.now()).append(".\n\n");
  }

  /** Emits a section listing the ontology namespace(s). */
  private void appendNamespacesSection(StringBuilder sb) {
    sb.append("Namespaces {#namespaces}\n");
    sb.append("==========\n\n");
    sb.append("The following namespace is used throughout this specification:\n\n");
    sb.append(": Ontology namespace\n");
    sb.append(":: <code>").append(ontologyInfo.getUri()).append("</code>\n\n");

    if (ontologyInfo.getExternalOntologies() != null
        && !ontologyInfo.getExternalOntologies().isEmpty()) {
      sb.append("External ontologies referenced:\n\n");
      ontologyInfo.getExternalOntologies().forEach((prefix, extOntology) -> {
        sb.append(": ").append(prefix).append("\n");
        sb.append(":: <code>").append(extOntology.getUri()).append("</code>\n");
      });
      sb.append("\n");
    }
  }

  /** Emits one dfn-based section per class defined in the ontology. */
  private void appendClassesSection(StringBuilder sb) {
    List<ClassInfo> classes = getOntologyClasses();
    if (classes.isEmpty()) {
      return;
    }

    sb.append("Classes {#classes}\n");
    sb.append("=======\n\n");

    for (ClassInfo classInfo : classes) {
      appendClassEntry(sb, classInfo);
    }
  }

  private void appendClassEntry(StringBuilder sb, ClassInfo classInfo) {
    String label = resolveLabel(classInfo);

    sb.append("## <dfn>").append(label).append("</dfn> ## {#class-")
        .append(sanitizeAnchor(classInfo.getName())).append("}\n\n");

    // IRI
    sb.append(": <b>IRI</b>\n");
    if (classInfo.getComment() != null && !classInfo.getComment().isBlank()) {
      sb.append(classInfo.getComment()).append("\n\n");
    }

    if (!classInfo.getSuperClasses().isEmpty()) {
      sb.append(": **Sub-class of**\n");
      classInfo.getSuperClasses().stream()
          .filter(Objects::nonNull)
          .forEach(superClass -> {
            String superLabel = resolveLabel(superClass);
            String superUri = superClass.getUri() != null ? superClass.getUri() : "";
            sb.append(":: <a href=\"").append(superUri).append("\">")
                .append(escapeHtml(superLabel)).append("</a>\n");
          });
      sb.append("\n");
    }

    if (!classInfo.getProperties().isEmpty()) {
      appendPropertiesTable(sb, classInfo, classInfo.getProperties());
    }
  }

  private void appendPropertiesTable(StringBuilder sb, ClassInfo classInfo, List<PropertyInfo> properties) {
    String classLabel = resolveLabel(classInfo);
    sb.append(": **Properties**\n");
    sb.append("::\n\n");

    sb.append("    <table class=\"data\">\n");
    sb.append("      <thead>\n");
    sb.append("        <tr>\n");
    sb.append("          <th>Property</th>\n");
    sb.append("          <th>IRI</th>\n");
    sb.append("          <th>Range</th>\n");
    sb.append("          <th>Cardinality</th>\n");
    sb.append("          <th>Description</th>\n");
    sb.append("        </tr>\n");
    sb.append("      </thead>\n");
    sb.append("      <tbody>\n");

    properties.stream()
        .sorted(java.util.Comparator.comparing(
            p -> p.getName() != null ? p.getName() : "",
            java.util.Comparator.nullsLast(String::compareTo)))
        .forEach(prop -> appendPropertyRow(sb, classLabel, prop));

    sb.append("      </tbody>\n");
    sb.append("    </table>\n\n");
  }

  private void appendPropertyRow(StringBuilder sb, String classLabel, PropertyInfo prop) {
    String name = prop.getName() != null ? prop.getName() : "";
    String uri = prop.getUri() != null ? prop.getUri() : "";
    String range = resolveRangeLabel(prop);
    String cardinality = resolveCardinality(prop);
    String description = prop.getComment() != null ? escapeHtml(prop.getComment()) : "";

    sb.append("        <tr>\n");
    // Scope the dfn to the containing class so that identical property names across classes
    // are treated as distinct definitions and do not produce duplicate-id warnings.
    sb.append("          <td><dfn for=\"").append(escapeHtml(classLabel)).append("\">")
        .append(escapeHtml(name)).append("</dfn></td>\n");
    sb.append("          <td><code><a href=\"").append(uri).append("\">")
        .append(escapeHtml(uri)).append("</a></code></td>\n");
    sb.append("          <td>").append(range).append("</td>\n");
    sb.append("          <td>").append(cardinality).append("</td>\n");
    sb.append("          <td>").append(description).append("</td>\n");
    sb.append("        </tr>\n");
  }


  private String resolveTitle() {
    if (properties.getTitle() != null && !properties.getTitle().isBlank()) {
      return properties.getTitle();
    }
    if (ontologyInfo.getLabel() != null && !ontologyInfo.getLabel().isBlank()) {
      return ontologyInfo.getLabel();
    }
    if (ontologyInfo.getName() != null && !ontologyInfo.getName().isBlank()) {
      return ontologyInfo.getName();
    }
    return "Ontology Specification";
  }

  private String resolveShortname() {
    if (properties.getShortname() != null && !properties.getShortname().isBlank()) {
      return properties.getShortname();
    }
    if (ontologyInfo.getName() != null && !ontologyInfo.getName().isBlank()) {
      return ontologyInfo.getName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }
    return "ontology";
  }

  private String resolveAbstract() {
    if (properties.getAbstractText() != null && !properties.getAbstractText().isBlank()) {
      return properties.getAbstractText();
    }
    if (ontologyInfo.getComment() != null && !ontologyInfo.getComment().isBlank()) {
      return ontologyInfo.getComment();
    }
    return "Specification generated from " + ontologyInfo.getUri() + ".";
  }

  private String resolveLabel(ClassInfo classInfo) {
    if (classInfo.getLabel() != null && !classInfo.getLabel().isBlank()) {
      return classInfo.getLabel();
    }
    return classInfo.getName() != null ? classInfo.getName() : classInfo.getUri();
  }

  /** Builds a human-readable range string from the property range URIs. */
  private String resolveRangeLabel(PropertyInfo prop) {
    if (prop.getRange() == null || prop.getRange().isEmpty()) {
      return "";
    }
    return prop.getRange().stream()
        .map(rangeUri -> "<a href=\"" + rangeUri + "\"><code>" + localName(rangeUri) + "</code></a>")
        .reduce((a, b) -> a + " | " + b)
        .orElse("");
  }

  private String resolveCardinality(PropertyInfo prop) {
    PropertyInfo.Cardinality cardTo = prop.getCardinalityTo();
    if (cardTo == null) {
      return "";
    }
    Integer min = cardTo.getMin();
    Integer max = cardTo.getMax();
    if (min == null && max == null) {
      return "";
    }
    String minStr = min != null ? String.valueOf(min) : "0";
    String maxStr = max != null ? String.valueOf(max) : "*";
    return minStr + ".." + maxStr;
  }

  /** Extracts the local name from a URI (fragment or last path segment). */
  private static String localName(String uri) {
    if (uri == null) {
      return "";
    }
    int hash = uri.lastIndexOf('#');
    if (hash >= 0 && hash < uri.length() - 1) {
      return uri.substring(hash + 1);
    }
    int slash = uri.lastIndexOf('/');
    if (slash >= 0 && slash < uri.length() - 1) {
      return uri.substring(slash + 1);
    }
    return uri;
  }

  /** Converts a class name to a safe HTML anchor fragment. */
  private static String sanitizeAnchor(String name) {
    if (name == null) {
      return "unknown";
    }
    return name.toLowerCase().replaceAll("[^a-z0-9-]", "-");
  }

  private static String escapeHtml(String text) {
    if (text == null) {
      return "";
    }
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}

