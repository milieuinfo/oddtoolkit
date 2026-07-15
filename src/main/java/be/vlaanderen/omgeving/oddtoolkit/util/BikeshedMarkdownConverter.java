package be.vlaanderen.omgeving.oddtoolkit.util;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.AttributeProviderFactory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.html.MutableAttributes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Converts CommonMark / GFM markdown content to Bikeshed-compatible markup using flexmark-java.
 *
 * <p>Bikeshed's {@code Markup Shorthands: markdown yes} supports basic markdown but does NOT
 * fully render GFM-style pipe tables. This converter uses flexmark-java (v0.64.8) with the
 * TablesExtension to properly parse and render GFM tables as HTML, then wraps the output
 * in Bikeshed-compatible section headings when a section title is provided.
 *
 * <p>Key transformations:
 * <ul>
 *   <li>Headings → Bikeshed heading syntax with anchors: {@code ## Title ## {#anchor}}</li>
 *   <li>Tables → {@code <table class="data">} HTML (via custom AttributeProvider)</li>
 *   <li>Code blocks, lists, blockquotes, bold/italic, links → standard HTML passed through</li>
 * </ul>
 */
public final class BikeshedMarkdownConverter {

  /** DataKey for Bikeshed heading level override (used internally). */
  private static final DataKey<Integer> BIKESEAD_HEADING_LEVEL = new DataKey<>("BIKESEAD_HEADING_LEVEL", 2);

  /** Reusable flexmark parser and renderer — thread-safe after build. */
  private static final Parser PARSER;
  private static final HtmlRenderer RENDERER;

  static {
    MutableDataSet options = new MutableDataSet();

    // Enable GFM extensions
    options.set(Parser.EXTENSIONS, List.of(
        TablesExtension.create(),
        StrikethroughExtension.create()
    ));

    PARSER = Parser.builder(options).build();
    RENDERER = HtmlRenderer.builder(options)
        .attributeProviderFactory(TableDataAttributeProvider.Factory())
        .build();
  }

  private BikeshedMarkdownConverter() {
    // utility class
  }

  /**
   * Convert markdown string to Bikeshed-compatible markup.
   *
   * @param markdown the raw markdown content
   * @param sectionTitle optional section title; if provided, a Bikeshed heading is prepended
   * @return the converted markup
   */
  public static String convert(String markdown, String sectionTitle) {
    if (markdown == null || markdown.isBlank()) {
      return "";
    }

    StringBuilder result = new StringBuilder();

    // If a section title is provided, emit a Bikeshed heading
    if (sectionTitle != null && !sectionTitle.isBlank()) {
      String anchor = toBikeshadAnchorInternal(sectionTitle);
      result.append("## ").append(escapeMarkdown(sectionTitle)).append(" ## {#").append(anchor).append("}\n\n");
    }

    // Parse markdown → AST → HTML using flexmark
    Node document = PARSER.parse(markdown);
    String html = RENDERER.render(document);

    // Post-process: convert <h1>...</h1> headings to Bikeshed heading syntax with anchors
    String processedHtml = convertHeadingsToBikeshad(html);

    result.append(processedHtml);

    return result.toString();
  }

  /** Convert markdown to Bikeshed markup without a section title wrapper. */
  public static String convert(String markdown) {
    return convert(markdown, null);
  }

  // ---- Heading conversion: <hN>text</hN> → Bikeshad heading with anchor ----

  private static String convertHeadingsToBikeshad(String html) {
    if (html == null || html.isEmpty()) {
      return html;
    }

    // Use regex to find and replace all <hN>...</hN> patterns (including multi-line)
    java.util.regex.Pattern headingPattern = java.util.regex.Pattern.compile(
        "<h([1-6])>(.*?)</h\\1>", java.util.regex.Pattern.DOTALL);
    
    java.util.regex.Matcher matcher = headingPattern.matcher(html);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      int level = Integer.parseInt(matcher.group(1));
      String text = matcher.group(2).trim();
      // Escape HTML entities first (for security)
      text = escapeMarkdownInline(text);
      // Then strip any remaining HTML tags (e.g., <strong>, <em> from inline formatting)
      text = text.replaceAll("<[^>]+>", "");
      
      String anchor = toBikeshadAnchorInternal(text);
      String headingMarks = "#".repeat(level);

      // Bikeshad heading syntax: ## Title ## {#anchor}
      matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(
          headingMarks + " " + text + " " + headingMarks + " {#" + anchor + "}"));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  // ---- Anchor generation ----

  /** Convert a string to a Bikeshad-safe anchor ID. */
  private static String toBikeshadAnchorInternal(String text) {
    if (text == null || text.isBlank()) return "unknown";
    return text.toLowerCase()
        .replaceAll("[^a-z0-9\\s-]", "")
        .replace(' ', '-')
        .replaceAll("-+", "-")
        .replace("-", "-")
        .replaceFirst("^-|-$", "");
  }

  // ---- HTML escaping helpers ----

  /** Escape HTML entities in raw content (code blocks, table cells). */
  static String escapeHtml(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;")
               .replace("\"", "&quot;");
  }

  /** Escape HTML in markdown inline content (headings). */
  static String escapeMarkdownInline(String text) {
    if (text == null) return "";
    // Process links first to avoid double-escaping link URLs
    text = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)")
        .matcher(text).replaceAll("[${1}](${2})");
    // Escape remaining HTML
    text = escapeHtml(text);
    // Restore inline code (already escaped above, so un-escape the content inside backticks)
    text = java.util.regex.Pattern.compile("`([^`]+)`")
        .matcher(text).replaceAll("`$1`");
    return text;
  }

  /** Escape markdown special characters for heading text. */
  static String escapeMarkdown(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;");
  }

  // ---- Custom AttributeProvider for table class="data" ----

  /** Adds {@code class="data"} attribute to <table> elements for Bikeshad styling. */
  private static class TableDataAttributeProvider implements AttributeProvider {
    @Override
    public void setAttributes(@NotNull Node node, @NotNull AttributablePart part,
                              @NotNull MutableAttributes attributes) {
      // flexmark table block nodes have class name containing "TableBlock"
      if (node.getClass().getSimpleName().contains("TableBlock")) {
        attributes.replaceValue("class", "data");
      }
    }

    static AttributeProviderFactory Factory() {
      return new IndependentAttributeProviderFactory() {
        @NotNull
        @Override
        public AttributeProvider apply(@NotNull com.vladsch.flexmark.html.renderer.LinkResolverContext context) {
          return new TableDataAttributeProvider();
        }
      };
    }
  }
}
