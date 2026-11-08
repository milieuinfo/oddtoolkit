package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyDiagramProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a Mermaid flowchart documenting the ontology itself: classes as plain nodes, subclass
 * relations as red dotted arrows and object property relations as labeled black arrows, so the
 * two relation types stay distinguishable where edges cross. The SQL-oriented
 * class-diagram/er-diagram generators document the application model instead.
 *
 * <p>Nodes are named with the prefix declared in the ontology (e.g. {@code ssn.System}); Mermaid
 * does not accept {@code :} in node names. Colors per namespace prefix can be configured via
 * {@code generators.ontology-diagram.namespace-styles}.
 */
public class OntologyDiagramGenerator extends DiagramGenerator {

  private static final Logger logger = LoggerFactory.getLogger(OntologyDiagramGenerator.class);

  private static final String VANN_PREFERRED_NAMESPACE_PREFIX =
      "http://purl.org/vocab/vann/preferredNamespacePrefix";
  private static final String VANN_PREFERRED_NAMESPACE_URI =
      "http://purl.org/vocab/vann/preferredNamespaceUri";
  private static final String RDFS_LITERAL = "http://www.w3.org/2000/01/rdf-schema#Literal";
  private static final String RDFS_DATATYPE = "http://www.w3.org/2000/01/rdf-schema#Datatype";

  private final OntologyDiagramProperties generatorProperties;

