package be.vlaanderen.omgeving.oddtoolkit.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.List;
import org.junit.jupiter.api.Test;

class OntologyPropertyOverrideAdapterTest {

  @Test
  void adaptAppliesRangeIdentifierAndCardinality() {
    OntologyConfiguration.OverrideProperty overrideProperty = new OntologyConfiguration.OverrideProperty();
    overrideProperty.setUri("https://example.org/id");
    overrideProperty.setName("uuid");
    overrideProperty.setComment("Primary identifier");
    overrideProperty.setRange("http://www.w3.org/2001/XMLSchema#string");
    overrideProperty.setIdentifier(true);

    PropertyInfo.Cardinality cardinality = new PropertyInfo.Cardinality();
    cardinality.setMin(1);
    cardinality.setMax(1);
    overrideProperty.setCardinality(cardinality);

    OntologyConfiguration configuration = new OntologyConfiguration();
    configuration.setOverrideProperties(List.of(overrideProperty));

    PropertyInfo property = createProperty("https://example.org/id", "http://www.w3.org/2001/XMLSchema#integer");
    property.setIdentifier(false);
    ClassInfo classInfo = createClass("https://example.org/Thing", property);

    OntologyInfo ontologyInfo = new OntologyInfo(configuration);
    ontologyInfo.setClasses(List.of(classInfo));

    new OntologyPropertyOverrideAdapter(configuration).adapt(ontologyInfo);

    assertEquals("uuid", property.getName());
    assertEquals(List.of("http://www.w3.org/2001/XMLSchema#string"), property.getRange());
    assertTrue(property.isIdentifier());
    assertEquals("Primary identifier", property.getComment());
    assertEquals(1, property.getCardinalityTo().getMin());
    assertEquals(1, property.getCardinalityTo().getMax());
  }

  @Test
  void adaptUsesRangeBeforeLegacyDatatype() {
    OntologyConfiguration.OverrideProperty overrideProperty = new OntologyConfiguration.OverrideProperty();
    overrideProperty.setUri("https://example.org/id");
    overrideProperty.setRange("http://www.w3.org/2001/XMLSchema#string");
    overrideProperty.setDatatype("http://www.w3.org/2001/XMLSchema#integer");

    OntologyConfiguration configuration = new OntologyConfiguration();
    configuration.setOverrideProperties(List.of(overrideProperty));

    PropertyInfo property = createProperty("https://example.org/id", "http://www.w3.org/2001/XMLSchema#decimal");
    ClassInfo classInfo = createClass("https://example.org/Thing", property);

    OntologyInfo ontologyInfo = new OntologyInfo(configuration);
    ontologyInfo.setClasses(List.of(classInfo));

    new OntologyPropertyOverrideAdapter(configuration).adapt(ontologyInfo);

    assertEquals(List.of("http://www.w3.org/2001/XMLSchema#string"), property.getRange());
  }

  @Test
  void adaptFallsBackToLegacyDatatypeWhenRangeMissing() {
    OntologyConfiguration.OverrideProperty overrideProperty = new OntologyConfiguration.OverrideProperty();
    overrideProperty.setUri("https://example.org/id");
    overrideProperty.setDatatype("http://www.w3.org/2001/XMLSchema#string");

    OntologyConfiguration configuration = new OntologyConfiguration();
    configuration.setOverrideProperties(List.of(overrideProperty));

    PropertyInfo property = createProperty("https://example.org/id", "http://www.w3.org/2001/XMLSchema#decimal");
    ClassInfo classInfo = createClass("https://example.org/Thing", property);

    OntologyInfo ontologyInfo = new OntologyInfo(configuration);
    ontologyInfo.setClasses(List.of(classInfo));

    new OntologyPropertyOverrideAdapter(configuration).adapt(ontologyInfo);

    assertEquals(List.of("http://www.w3.org/2001/XMLSchema#string"), property.getRange());
  }

  @Test
  void adaptIgnoresBlankOverrideValues() {
    OntologyConfiguration.OverrideProperty overrideProperty = new OntologyConfiguration.OverrideProperty();
    overrideProperty.setUri("https://example.org/id");
    overrideProperty.setName("  ");
    overrideProperty.setRange("   ");
    overrideProperty.setDatatype(" ");

    OntologyConfiguration configuration = new OntologyConfiguration();
    configuration.setOverrideProperties(List.of(overrideProperty));

    PropertyInfo property = createProperty("https://example.org/id", "http://www.w3.org/2001/XMLSchema#decimal");
    property.setIdentifier(false);
    property.setName("property");
    ClassInfo classInfo = createClass("https://example.org/Thing", property);

    OntologyInfo ontologyInfo = new OntologyInfo(configuration);
    ontologyInfo.setClasses(List.of(classInfo));

    new OntologyPropertyOverrideAdapter(configuration).adapt(ontologyInfo);

    assertEquals("property", property.getName());
    assertEquals(List.of("http://www.w3.org/2001/XMLSchema#decimal"), property.getRange());
    assertFalse(property.isIdentifier());
  }

  private static ClassInfo createClass(String uri, PropertyInfo... properties) {
    ClassInfo classInfo = new ClassInfo(Scope.ONTOLOGY, null);
    classInfo.setUri(uri);
    classInfo.setName("Thing");
    classInfo.setProperties(List.of(properties));
    return classInfo;
  }

  private static PropertyInfo createProperty(String uri, String... range) {
    PropertyInfo propertyInfo = new PropertyInfo(Scope.ONTOLOGY, null);
    propertyInfo.setUri(uri);
    propertyInfo.setName("property");
    propertyInfo.setRange(List.of(range));
    return propertyInfo;
  }
}
