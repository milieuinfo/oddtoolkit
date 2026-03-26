package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyReasonerProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.security.MessageDigest;

@AdapterDependency({
    OntologyExtractExternalAdapter.class
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-reasoner.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyReasonerAdapter extends AbstractAdapter<OntologyInfo> {

  private static final Logger logger = LoggerFactory.getLogger(OntologyReasonerAdapter.class);
  private final OntologyConfiguration ontologyConfiguration;
  private Reasoner reasoner;
  private final OntologyReasonerProperties reasonerProperties;
  private final Path inferredCacheDir;

  public OntologyReasonerAdapter(OntologyReasonerProperties reasonerProperties,
      OntologyConfiguration ontologyConfiguration) {
    super(OntologyInfo.class, false);
    this.reasonerProperties = reasonerProperties;
    initialize();
    // prepare inferred cache dir if enabled
    java.nio.file.Path dir = null;
    var useCache = reasonerProperties.isInferredCacheEnabled();
    var cacheDirStr = reasonerProperties.getInferredCacheDir();
    if (useCache && cacheDirStr != null && !cacheDirStr.isBlank()) {
      try {
        dir = java.nio.file.Paths.get(cacheDirStr);
        java.nio.file.Files.createDirectories(dir);
      } catch (Exception e) {
        logger.warn("Unable to create inferred cache directory {}: {}", cacheDirStr, e.getMessage());
        dir = null;
      }
    }
    this.inferredCacheDir = dir;
    this.ontologyConfiguration = ontologyConfiguration;
  }

  @Override
  protected void initialize() {
    // If a rule file is configured, create a GenericRuleReasoner from it.
    String rulesFile = reasonerProperties.getRulesFile();
    if (rulesFile != null && !rulesFile.isBlank()) {
      try {
        Path p = Paths.get(rulesFile);
        List<Rule> rules = Rule.rulesFromURL(p.toUri().toString());
        GenericRuleReasoner grr = new GenericRuleReasoner(rules);
        grr.setDerivationLogging(false);
        this.reasoner = grr;
        logger.info("Using custom rules reasoner from {}", rulesFile);
        return;
      } catch (Exception e) {
        logger.warn("Failed to load rules from {}: {}. Falling back to configured reasoner.", rulesFile, e.getMessage());
      }
    }

    // Choose configured reasoner type to improve performance where possible
    String type = reasonerProperties.getReasonerType();
    if (type != null && type.equalsIgnoreCase("rdfs")) {
      this.reasoner = ReasonerRegistry.getRDFSReasoner();
      logger.info("Using RDFS reasoner (configured) for faster, lighter-weight reasoning");
    } else if (type != null && type.equalsIgnoreCase("owl")) {
      this.reasoner = ReasonerRegistry.getOWLMicroReasoner();
      logger.info("Using OWL reasoner (configured) for more complete OWL reasoning");
    } else if (type != null && type.equalsIgnoreCase("transitive")) {
      this.reasoner = ReasonerRegistry.getTransitiveReasoner();
      logger.info("Using transitive rule reasoner (configured) without rules");
    } else {
      this.reasoner = ReasonerRegistry.getOWLMicroReasoner();
      logger.info("Using OWL reasoner (configured) for OWL reasoning");
    }
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // Build a union model view (base + externals) without copying triples to reduce memory and time
    Model base = info.getModel();
    Model union = (base != null) ? base : ModelFactory.createDefaultModel();

    if (info.getExternalOntologies() != null) {
      for (OntologyInfo external : info.getExternalOntologies().values()) {
        Model em = external.getModel();
        if (em == null) continue;
        union = ModelFactory.createUnion(union, em);
      }
    }

    // If caching enabled, attempt to load cached inferred model to avoid long reasoning
    InfModel inf = null;
    boolean loadedFromCache = false;
    try {
      var useCache = reasonerProperties.isInferredCacheEnabled();
      var ttl = reasonerProperties.getInferredCacheTtlMs();
      var fmt = reasonerProperties.getInferredCacheFormat();
      if (useCache && inferredCacheDir != null) {
        String cacheKey = computeCacheKey(ontologyConfiguration.getOntologyFilePath(), info);
        var cacheFile = cacheFileFor(cacheKey);
        if (cacheFile != null && java.nio.file.Files.exists(cacheFile)) {
          long lastModified = java.nio.file.Files.getLastModifiedTime(cacheFile).toMillis();
          if (ttl <= 0 || (System.currentTimeMillis() - lastModified) <= ttl) {
            var loaded = loadModelFromFile(cacheFile, fmt);
            if (loaded != null) {
              logger.info("Loaded inferred model from cache {}", cacheFile);
              inf = ModelFactory.createInfModel(reasoner, loaded);
              loadedFromCache = true;
            }
          } else {
            logger.debug("Inferred cache expired for {}", cacheFile);
          }
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to load inferred cache: {}", e.getMessage());
    }

    if (inf == null) {
      // Create the InfModel once over the union model. This avoids copying data and speeds up reasoning.
      inf = ModelFactory.createInfModel(reasoner, union);
      // Only materialize if configured (prepare() is expensive)
      var materialize = reasonerProperties.isReasonerMaterialize();
      if (materialize) {
        logger.info("Preparing inferred model (this may take some time for large ontologies)...");
        inf.prepare();
      } else {
        logger.info("Skipping infModel.prepare() (reasonerMaterialize=false) — inference will be lazy");
      }

      // Only compute and log sizes when debug enabled to avoid expensive operations.
      if (logger.isDebugEnabled()) {
        try {
          long s = inf.size();
          logger.debug("Inferred model has {} statements", s);
        } catch (Exception ex) {
          logger.debug("Computing inferred model size failed: {}", ex.getMessage());
        }
      }

      // After reasoning, store inferred model to cache if enabled
      var useCache = reasonerProperties.isInferredCacheEnabled();
      var fmt = reasonerProperties.getInferredCacheFormat();
      if (useCache && inferredCacheDir != null) {
        try {
          String cacheKey = computeCacheKey(ontologyConfiguration.getOntologyFilePath(), info);
          var cacheFile = cacheFileFor(cacheKey);
          if (cacheFile != null) {
            writeModelToFile(inf, cacheFile, fmt);
            logger.info("Wrote inferred model cache to {}", cacheFile);
          }
        } catch (Exception e) {
          logger.warn("Failed to write inferred model cache: {}", e.getMessage());
        }
      }
    }

    info.setInferredModel(inf);

    // Optionally write inferred model to TTL file
    try {
      var outEnabled = reasonerProperties.isInferredOutputEnabled();
      var outPath = reasonerProperties.getInferredOutputPath();
      if (outEnabled && outPath != null && !outPath.isBlank()) {
        writeInferredModel(info.getInferredModel(), outPath);
      }
    } catch (Exception e) {
      logger.warn("Failed to write inferred model to {}: {}", reasonerProperties.getInferredOutputPath(), e.getMessage());
    }

    return info;
  }

  private void writeInferredModel(Model model, String outputPath) throws Exception {
    logger.info("Writing inferred model to {}", outputPath);
    Path output = Paths.get(outputPath);
    Path dir = output.getParent();
    if (dir != null) Files.createDirectories(dir);
    Path tmp = output.resolveSibling(output.getFileName().toString() + ".tmp");
    try {
      try (var out = Files.newOutputStream(tmp)) {
        model.write(out, "TURTLE");
      }
      try {
        Files.move(tmp, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException amnse) {
        Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      if (Files.exists(tmp)) Files.deleteIfExists(tmp);
    }
  }

  // --- inferred cache helpers ---
  private String computeCacheKey(String ontologyFilePath, OntologyInfo info) throws Exception {
    StringBuilder sb = new StringBuilder();
    if (ontologyFilePath != null) sb.append(ontologyFilePath).append('|');
    // include external references to make cache key sensitive to imports
    if (info.getExternalOntologies() != null && !info.getExternalOntologies().isEmpty()) {
      var refs = new java.util.ArrayList<>(info.getExternalOntologies().keySet());
      refs.sort(String::compareTo);
      for (String r : refs) {
        sb.append(r).append('|');
      }
    }
    return sha256Hex(sb.toString());
  }

  private Path cacheFileFor(String cacheKey) {
    if (inferredCacheDir == null) return null;
    String ext = ".ttl";
    return inferredCacheDir.resolve(cacheKey + ext);
  }

  private Model loadModelFromFile(Path file, String format) {
    try (InputStream in = Files.newInputStream(file)) {
      var model = ModelFactory.createDefaultModel();
      try {
        model.read(in, null, format);
        if (!model.isEmpty()) return model;
      } catch (Exception ex) {
        logger.debug("Failed to parse cached inferred model {} with format {}: {}", file, format, ex.getMessage());
      }
    } catch (Exception e) {
      logger.warn("Failed to read cached inferred model file {}: {}", file, e.getMessage());
    }
    return null;
  }

  private void writeModelToFile(Model model, Path file, String format) throws Exception {
    var tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
    try {
      try (var out = Files.newOutputStream(tmp)) {
        model.write(out, format);
      }
      try {
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException amnse) {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      try { if (Files.exists(tmp)) Files.deleteIfExists(tmp); } catch (Exception ignore) {}
    }
  }

  private static String sha256Hex(String input) throws Exception {
    var md = MessageDigest.getInstance("SHA-256");
    var digest = md.digest((input == null ? "" : input).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var sb = new StringBuilder();
    for (byte b : digest) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}
