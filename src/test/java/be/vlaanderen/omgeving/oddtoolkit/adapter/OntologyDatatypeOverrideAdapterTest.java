package be.vlaanderen.omgeving.oddtoolkit.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.util.List;
import org.junit.jupiter.api.Test;

public class OntologyDatatypeOverrideAdapterTest {
  private static final String DATE_TIME_URI = "http://www.w3.org/2001/XMLSchema#dateTime";
  private static final String DATE_URI = "http://www.w3.org/2001/XMLSchema#date";
  private static final String STRING_URI = "http://www.w3.org/2001/XMLSchema#string";
  private static final String INTEGER_URI = "http://www.w3.org/2001/XMLSchema#integer";

  @Test
  void adaptReplacesConfiguredDatatypeUrisAcrossProperties() {
    OntologyConfiguration configuration = new OntologyConfiguration();
    configuration.setOverrideDatatypes(List.of(createOverride(DATE_TIME_URI, DATE_URI)));

    PropertyInfo createdProperty = createProperty("https://example.org/created", DATE_TIME_URI, STRING_URI);
    PropertyInfo identifierProperty = createProperty("https://example.org/id", INTEGER_URI);
    ClassInfo classInfo = createClass("https://example.org/Exploitant", createdProperty, identifierProperty);

    OntologyInfo ontologyInfo = new OntologyInfo(configuration);
    ontologyInfo.setClasses(List.of(classInfo));

    new OntologyDatatypeOverrideAdapter(configuration).adapt(ontologyInfo);

    assertEquals(List.of(DATE_URI, STRING_URI), createdProperty.getRange());
    assertEquals(List.of(INTEGER_URI), identifierProperty.getRange());
  }

  @Test
  void adaptIgnoresIncompleteOverrideEntries() {
    OntologyConfiguration configuration = new OntologyConfiguration();
    configuration.setOverrideDatatypes(List.of(
        createOverride("", DATE_URI),
        createOverride(DATE_TIME_URI, " ")));

    PropertyInfo createdProperty = createProperty("https://example.org/created", DATE_TIME_URI);
    ClassInfo classInfo = createClass("https://example.org/Exploitant", createdProperty);

    OntologyInfo ontologyInfo = new OntologyInfo(configuration);
    ontologyInfo.setClasses(List.of(classInfo));

    new OntologyDatatypeOverrideAdapter(configuration).adapt(ontologyInfo);

    assertEquals(List.of(DATE_TIME_URI), createdProperty.getRange());
  }

  private static OntologyConfiguration.OverrideDatatype createOverride(String uri, String override) {
    OntologyConfiguration.OverrideDatatype overrideDatatype = new OntologyConfiguration.OverrideDatatype();
    overrideDatatype.setUri(uri);
    overrideDatatype.setOverride(override);
    return overrideDatatype;
  }

  private static ClassInfo createClass(String uri, PropertyInfo... properties) {
    ClassInfo classInfo = new ClassInfo(Scope.ONTOLOGY, null);
    classInfo.setUri(uri);
    classInfo.setName("Exploitant");
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
