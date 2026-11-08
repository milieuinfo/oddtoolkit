package be.vlaanderen.omgeving.oddtoolkit.util;

import org.apache.jena.rdf.model.Model;

/**
 * Determines the Jena language name for an RDF file path or URL based on its file extension, and
 * reads RDF documents from either local paths or remote URLs.
 */
public final class RdfFormat {

  private RdfFormat() {
    // utility class
  }

  /**
   * Reads the RDF document at the given location (local path or http(s) URL) into the model.
   *
   * <p>Remote URLs are read with an explicit language derived from the extension, because servers
   * (e.g. GitHub raw) often serve Turtle as {@code text/plain}, which Jena cannot auto-detect.
   * Local paths use Jena's regular file resolution with format auto-detection, since the
   * explicit-language overload only accepts URLs.
   */
  public static void read(Model model, String location) {
    if (location.startsWith("http://") || location.startsWith("https://")) {
      model.read(location, null, languageFor(location));
    } else {
      model.read(location);
    }
  }

  /**
   * Returns the Jena language name for the given location, or {@code null} to let Jena
   * auto-detect (e.g. from the content type of a remote URL).
   */
  public static String languageFor(String location) {
    if (location == null || location.isBlank()) {
      return null;
    }
    return switch (location.toLowerCase()) {
      case String l when l.endsWith(".ttl") || l.endsWith(".turtle") -> "TURTLE";
      case String l when l.endsWith(".nt") || l.endsWith(".ntriples") -> "N-TRIPLES";
      case String l when l.endsWith(".jsonld") || l.endsWith(".json") -> "JSON-LD";
      case String l when l.endsWith(".rdf") || l.endsWith(".owl") || l.endsWith(".xml") -> "RDF/XML";
      default -> null;
    };
  }
}
