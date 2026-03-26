package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import be.vlaanderen.omgeving.oddtoolkit.config.ConfigPrefix;
import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.attribute.FileTime;

/**
 * Adapter responsible for fetching external ontologies referenced through owl:imports
 */
@AdapterDependency({
    OntologyLoadAdapter.class
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-extract-external.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyExtractExternalAdapter extends AbstractAdapter<OntologyInfo> {

  private static final Logger logger = LoggerFactory.getLogger(
      OntologyExtractExternalAdapter.class);

  private final ExtractExternalProperties properties;
  private final HttpClient httpClient;

  // directory for file-based cache (may be null if disabled)
  private final Path cacheDir;

  public OntologyExtractExternalAdapter(ExtractExternalProperties properties) {
    super(OntologyInfo.class);
    this.properties = properties;
    var builder = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
    if (properties.isFollowRedirects()) {
      builder.followRedirects(HttpClient.Redirect.NORMAL);
    }
    this.httpClient = builder.build();

    // initialize file cache directory if caching enabled
    Path dirPath = null;
    if (properties.isCacheEnabled() && properties.getCacheDir() != null && !properties.getCacheDir()
        .isBlank()) {
      Path tmp = Paths.get(properties.getCacheDir());
      try {
        Files.createDirectories(tmp);
        dirPath = tmp;
      } catch (Exception e) {
        logger.warn("Unable to create cache directory {}: {}", properties.getCacheDir(),
            e.getMessage());
      }
    }
    this.cacheDir = dirPath;
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    if (info == null || info.getModel() == null) {
      return info;
    }

    var externalReferences = info.getModel().listStatements().toList().stream()
        .filter(statement -> statement.getPredicate().equals(OWL2.imports))
        .map(statement -> statement.getObject().toString())
        .distinct()
        .toList();

    if (externalReferences.isEmpty()) {
      return info;
    }

    if (info.getExternalOntologies() == null) {
      info.setExternalOntologies(new HashMap<>());
    }

    for (var reference : externalReferences) {
      if (info.getExternalOntologies().containsKey(reference)) {
        continue;
      }

      Model model = null;

      // Check file cache (if configured and caching enabled)
      if (properties.isCacheEnabled() && cacheDir != null) {
        try {
          var file = cacheFileFor(reference);
          if (Files.exists(file)) {
            var lastModified = Files.getLastModifiedTime(file).toMillis();
            if (properties.getCacheTtlMs() <= 0
                || (System.currentTimeMillis() - lastModified) <= properties.getCacheTtlMs()) {
              var loaded = loadModelFromFile(file);
              if (loaded != null) {
                logger.debug("Loaded external ontology from file cache for {}", reference);
                model = loaded;
              }
            } else {
              logger.debug("File cache expired for {}", reference);
            }
          }
        } catch (Exception e) {
          logger.warn("Error reading file cache for {}: {}", reference, e.getMessage());
        }
      }

      if (model == null) {
        // Build list of URIs to try: original + mirrors (if any)
        List<String> toTry = new ArrayList<>();
        toTry.add(reference);

        // Lookup mirrors using normalized keys (exact, fragmentless, trailing-slashless)
        var mirrors = findMirrorsFor(reference);
        if (mirrors != null && !mirrors.isEmpty()) {
          toTry.addAll(mirrors);
        }

        for (var candidate : toTry) {
          model = fetchExternalOntology(candidate);
          if (model != null) {
            if (properties.isCacheEnabled() && cacheDir != null) {
              try {
                putInFileCache(reference, model);
              } catch (Exception e) {
                logger.warn("Failed to write file cache for {}: {}", reference, e.getMessage());
              }
            }
            var externalInfo = new OntologyInfo(Scope.EXTERNAL, info.getConfig(), null);
            externalInfo.setModel(model);
            info.getExternalOntologies().put(reference, externalInfo);
            break;
          } else {
            logger.debug("Failed to fetch candidate {} for original reference {}", candidate,
                reference);
          }
        }
      } else {
        var externalInfo = new OntologyInfo(Scope.EXTERNAL, info.getConfig(), null);
        externalInfo.setModel(model);
        info.getExternalOntologies().put(reference, externalInfo);
      }
    }
    return info;
  }

  private List<String> findMirrorsFor(String reference) {
    var list = properties.getMirrors();
    if (list == null || list.isEmpty()) return List.of();

    // 1) exact match
    for (var entry : list) {
      var uri = entry.getUri();
      if (uri == null || uri.isBlank()) continue;
      if (uri.equals(reference)) return entry.getResolvedMirrors();
    }

    // 2) fragmentless / trailing-slashless
    var refNoFrag = stripFragment(reference);
    for (var entry : list) {
      var uri = entry.getUri();
      if (uri == null) continue;
      if (stripFragment(uri).equals(refNoFrag)) return entry.getResolvedMirrors();
    }
    if (reference.endsWith("/")) {
      var refNoSlash = stripTrailingSlash(reference);
      for (var entry : list) {
        var uri = entry.getUri();
        if (uri == null) continue;
        if (stripTrailingSlash(uri).equals(refNoSlash)) return entry.getResolvedMirrors();
      }
    }

    // 3) prefix-based
    for (var entry : list) {
      var uri = entry.getUri();
      if (uri == null || uri.isBlank()) continue;
      if (reference.startsWith(uri)) return entry.getResolvedMirrors();
    }

    return List.of();
  }

  // Helper: strip fragment (#...) from URI
  private static String stripFragment(String uri) {
    var idx = uri.indexOf('#');
    return idx > 0 ? uri.substring(0, idx) : uri;
  }

  // Helper: strip trailing slash
  private static String stripTrailingSlash(String uri) {
    return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
  }

  private void putInFileCache(String reference, Model model) throws Exception {
    if (cacheDir == null) {
      return;
    }
    var file = cacheFileFor(reference);
    var tmp = cacheDir.resolve(file.getFileName().toString() + ".tmp");
    try {
      try (var out = Files.newOutputStream(tmp)) {
        model.write(out, properties.getCacheFormat());
      }
      try {
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException amnse) {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
      }
      try {
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
      } catch (Exception e) { /* ignore */ }
    } finally {
      try {
        if (Files.exists(tmp)) {
          Files.deleteIfExists(tmp);
        }
      } catch (Exception e) { /* ignore */ }
    }
  }

  private Model loadModelFromFile(Path file) {
    try (InputStream in = Files.newInputStream(file)) {
      var model = ModelFactory.createDefaultModel();
      try {
        model.read(in, null, properties.getCacheFormat());
        if (!model.isEmpty()) {
          return model;
        }
      } catch (Exception ex) {
        logger.debug("Failed to parse cached model {} with format {}: {}", file,
            properties.getCacheFormat(), ex.getMessage());
      }
    } catch (Exception e) {
      logger.warn("Failed to read cached model file {}: {}", file, e.getMessage());
    }
    return null;
  }

  private Path cacheFileFor(String reference) throws Exception {
    var hash = sha256Hex(reference);
    var ext = ".ttl";
    return cacheDir.resolve(hash + ext);
  }

  private static String sha256Hex(String input) throws Exception {
    var md = MessageDigest.getInstance("SHA-256");
    var digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var sb = new StringBuilder();
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private Model fetchExternalOntology(String reference) {
    logger.info("Fetching external ontology {}", reference);
    String lastError = null;
    int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
    final int maxRedirects = 5;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      String current = reference;
      int redirects = 0;
      try {
        while (true) {
          var request = HttpRequest.newBuilder()
              .uri(URI.create(current))
              .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
              .header("User-Agent", properties.getUserAgent())
              .GET()
              .build();

          HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
          int status = response.statusCode();

          if (status >= 200 && status < 300) {
            var body = response.body();
            var contentType = response.headers().firstValue("content-type");
            var model = parseModelFromBytes(reference, body, contentType.orElse(null));
            if (model != null) return model;
            lastError = String.format("Unable to parse external ontology content from %s (content-type=%s)", current, contentType.orElse("<none>"));
            break; // parsing failed, don't retry this candidate further
          }

          if (status >= 300 && status < 400) {
            if (redirects >= maxRedirects) {
              lastError = "Too many redirects";
              break;
            }
            var loc = response.headers().firstValue("location");
            if (loc.isPresent()) {
              try {
                current = URI.create(current).resolve(loc.get()).toString();
                redirects++;
                // debug log only for redirects
                logger.debug("Redirecting to {} ({} of {})", current, redirects, maxRedirects);
                continue; // follow redirect
              } catch (Exception ex) {
                lastError = "Invalid redirect location: " + ex.getMessage();
                break;
              }
            } else {
              lastError = "Redirect response with no Location header";
              break;
            }
          }

          // other non-success status -> don't retry
          lastError = "Non-success HTTP status " + status;
          break;
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        logger.warn("Interrupted while fetching {}", reference);
        return null;
      } catch (IOException ioe) {
        lastError = ioe.getMessage();
        logger.debug("Network error fetching {} (attempt {}): {}", reference, attempt, ioe.getMessage());
        // try next attempt if any
        if (attempt == maxAttempts) break;
        else continue;
      }
      // if we reach here without returning, parsing or redirects failed => don't retry further
      break;
    }

    logger.warn("Failed fetching external ontology {}: {}", reference, lastError == null ? "unknown error" : lastError);
    return null;
  }

  private Model parseModelFromBytes(String reference, byte[] body, String contentType) {
    // Prefer explicit content-type mapping
    String mime = contentType == null ? null : contentType.split(";")[0].trim().toLowerCase();
    String lang = mapMimeToLang(mime);
    if (lang != null) {
      var parsed = parseWithLang(body, lang);
      if (parsed != null) {
        return parsed;
      }
    }
    // If content-type is missing or unrecognized try filename
    switch (reference) {
      case String r when r.endsWith(".json") || r.endsWith(".jsonld") -> {
        lang = "JSON-LD";
      }
      case String r when r.endsWith(".rdf") || r.endsWith(".xml") -> {
        lang = "RDF/XML";
      }
      case String r when r.endsWith(".nt") || r.endsWith(".ntriples") -> {
        lang = "N-TRIPLES";
      }
      case String r when r.endsWith(".n3") -> {
        lang = "N3";
      }
      default -> {
        lang = "TURTLE";
      }
    }
    return parseWithLang(body, lang);
  }

  private String mapMimeToLang(String mime) {
    if (mime == null) {
      return null;
    }
    return switch (mime) {
      case "text/turtle", "application/x-turtle", "application/turtle", "text/ttl" -> "TURTLE";
      case "application/ld+json", "application/json" -> "JSON-LD";
      case "application/rdf+xml", "application/xml", "text/xml" -> "RDF/XML";
      case "application/n-triples", "application/ntriples", "text/plain" -> "N-TRIPLES";
      case "text/n3" -> "N3";
      default -> null;
    };
  }

  private Model parseWithLang(byte[] body, String lang) {
    try (var in = new ByteArrayInputStream(body)) {
      var model = ModelFactory.createDefaultModel();
      try {
        model.read(in, null, lang);
        if (!model.isEmpty()) {
          return model;
        }
      } catch (Exception ex) {
        logger.debug("Failed parsing as {}: {}", lang, ex.getMessage());
      }
    } catch (Exception ex) {
      // ignore
    }
    return null;
  }


  /*
   * Configuration for fetching external ontologies (HTTP client settings), cache and mirrors.
   * Properties prefix: adapters.ontology-extract-external
   * Example YAML:
   * adapters:
   *   ontology-extract-external:
   *     enabled: true
   *     connect-timeout-ms: 2000
   *     read-timeout-ms: 5000
   *     max-retries: 1
   *     follow-redirects: true
   *     user-agent: "oddtoolkit/1.0"
   *     cache-enabled: true
   *     cache-ttl-ms: 3600000
   *     cache-max-entries: 100
   *     cache-dir: "target/cache/ontology-extract-external"
   *     cache-format: "TURTLE"
   *     mirrors:
   *       "http://example.org/ontology":
   *         - "https://mirror1.example.org/ontology"
   *         - "https://mirror2.example.org/ontology"
   */
  @Setter
  @Getter
  @ConfigPrefix("adapters.ontology-extract-external")
  public static class ExtractExternalProperties {

    private boolean enabled = true;
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 5000;
    private int maxRetries = 1;
    private boolean followRedirects = true;
    private String userAgent = "oddtoolkit/1.0";

    private boolean cacheEnabled = true;
    private long cacheTtlMs = 3600000L; // 1 hour
    private int cacheMaxEntries = 100;

    // file-based cache directory (relative or absolute). If null/blank -> no file cache.
    private String cacheDir = "target/cache/ontology-extract-external";

    // format used when writing/reading cached models. Jena language names, e.g. TURTLE, RDF/XML
    private String cacheFormat = "TURTLE";

    // Mirrors are now configured as a list of entries for better YAML compatibility.
    private List<MirrorEntry> mirrors = new ArrayList<>();

    @Getter
    @Setter
    public static class MirrorEntry {
      // The URI or prefix this mirror applies to
      private String uri;
      // Primary field: a list of mirror locations
      private List<String> mirrors;
      // Backwards-compatible single mirror value
      private String mirror;

      public List<String> getResolvedMirrors() {
        if (mirrors != null && !mirrors.isEmpty()) return mirrors;
        if (mirror != null) return List.of(mirror);
        return List.of();
      }
    }

  }
}
