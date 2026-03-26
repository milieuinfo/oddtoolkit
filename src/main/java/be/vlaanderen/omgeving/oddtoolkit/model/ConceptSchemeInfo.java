package be.vlaanderen.omgeving.oddtoolkit.model;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Model;

@Getter
@Setter
public class ConceptSchemeInfo extends AbstractInfo {
  @Setter(AccessLevel.NONE)
  private final OntologyConfiguration config;
  private List<ClassConceptInfo> classConcepts;
  private List<PropertyConceptInfo> propertyConcepts;
  private Model model;

  public ConceptSchemeInfo(OntologyConfiguration config) {
    super(Scope.CONCEPTS, null);
    this.config = config;
  }
}
