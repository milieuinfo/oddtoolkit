package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AdapterDependency({
    OntologyPropertyExtractAdapter.class
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-datatype-override.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyDatatypeOverrideAdapter extends AbstractAdapter<OntologyInfo> {
  private final OntologyConfiguration ontologyConfiguration;

  public OntologyDatatypeOverrideAdapter(OntologyConfiguration ontologyConfiguration) {
    super(OntologyInfo.class);
    this.ontologyConfiguration = ontologyConfiguration;
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    Map<String, String> datatypeOverrides = mapDatatypeOverrides(
        ontologyConfiguration.getOverrideDatatypes());
    if (datatypeOverrides.isEmpty()) {
      return info;
    }

    info.getClasses().forEach(classInfo -> classInfo.getProperties().forEach(propertyInfo ->
        applyDatatypeOverrides(propertyInfo, datatypeOverrides)));
    return info;
  }

  private void applyDatatypeOverrides(PropertyInfo propertyInfo, Map<String, String> datatypeOverrides) {
    if (propertyInfo.getRange() == null || propertyInfo.getRange().isEmpty()) {
      return;
    }

    List<String> updatedRange = propertyInfo.getRange().stream()
        .map(rangeUri -> datatypeOverrides.getOrDefault(rangeUri, rangeUri))
        .toList();
    propertyInfo.setRange(updatedRange);
  }

  private Map<String, String> mapDatatypeOverrides(
      List<OntologyConfiguration.OverrideDatatype> overrideDatatypes) {
    Map<String, String> datatypeOverrides = new HashMap<>();
    if (overrideDatatypes == null || overrideDatatypes.isEmpty()) {
      return datatypeOverrides;
    }

    overrideDatatypes.forEach(override -> {
      if (!hasText(override.getUri()) || !hasText(override.getOverride())) {
        return;
      }
      datatypeOverrides.put(override.getUri().trim(), override.getOverride().trim());
    });
    return datatypeOverrides;
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
