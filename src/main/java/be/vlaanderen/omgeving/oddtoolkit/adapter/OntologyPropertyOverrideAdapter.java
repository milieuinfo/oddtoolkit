package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.List;

@AdapterDependency({
    OntologyPropertyExtractAdapter.class
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-property-override.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyPropertyOverrideAdapter extends AbstractAdapter<OntologyInfo> {
  private final OntologyConfiguration ontologyConfiguration;

  public OntologyPropertyOverrideAdapter(OntologyConfiguration ontologyConfiguration) {
    super(OntologyInfo.class);
    this.ontologyConfiguration = ontologyConfiguration;
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // Override properties based on configuration
    if (ontologyConfiguration.getOverrideProperties() != null) {
      ontologyConfiguration.getOverrideProperties().forEach((overrideProperty) -> {
        // Loop through the classes and their properties
        info.getClasses().forEach((classInfo) -> {
          classInfo.getProperties().forEach((propertyInfo) -> {
            // If the property matches the override property, apply the overrides
            if (propertyInfo.getUri().equals(overrideProperty.getUri())) {
              if (overrideProperty.getDatatype() != null) {
                propertyInfo.setRange(List.of(overrideProperty.getDatatype()));
              }
              if (overrideProperty.getCardinality() != null) {
                propertyInfo.setCardinalityTo(overrideProperty.getCardinality());
              }
            }
          });
        });
      });
    }
    return info;
  }
}
