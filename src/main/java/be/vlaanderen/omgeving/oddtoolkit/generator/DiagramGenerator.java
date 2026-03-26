package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.util.MermaidExporter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared base for diagram generators (class diagram, ER diagram, ...). Centralizes generation flow,
 * style handling and file output. Subclasses must provide style entries (if any) and may override
 * output file.
 */
public abstract class DiagramGenerator extends ClassGenerator {

  private static final Logger logger = LoggerFactory.getLogger(DiagramGenerator.class);
  protected final DiagramGeneratorProperties diagramGeneratorProperties;
  protected final Map<String, String> stylesMap = new LinkedHashMap<>();

  public DiagramGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo, List<AbstractAdapter<?>> adapters,
      DiagramGeneratorProperties diagramGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
    this.diagramGeneratorProperties = diagramGeneratorProperties;
  }

  @Override
  public void run() {
    super.run();
    prepareStyleDefinitions();
  }

  protected void saveDiagram(String diagramContent) {
    String outputFile = getOutputFile();
    if (outputFile != null) {
      saveToFile(outputFile, diagramContent);
      if (diagramGeneratorProperties == null || diagramGeneratorProperties.isExportPng()) {
        // Save as high-resolution PNG with the same base filename when enabled
        saveDiagramAsPng(outputFile, diagramContent);
      } else {
        logger.info("PNG export disabled by configuration (generators.diagram-generator.export-png=false)");
      }
    } else {
      System.out.println(diagramContent);
    }
  }

  /**
   * Exports the Mermaid diagram content to a high-resolution PNG file.
   * The PNG file is saved with the same name as the Mermaid file but with .png extension.
   *
   * @param mermaidFilePath the path to the Mermaid diagram file
   * @param diagramContent the Mermaid diagram content
   */
  private void saveDiagramAsPng(String mermaidFilePath, String diagramContent) {
    try {
      String pngFilePath = mermaidFilePath.replaceAll("\\.[^.]+$", ".png");
      logger.info("Exporting diagram to PNG: {}", pngFilePath);
      MermaidExporter.exportToPng(diagramContent, pngFilePath);
      logger.info("Successfully exported diagram to PNG: {}", pngFilePath);
    } catch (IOException e) {
      logger.error("Failed to export diagram to PNG", e);
    } catch (LinkageError | RuntimeException e) {
      logger.warn("PNG export skipped: Playwright runtime is unavailable ({}). "
          + "Set generators.diagram-generator.export-png=false to suppress this warning.",
          e.getMessage());
    }
  }

  protected void emitStyleDefinitions(StringBuilder builder) {
    List<DiagramStyle> styles = getStyleEntries();
    if (styles == null) {
      return;
    }
    for (DiagramStyle style : styles) {
      // Emit the classDef once per style name (props are shared even if multiple URIs map to the same style)
      if (style != null && style.name != null && style.props != null) {
        builder.append("classDef ").append(style.name).append(" ");
        int i = 0;
        for (Map.Entry<String, Object> e : style.props.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo)))
            .toList()) {
          if (i++ > 0) {
            builder.append(',');
          }
          builder.append(e.getKey()).append(":").append(e.getValue());
        }
        builder.append("\n");
      }
    }
  }

  protected List<DiagramStyle> getStyleEntries() {
    if (diagramGeneratorProperties == null || diagramGeneratorProperties.getStyles() == null) {
      return List.of();
    }
    List<DiagramStyle> res = new ArrayList<>();
    diagramGeneratorProperties.getStyles()
        .forEach(s -> {
          // Support both 'uris' (list) and legacy single 'uri' entries
          List<String> uris = s.getUris();
          if (uris == null || uris.isEmpty()) {
            if (s.getUri() != null) {
              uris = List.of(s.getUri());
            }
          }
          res.add(new DiagramStyle(s.getName(), uris, s.getProps()));
        });
    return res;
  }

  /**
   * Hook: subclasses may return an output file path. Default: null -> stdout.
   */
  protected String getOutputFile() {
    return null;
  }

  private void prepareStyleDefinitions() {
    stylesMap.clear();
    List<DiagramStyle> styles = getStyleEntries();
    if (styles == null) {
      return;
    }
    for (DiagramStyle style : styles) {
      if (style != null && style.uris != null && style.name != null && style.props != null) {
        for (String uri : style.uris) {
          if (uri != null) {
            stylesMap.put(uri, style.name);
          }
        }
      }
    }
  }

  /**
   * Core generation flow. Subclasses provide the content by implementing protected helpers
   * (generateClass/generateProperty/etc.) or by calling their own helpers from within the
   * generation.
   */
  protected String generate(String type) {
    StringBuilder builder = new StringBuilder();
    builder.append("---\n");
    builder.append("config:\n");
    builder.append("  theme: default\n");
    builder.append("  layout: elk\n");
    builder.append("  elk:\n");
    builder.append("    nodePlacementStrategy: SIMPLE\n");
    builder.append("---\n");
    builder.append("%% Generated by ODDToolkit\n");
    builder.append(type).append("\n");

    // subclasses are expected to emit the main content (classes, relations, properties)
    // They can use the protected helpers retained in ClassDiagramGenerator.
    // Call a lifecycle hook implemented by subclasses to append content.
    renderContent(builder, type);

    // Note: style emission is the responsibility of subclasses (classDiagram/erDiagram)

    // Post-process: indent every line after the first block header with a tab
    String diagram = builder.toString();
    String[] lines = diagram.split("\\R", -1);
    int firstLineAfterType = -1;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].strip().equals(type)) {
        firstLineAfterType = i;
        break;
      }
    }
    if (firstLineAfterType == -1) {
      return diagram;
    }
    StringBuilder out = new StringBuilder();
    for (int i = 0; i <= firstLineAfterType; i++) {
      out.append(lines[i]);
      out.append('\n');
    }
    out.append('\n');
    for (int i = firstLineAfterType + 1; i < lines.length; i++) {
      out.append('\t').append(lines[i]);
      if (i < lines.length - 1) {
        out.append('\n');
      }
    }
    return out.toString();
  }

  /**
   * Return the style id (class name) for the given class if a style was configured for its URI or
   * for any of its superclasses. Returns null when no style applies.
   */
  protected String getStyleForClass(ClassInfo classInfo) {
    if (classInfo == null) {
      return null;
    }
    // direct match
    String uri = classInfo.getUri();
    if (uri != null && stylesMap.containsKey(uri)) {
      return stylesMap.get(uri);
    }
    // Loop through the styles
    for (Map.Entry<String, String> entry : stylesMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (classInfo.isSubClassOf(key)) {
        return value;
      }
    }
    return null;
  }

  /**
   * Subclasses must implement this to render the diagram body (classes, relations, ...). The base
   * will call this during generation.
   */
  protected void renderContent(StringBuilder builder, String type) {

  }

  /**
   * Minimal DTO that subclasses can use to provide style configuration.
   */
  protected record DiagramStyle(String name, List<String> uris, Map<String, Object> props) {

  }
}
