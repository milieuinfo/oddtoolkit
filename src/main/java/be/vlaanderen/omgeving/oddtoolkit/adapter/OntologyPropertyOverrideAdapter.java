package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
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
    if (ontologyConfiguration.getOverrideProperties() == null) {
      return info;
    }

    ontologyConfiguration.getOverrideProperties().forEach(overrideProperty -> {
      String overrideUri = normalize(overrideProperty == null ? null : overrideProperty.getUri());
      if (overrideUri == null) {
        return;
      }

      String range = resolveRange(overrideProperty);
      String name = normalize(overrideProperty.getName());
      String comment = normalize(overrideProperty.getComment());
      info.getClasses().forEach(classInfo -> classInfo.getProperties().forEach(propertyInfo -> {
        if (!overrideUri.equals(propertyInfo.getUri())) {
          return;
        }
        if (range != null) {
          propertyInfo.setRange(List.of(range));
        }
        if (name != null) {
          propertyInfo.setName(name);
        }
        if (overrideProperty.getCardinality() != null) {
          propertyInfo.setCardinalityTo(overrideProperty.getCardinality());
        }
        if (overrideProperty.getIdentifier() != null) {
          propertyInfo.setIdentifier(overrideProperty.getIdentifier());
        }
        if (comment != null) {
          propertyInfo.setComment(comment);
        }
      }));
    });

    return info;
  }

  private static String resolveRange(OntologyConfiguration.OverrideProperty overrideProperty) {
    String range = normalize(overrideProperty.getRange());
    return range != null ? range : normalize(overrideProperty.getDatatype());
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
