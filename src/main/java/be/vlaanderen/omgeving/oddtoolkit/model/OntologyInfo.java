package be.vlaanderen.omgeving.oddtoolkit.model;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;

@Getter
@Setter
public class OntologyInfo extends AbstractInfo {
  @Setter(AccessLevel.NONE)
  private final OntologyConfiguration config;
  private final ConceptSchemeInfo concepts;
  private final Map<String, ClassInfo> classesByUri = new LinkedHashMap<>();

  private Model model;
  private InfModel inferredModel;
  private Map<String, OntologyInfo> externalOntologies;

  public OntologyInfo(OntologyConfiguration config) {
    super(Scope.ONTOLOGY, null);
    this.config = config;
    this.concepts = new ConceptSchemeInfo(config);
  }

  // Constructor for creating external ontology instances (non-Spring managed)
  public OntologyInfo(Scope scope, OntologyConfiguration config, ConceptSchemeInfo concepts) {
    super(scope, null);
    this.config = config;
    this.concepts = concepts;
  }

  /**
   * Return a mutable list snapshot of classes (in insertion order). Modifying this list does not
   * affect the internal storage; use addClass() to add while ensuring uniqueness.
   */
  public List<ClassInfo> getClasses() {
    return new ArrayList<>(classesByUri.values());
  }

  /**
   * Replace the classes collection. Duplicates (by URI) will be ignored and the last occurrence wins.
   */
  public void setClasses(List<ClassInfo> classes) {
    classesByUri.clear();
    if (classes == null) return;
    for (ClassInfo ci : classes) {
      if (ci == null || ci.getUri() == null) continue;
      classesByUri.put(ci.getUri(), ci);
    }
  }

  /**
   * Add a class if its URI is not already present. Returns true if added, false if a duplicate.
   */
  public void addClass(ClassInfo classInfo) {
    if (classInfo == null || classInfo.getUri() == null) return;
    if (classesByUri.containsKey(classInfo.getUri())) return;
    classesByUri.put(classInfo.getUri(), classInfo);
  }

  public ClassInfo getClassByUri(String uri) {
    return classesByUri.get(uri);
  }

}
