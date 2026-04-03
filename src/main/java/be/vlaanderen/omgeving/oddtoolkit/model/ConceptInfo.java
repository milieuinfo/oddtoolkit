package be.vlaanderen.omgeving.oddtoolkit.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.rdf.model.Resource;

@Getter
@Setter
public abstract class ConceptInfo extends AbstractInfo {
  private List<String> equivalents;

  public ConceptInfo(Scope scope, Resource resource) {
    super(scope, resource);
  }

  @Override
  protected void initializeFromResource(Resource resource) {
    super.initializeFromResource(resource);
    this.equivalents = new ArrayList<>();
  }
}
