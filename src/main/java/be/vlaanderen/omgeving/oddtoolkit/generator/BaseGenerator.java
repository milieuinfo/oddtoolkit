package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all generators. Subclasses must implement {@link #run()} with their
 * specific generation logic, and may override {@link #getName()}, {@link #getDescription()}, and
 * {@link #validate()}.
 */
public abstract class BaseGenerator {
  private static final Logger logger = LoggerFactory.getLogger(BaseGenerator.class);
  protected final OntologyInfo ontologyInfo;
  protected final ConceptSchemeInfo conceptSchemeInfo;
  protected final List<AbstractAdapter<?>> adapters;

  protected static final Comparator<ClassInfo> CLASS_INFO_ORDER = Comparator
      .comparing((ClassInfo c) -> c != null ? c.getUri() : null,
          Comparator.nullsLast(String::compareTo))
      .thenComparing(c -> c != null ? c.getName() : null,
          Comparator.nullsLast(String::compareTo));

  protected static final Comparator<ClassConceptInfo> CLASS_CONCEPT_ORDER = Comparator
      .comparing((ClassConceptInfo c) -> c != null ? c.getUri() : null,
          Comparator.nullsLast(String::compareTo))
      .thenComparing(c -> c != null ? c.getName() : null,
          Comparator.nullsLast(String::compareTo));

  public BaseGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters) {
    this.ontologyInfo = ontologyInfo;
    this.conceptSchemeInfo = conceptSchemeInfo;
    this.adapters = adapters != null ? adapters : List.of();
  }

  /**
   * Get the unique name of this generator.
   * Must be unique across all registered generators.
   *
   * @return generator name (e.g., "class-diagram", "sql", "typescript")
   */
  public String getName() {
    String simpleName = this.getClass().getSimpleName();
    String baseName = simpleName.endsWith("Generator")
        ? simpleName.substring(0, simpleName.length() - "Generator".length())
        : simpleName;
    return toKebabCase(baseName);
  }

  /**
   * Get a human-readable description of what this generator does.
   *
   * @return generator description
   */
  public String getDescription() {
    return "Generator: " + getName();
  }

  /**
   * Delegates to {@link #run()}. Subclasses typically override {@link #run()} directly.
   */
  public void generate() throws Exception {
    run();
  }

  /**
   * Run the generator. This is the main execution method.
   * Subclasses should override this to implement their generation logic.
   */
  @SuppressWarnings("unchecked")
  public void run() {
    for (AbstractAdapter<?> adapter : adapters) {
      logger.info("Running adapter: {}", adapter.getClass().getSimpleName());
      if (adapter.canAdapt(ontologyInfo)) {
        ((AbstractAdapter<OntologyInfo>) adapter).adapt(ontologyInfo);
      }
      if (adapter.canAdapt(conceptSchemeInfo)) {
        ((AbstractAdapter<ConceptSchemeInfo>) adapter).adapt(conceptSchemeInfo);
      }
    }
  }

  /**
   * Validate that the generator can execute with current configuration.
   * Override to add custom validation logic.
   *
   * @throws IllegalStateException if validation fails
   */
  public void validate() throws IllegalStateException {
    if (ontologyInfo == null) {
      throw new IllegalStateException("ontologyInfo must not be null");
    }
    if (conceptSchemeInfo == null) {
      throw new IllegalStateException("conceptSchemeInfo must not be null");
    }
  }

  /**
   * Get all classes defined in the ontology
   *
   * @return a list of ClassInfo objects representing the classes
   */
  public List<ClassInfo> getOntologyClasses() {
    return ontologyInfo.getClasses().stream()
        .filter(c -> c.getScope() == Scope.ONTOLOGY)
        .sorted(CLASS_INFO_ORDER)
        .toList();
  }

  /**
   * Get all classes
   *
   * @return a list of ClassInfo objects representing all classes
   */
  public List<ClassInfo> getAllClasses() {
    return ontologyInfo.getClasses().stream()
        .sorted(CLASS_INFO_ORDER)
        .toList();
  }

  /**
   * Get all concepts defined in the concept scheme
   *
   * @return a list of ConceptInfo objects representing the class concepts in the concept scheme
   */
  public List<ClassConceptInfo> getOntologyClassConcepts() {
    if (conceptSchemeInfo == null || conceptSchemeInfo.getClassConcepts() == null) {
      return List.of();
    }
    return conceptSchemeInfo.getClassConcepts().stream()
        .sorted(CLASS_CONCEPT_ORDER)
        .toList();
  }

  /**
   * Returns the {@link PropertyConceptInfo} whose equivalents contain the given property URI,
   * or {@code null} if none is found.
   */
   public PropertyConceptInfo getPropertyConceptForProperty(String propertyUri) {
     return conceptSchemeInfo.getPropertyConcepts().stream()
         .filter(pc -> pc.getEquivalents().contains(propertyUri))
         .findFirst()
         .orElse(null);
   }

   /**
    * Returns a concept info (class or property) whose equivalents contain the given URI,
    * or {@code null} if none is found.
    */
   public ConceptInfo getConceptForPropertyOrClass(String uri) {
     PropertyConceptInfo pci = getPropertyConceptForProperty(uri);
     if (pci != null) {
       return pci;
     }
     return conceptSchemeInfo.getClassConcepts().stream()
         .filter(cc -> cc.getEquivalents().contains(uri))
         .findFirst()
         .orElse(null);
   }

  /**
   * Returns the {@link ClassConceptInfo} whose equivalents contain the given class URI,
   * or {@code null} if none is found.
   */
  public ClassConceptInfo getClassConceptForClass(String classUri) {
    return conceptSchemeInfo.getClassConcepts().stream()
        .filter(cc -> cc.getEquivalents().contains(classUri))
        .findFirst()
        .orElse(null);
  }

  /**
   * Convert a string to snake_case.
   *
   * @param input the string to convert
   * @return the string in snake_case
   */
  protected static String toSnakeCase(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    return input
        .replaceAll("([a-z])([A-Z]+)", "$1_$2")
        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
        .toLowerCase();
  }

  protected static String toKebabCase(String input) {
    String snakeCase = toSnakeCase(input);
    return snakeCase != null ? snakeCase.replace('_', '-') : null;
  }

  protected void saveToFile(String outputFile, String content) {
    if (outputFile == null) {
      return;
    }
    try {
      Path parentDir = Paths.get(outputFile).getParent();
      if (parentDir != null) {
        Files.createDirectories(parentDir);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to create directories for output file: " + outputFile, e);
    }
    try (FileWriter writer = new FileWriter(outputFile)) {
      writer.write(content);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write diagram to file: " + outputFile, e);
    }
  }
}