  public OntologyDiagramGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      OntologyDiagramProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties);
    this.generatorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    String diagram = generate("flowchart LR");
    if (getOutputFile() != null) {
      logger.info("Writing ontology diagram to {}", getOutputFile());
      saveDiagram(diagram);
    } else {
      System.out.println(diagram);
    }
  }

  @Override
  protected String getOutputFile() {
    return generatorProperties != null ? generatorProperties.getOutputFile() : null;
  }

  @Override
  protected void renderContent(StringBuilder builder, String type) {
    emitStyleDefinitions(builder);
    Map<String, String> namespaceToPrefix = buildNamespaceToPrefixMap();

    Map<String, ClassInfo> classes = new LinkedHashMap<>();
    for (ClassInfo classInfo : getAllClasses()) {
      if (classInfo.getScope() != Scope.CONCEPTS && classInfo.getUri() != null) {
        classes.putIfAbsent(classInfo.getUri(), classInfo);
      }
    }

    // Object property ranges that are not themselves declared classes also get a node
    Set<String> extraPropertyUris = extraPropertyUris();
    Set<String> nodeUris = new TreeSet<>(classes.keySet());
    for (ClassInfo classInfo : classes.values()) {
      for (PropertyInfo property : classInfo.getProperties()) {
        if (extraPropertyUris.contains(property.getUri())) {
          continue;
        }
        property.getRange().stream()
            .filter(Objects::nonNull)
            .filter(range -> !isDatatypeUri(range))
            .forEach(nodeUris::add);
      }
    }

    // Optional namespace filter; the ontology's own namespace is always kept
    List<String> allowed = generatorProperties != null ? generatorProperties.getNamespaces() : null;
    if (allowed != null && !allowed.isEmpty()) {
      Set<String> allowedPrefixes = new HashSet<>(allowed);
      String ownNamespace = ownNamespace();
      nodeUris.removeIf(uri -> {
        String namespace = namespaceOf(uri, namespaceToPrefix);
        return namespace == null
            || (!allowedPrefixes.contains(namespaceToPrefix.get(namespace))
                && !Objects.equals(namespace, ownNamespace));
      });
    }

    if (nodeUris.isEmpty()) {
      logger.warn("No classes found for the ontology diagram");
      builder.append("%% no classes found\n");
      return;
    }
    logger.info("Rendering ontology diagram with {} classes", nodeUris.size());

    Map<String, String> idByUri = nodeIds(nodeUris, namespaceToPrefix);

    for (String uri : nodeUris) {
      builder.append("%% ").append(uri).append('\n');
      builder.append(idByUri.get(uri))
          .append("[\"")
          .append(displayName(uri, namespaceToPrefix))
          .append("\"]");
      String style = styleFor(uri, classes.get(uri), namespaceToPrefix);
      if (style != null) {
        builder.append(":::").append(style);
      }
      builder.append('\n');
    }

    // Inheritance edges get a distinct style (red dotted) via linkStyle so they stay
    // recognizable even where many edges cross; linkStyle indices follow link order.
    List<Integer> inheritanceEdgeIndices = new ArrayList<>();
    int edgeIndex = 0;
    for (String uri : nodeUris) {
      ClassInfo classInfo = classes.get(uri);
      if (classInfo == null) {
        continue;
      }
      for (ClassInfo superClass : classInfo.getSuperClasses()) {
        if (superClass.getUri() != null && nodeUris.contains(superClass.getUri())) {
          builder.append("%% ").append(uri).append(" rdfs:subClassOf ")
              .append(superClass.getUri()).append('\n');
          builder.append(idByUri.get(uri))
              .append(" -.-> ")
              .append(idByUri.get(superClass.getUri())).append('\n');
          inheritanceEdgeIndices.add(edgeIndex++);
        }
      }
      for (PropertyInfo property : classInfo.getProperties()) {
        if (extraPropertyUris.contains(property.getUri())) {
          continue;
        }
        for (String rangeUri : property.getRange()) {
          if (isDatatypeUri(rangeUri) || !nodeUris.contains(rangeUri)) {
            continue;
          }
          builder.append("%% ").append(property.getUri()).append('\n');
          builder.append(idByUri.get(uri))
              .append(" -->|")
              .append(propertyName(property))
              .append("| ")
              .append(idByUri.get(rangeUri)).append('\n');
          edgeIndex++;
        }
      }
    }

    if (!inheritanceEdgeIndices.isEmpty()) {
      builder.append("linkStyle ")
          .append(String.join(",", inheritanceEdgeIndices.stream().map(String::valueOf).toList()))
          .append(" stroke:#c0392b,stroke-width:2px\n");
    }

    emitNamespaceStyleDefinitions(builder);
  }

  /** Unique, flowchart-safe node ids derived from the display name (dots become underscores). */
  private Map<String, String> nodeIds(Set<String> nodeUris, Map<String, String> namespaceToPrefix) {
    Map<String, String> idByUri = new HashMap<>();
    Set<String> used = new HashSet<>();
    for (String uri : nodeUris) {
      String base = safeIdentifier(displayName(uri, namespaceToPrefix)).replace('.', '_');
      String id = base;
      int suffix = 2;
      while (!used.add(id)) {
        id = base + "_" + suffix++;
      }
      idByUri.put(uri, id);
    }
    return idByUri;
  }

  /**
   * Style for a node: first the shared URI-based styles (generators.diagram-generator.styles),
   * then the optional per-namespace styles (generators.ontology-diagram.namespace-styles).
   */
  @Nullable
  private String styleFor(String uri, @Nullable ClassInfo classInfo,
      Map<String, String> namespaceToPrefix) {
    if (classInfo != null) {
      String shared = getStyleForClass(classInfo);
      if (shared != null) {
        return shared;
      }
    }
    Map<String, Map<String, Object>> namespaceStyles = generatorProperties != null
        ? generatorProperties.getNamespaceStyles()
        : null;
    if (namespaceStyles == null || namespaceStyles.isEmpty()) {
      return null;
    }
    String prefix = prefixOf(uri, namespaceToPrefix);
    if (prefix != null && namespaceStyles.containsKey(prefix)) {
      return "ns_" + safeIdentifier(prefix);
    }
    return null;
  }

  private void emitNamespaceStyleDefinitions(StringBuilder builder) {
    Map<String, Map<String, Object>> namespaceStyles = generatorProperties != null
        ? generatorProperties.getNamespaceStyles()
        : null;
    if (namespaceStyles == null || namespaceStyles.isEmpty()) {
      return;
    }
    namespaceStyles.entrySet().stream()
        .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo)))
        .forEach(entry -> {
          builder.append("classDef ns_").append(safeIdentifier(entry.getKey())).append(" ");
          int i = 0;
          for (Map.Entry<String, Object> prop : entry.getValue().entrySet().stream()
              .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo)))
              .toList()) {
            if (i++ > 0) {
              builder.append(',');
            }
            builder.append(prop.getKey()).append(':').append(prop.getValue());
          }
          builder.append("\n");
        });
  }

  /**
   * Properties added via {@code ontology.extra-properties} are application-model artifacts (e.g.
   * the surrogate uuid/uri columns), not ontology properties; keep them out of the diagram.
   */
  private Set<String> extraPropertyUris() {
    Set<String> uris = new LinkedHashSet<>();
    var configuration = getOntologyConfiguration();
    if (configuration != null && configuration.getExtraProperties() != null) {
      configuration.getExtraProperties().forEach(extra -> {
        if (extra.getUri() != null) {
          uris.add(extra.getUri());
        }
      });
    }
    return uris;
  }

  private static String propertyName(PropertyInfo property) {
    return safeIdentifier(property.getName() != null
        ? property.getName()
        : localName(property.getUri()));
  }

  private static boolean isDatatypeUri(String uri) {
    return uri != null
        && (uri.startsWith(XSD.NS)
            || RDFS_LITERAL.equals(uri)
            || RDFS_DATATYPE.equals(uri));
  }

  /**
   * namespace -> prefix, entirely derived from the ontology: the {@code @prefix} declarations in
   * the file, vann:preferredNamespacePrefix for the ontology's own namespace, and for namespaces
   * that are used but not declared a prefix derived from the namespace URI itself.
   */
  private Map<String, String> buildNamespaceToPrefixMap() {
    Map<String, String> namespaceToPrefix = new LinkedHashMap<>();
    Model model = ontologyInfo.getModel();
    if (model == null) {
      return namespaceToPrefix;
    }
    model.getNsPrefixMap().forEach((prefix, namespace) -> {
      if (prefix != null && !prefix.isBlank() && namespace != null && !namespace.isBlank()) {
        namespaceToPrefix.merge(namespace, prefix, (a, b) -> a.length() <= b.length() ? a : b);
      }
    });

    String own = ownNamespace();
    if (own != null) {
      model.listResourcesWithProperty(RDF.type, OWL2.Ontology).forEachRemaining(ontology -> {
        if (!Objects.equals(ownNamespaceOf(ontology), own)) {
          return;
        }
        org.apache.jena.rdf.model.Statement preferred =
            ontology.getProperty(model.createProperty(VANN_PREFERRED_NAMESPACE_PREFIX));
        if (preferred != null && !preferred.getString().isBlank()) {
          namespaceToPrefix.put(own, preferred.getString());
        }
      });
    }

    Map<String, String> prefixToNamespace = new HashMap<>();
    namespaceToPrefix.forEach((namespace, prefix) -> prefixToNamespace.putIfAbsent(prefix, namespace));
    for (String namespace : observedNamespaces()) {
      if (namespaceToPrefix.containsKey(namespace)) {
        continue;
      }
      String prefix = derivePrefix(namespace);
      String owner = prefixToNamespace.get(prefix);
      if (owner != null && !owner.equals(namespace)) {
        int suffix = 2;
        while (prefixToNamespace.containsKey(prefix + "_" + suffix)) {
          suffix++;
        }
        prefix = prefix + "_" + suffix;
      }
      prefixToNamespace.put(prefix, namespace);
      namespaceToPrefix.put(namespace, prefix);
    }
    return namespaceToPrefix;
  }

  /** All namespaces of the URIs the diagram renders (classes, superclasses, properties, ranges). */
  private Set<String> observedNamespaces() {
    Set<String> namespaces = new TreeSet<>();
    for (ClassInfo classInfo : getAllClasses()) {
      addNamespace(namespaces, classInfo.getUri());
      for (ClassInfo superClass : classInfo.getSuperClasses()) {
        addNamespace(namespaces, superClass.getUri());
      }
      for (PropertyInfo property : classInfo.getProperties()) {
        addNamespace(namespaces, property.getUri());
        for (String range : property.getRange()) {
          addNamespace(namespaces, range);
        }
      }
    }
    return namespaces;
  }

  private static void addNamespace(Set<String> namespaces, String uri) {
    if (uri == null) {
      return;
    }
    int idx = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
    if (idx > 0) {
      namespaces.add(uri.substring(0, idx + 1));
    }
  }

  /**
   * Derives a short prefix from a namespace URI: its fragment, or its last path segment when the
   * fragment is empty (e.g. {@code http://qudt.org/schema/qudt/} -> {@code qudt}).
   */
  static String derivePrefix(String namespace) {
    String candidate = namespace;
    int hash = candidate.lastIndexOf('#');
    if (hash >= 0) {
      candidate = hash < candidate.length() - 1
          ? candidate.substring(hash + 1)
          : candidate.substring(0, hash);
    }
    candidate = candidate.replaceAll("/+$", "");
    int slash = candidate.lastIndexOf('/');
    if (slash >= 0) {
      candidate = candidate.substring(slash + 1);
    }
    candidate = candidate.replaceAll("[^\\p{L}\\p{N}]+", "_");
    candidate = candidate.replaceAll("^_+|_+$", "");
    if (candidate.isEmpty()) {
      return "?";
    }
    return Character.isDigit(candidate.charAt(0)) ? "_" + candidate : candidate;
  }

  /**
   * The namespace the ontology itself is declared in: vann:preferredNamespaceUri when present
   * (authoritative, also for ontologies using a relative {@code <>}), else the ontology resource's
   * own namespace.
   */
  @Nullable
  private String ownNamespace() {
    Model model = ontologyInfo.getModel();
    if (model == null) {
      return null;
    }
    String[] found = new String[1];
    model.listResourcesWithProperty(RDF.type, OWL2.Ontology).forEachRemaining(ontology -> {
      if (found[0] == null) {
        found[0] = ownNamespaceOf(ontology);
      }
    });
    return found[0];
  }

  @Nullable
  private String ownNamespaceOf(org.apache.jena.rdf.model.Resource ontology) {
    org.apache.jena.rdf.model.Statement uriStatement =
        ontology.getProperty(ontology.getModel().createProperty(VANN_PREFERRED_NAMESPACE_URI));
    if (uriStatement != null && !uriStatement.getString().isBlank()) {
      return uriStatement.getString();
    }
    return ontology.getNameSpace();
  }

  @Nullable
  private static String namespaceOf(String uri, Map<String, String> namespaceToPrefix) {
    String longest = null;
    for (String namespace : namespaceToPrefix.keySet()) {
      if (namespace != null && !namespace.isEmpty() && uri.startsWith(namespace)
          && (longest == null || namespace.length() > longest.length())) {
        longest = namespace;
      }
    }
    return longest;
  }

  @Nullable
  private static String prefixOf(String uri, Map<String, String> namespaceToPrefix) {
    String namespace = namespaceOf(uri, namespaceToPrefix);
    return namespace != null ? namespaceToPrefix.get(namespace) : null;
  }

  /** Prefixed display name for a URI, e.g. {@code http://www.w3.org/ns/ssn/System} -> {@code ssn.System}. */
  private static String displayName(String uri, Map<String, String> namespaceToPrefix) {
    String namespace = namespaceOf(uri, namespaceToPrefix);
    if (namespace == null) {
      return safeIdentifier(uri);
    }
    String prefix = namespaceToPrefix.get(namespace);
    String local = uri.substring(namespace.length());
    return safeIdentifier((prefix != null ? prefix + "." : "") + local);
  }

  private static String localName(String uri) {
    if (uri == null) {
      return "?";
    }
    int idx = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
    return idx >= 0 ? uri.substring(idx + 1) : uri;
  }

  /**
   * Makes a string safe to use as a Mermaid identifier: anything outside {@code [A-Za-z0-9_.]} (in
   * particular {@code :}, which Mermaid does not accept in node names) is replaced by {@code _}.
   */
  private static String safeIdentifier(String input) {
    if (input == null) {
      return "?";
    }
    String identifier = input.replaceAll("[^A-Za-z0-9_.]", "_");
    char first = identifier.charAt(0);
    return first >= '0' && first <= '9' ? "_" + identifier : identifier;
  }
}
