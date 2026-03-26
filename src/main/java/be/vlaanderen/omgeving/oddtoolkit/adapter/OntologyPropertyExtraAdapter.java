package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.ConditionalOnConfigProperty;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo.Cardinality;
import java.util.List;

@AdapterDependency({
    OntologyClassExtractAdapter.class,
})
@ConditionalOnConfigProperty(prefix = "adapters", name = "ontology-property-extra.enabled", havingValue = "true", matchIfMissing = true)
public class OntologyPropertyExtraAdapter extends AbstractAdapter<OntologyInfo> {

  private final OntologyConfiguration ontologyConfiguration;

  public OntologyPropertyExtraAdapter(OntologyConfiguration ontologyConfiguration) {
    super(OntologyInfo.class);
    this.ontologyConfiguration = ontologyConfiguration;
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    ensureExtraProperties(info);
    return info;
  }


  private void ensureExtraProperties(OntologyInfo info) {
    // Ensure that the extra properties are included in the concrete classes and interfaces
    for (OntologyConfiguration.ExtraProperty extraProperty : ontologyConfiguration.getExtraProperties()) {
      if (extraProperty == null || extraProperty.getUri() == null) {
        continue;
      }
      // Add to all ontology classes (preserve scope for each class)
      for (ClassInfo c : info.getClasses()) {
        boolean exists = c.getProperties().stream()
            .anyMatch(p -> extraProperty.getUri().equals(p.getUri()));
        if (exists) {
          continue;
        }
        PropertyInfo propertyInfo = getPropertyInfo(extraProperty,
            c);
        c.getProperties().add(propertyInfo);
      }
    }
  }

  private static PropertyInfo getPropertyInfo(OntologyConfiguration.ExtraProperty extraProperty, ClassInfo c) {
    PropertyInfo propertyInfo = new PropertyInfo(c.getScope(), null);
    propertyInfo.setUri(extraProperty.getUri());
    propertyInfo.setName(extraProperty.getName());
    propertyInfo.setLabel(extraProperty.getName());
    if (extraProperty.getRange() != null && !extraProperty.getRange().isEmpty()) {
      propertyInfo.setRange(List.of(extraProperty.getRange()));
    }
    propertyInfo.setCardinalityTo(extraProperty.getCardinality() == null ? new Cardinality()
        : extraProperty.getCardinality());
    propertyInfo.setComment(extraProperty.getComment());
    propertyInfo.setIdentifier(extraProperty.isIdentifier());
    return propertyInfo;
  }
}
