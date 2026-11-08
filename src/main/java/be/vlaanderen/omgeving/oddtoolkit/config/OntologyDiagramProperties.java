package be.vlaanderen.omgeving.oddtoolkit.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Typed configuration for the ontology-diagram generator.
 */
@Getter
@Setter
@ConfigPrefix("generators.ontology-diagram")
public class OntologyDiagramProperties {
  private String outputFile;
  /**
   * When non-empty, only classes from these namespace prefixes are included (e.g. {@code [ssn, sosa]}).
   * The ontology's own namespace (via vann:preferredNamespacePrefix) is always included.
   */
  private List<String> namespaces;
  /**
   * Optional classDef styling per namespace prefix, e.g.
   * <pre>
   * namespace-styles:
   *   ssn:
   *     fill: "#e6f4f5"
   *     stroke: "#007A87"
   * </pre>
   * The generated classDef name is {@code ns_<prefix>}.
   */
  private Map<String, Map<String, Object>> namespaceStyles;
}
