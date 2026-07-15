package be.vlaanderen.omgeving.oddtoolkit.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads markdown files from explicit paths or a directory, and converts them to Bikeshed-compatible
 * markup using {@link BikeshedMarkdownConverter}.
 */
public final class MarkdownFileLoader {

  private static final Logger logger = LoggerFactory.getLogger(MarkdownFileLoader.class);

  private MarkdownFileLoader() {
    // utility class
  }

  /** Represents a loaded and converted markdown section. */
  public static class MarkdownSection {
    private final String title;
    private final String markup;

    public MarkdownSection(String title, String markup) {
      this.title = title;
      this.markup = markup;
    }

    public String getTitle() { return title; }
    public String getMarkup() { return markup; }
  }

  /**
   * Load and convert markdown files from an explicit file list.
   *
   * @param filePaths list of file paths (relative to working directory or absolute)
   * @param sectionTitlePrefix optional prefix for section titles (e.g., "Additional Documentation")
   * @return list of converted markdown sections, in the order specified
   */
  public static List<MarkdownSection> loadFromFiles(List<String> filePaths, String sectionTitlePrefix) {
    List<MarkdownSection> sections = new ArrayList<>();
    if (filePaths == null || filePaths.isEmpty()) return sections;

    for (String pathStr : filePaths) {
      Path path = Paths.get(pathStr).toAbsolutePath().normalize();
      if (!Files.exists(path)) {
        logger.warn("Markdown file not found, skipping: {}", path);
        continue;
      }
      try {
        String content = Files.readString(path);
        String sectionTitle = extractSectionTitle(content, path.getFileName().toString(), sectionTitlePrefix);
        String markup = BikeshedMarkdownConverter.convert(content, sectionTitle);
        sections.add(new MarkdownSection(sectionTitle, markup));
        logger.info("Loaded markdown file: {} ({} bytes)", path, content.length());
      } catch (IOException e) {
        logger.warn("Failed to read markdown file {}, skipping: {}", path, e.getMessage());
      }
    }
    return sections;
  }

  /**
   * Load and convert all .md files from a directory.
   * Files are sorted alphabetically by filename.
   * README.md files are excluded (they serve as index/documentation).
   *
   * @param dirPath directory path (relative to working directory or absolute)
   * @param sectionTitlePrefix optional prefix for section titles
   * @return list of converted markdown sections, sorted alphabetically
   */
  public static List<MarkdownSection> loadFromDirectory(String dirPath, String sectionTitlePrefix) {
    List<MarkdownSection> sections = new ArrayList<>();
    if (dirPath == null || dirPath.isBlank()) return sections;

    Path dir = Paths.get(dirPath).toAbsolutePath().normalize();
    if (!Files.isDirectory(dir)) {
      logger.warn("Markdown directory not found: {}", dir);
      return sections;
    }

    try {
      List<Path> mdFiles = Files.list(dir)
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".md"))
          .filter(p -> !p.getFileName().toString().equals("README.md"))
          .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
          .toList();

      for (Path path : mdFiles) {
        String content = Files.readString(path);
        String sectionTitle = extractSectionTitle(content, path.getFileName().toString(), sectionTitlePrefix);
        String markup = BikeshedMarkdownConverter.convert(content, sectionTitle);
        sections.add(new MarkdownSection(sectionTitle, markup));
        logger.info("Loaded markdown file from directory: {} ({} bytes)", path, content.length());
      }
    } catch (IOException e) {
      logger.warn("Failed to list markdown directory {}, skipping: {}", dir, e.getMessage());
    }

    return sections;
  }

  /**
   * Combine multiple markdown sections into a single Bikeshed markup string.
   *
   * @param sections the loaded markdown sections
   * @return combined markup
   */
  public static String combineSections(List<MarkdownSection> sections) {
    if (sections == null || sections.isEmpty()) return "";

    StringBuilder sb = new StringBuilder();
    for (MarkdownSection section : sections) {
      sb.append(section.getMarkup());
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * Extract a section title from markdown content. Falls back to the filename if no heading is found.
   */
  private static String extractSectionTitle(String content, String filename, String prefix) {
    // Try to find the first # heading
    String[] lines = content.split("\n", -1);
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
        return trimmed.substring(2).trim();
      }
    }
    // Fallback: use filename without extension, prefixed
    String baseName = filename.replace(".md", "");
    String readable = baseName.replace("-", " ").replace("_", " ");
    // Capitalize first letter of each word
    StringBuilder result = new StringBuilder();
    boolean capitalizeNext = true;
    for (char c : readable.toCharArray()) {
      if (c == ' ' || c == '-') {
        capitalizeNext = true;
        result.append(c);
      } else if (capitalizeNext) {
        result.append(Character.toUpperCase(c));
        capitalizeNext = false;
      } else {
        result.append(c);
      }
    }
    return prefix + ": " + result.toString();
  }
}
