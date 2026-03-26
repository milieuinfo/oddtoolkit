package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigPrefix("ontology")
public class OntologyConfiguration {
  // Path to the main ontology file
  private String ontologyFilePath;

  private String conceptsFilePath;

  // Configuration for ontology classes that should be treated as enumerations
  // YAML key: ontology.enum-classes
  private EnumClassesConfiguration enumClasses = new EnumClassesConfiguration();

  // Extra fixed properties you want to inject into generated classes
  // YAML key: ontology.extra-properties
  private List<ExtraProperty> extraProperties = new ArrayList<>();

  private List<OverrideProperty> overrideProperties = new ArrayList<>();

  private List<OverrideDatatype> overrideDatatypes = new ArrayList<>();

  private List<String> temporalProperties = new ArrayList<>();

  private MetadataClasses metadataClasses = new MetadataClasses();

  @Getter
  @Setter
  public static class MetadataClasses {
    private String suffix = "Metadata";
    private String key;
    private String value;
    private List<String> classes = new ArrayList<>();
  }

  @Getter
  @Setter
  public static class OverrideProperty {
    private String uri;
    private PropertyInfo.Cardinality cardinality;
    private String datatype;
  }

  @Getter
  @Setter
  public static class OverrideDatatype {
    private String uri;
    private String override;
  }

  @Getter
  @Setter
  public static class ExtraProperty {
    private String name;
    private String uri;
    private String comment;
    private String range;
    private boolean identifier = false;
    private PropertyInfo.Cardinality cardinality;
  }

  @Getter
  @Setter
  public static class EnumClassesConfiguration {
    // List of class URIs that should be treated as enumerations
    // YAML key: ontology.enum-classes.classes
    private List<String> classes = new ArrayList<>();

    // If true, enum class name tokens are removed from enum value names when they are a
    // redundant prefix or suffix (e.g. TRANSPORT_PROCEDURE -> TRANSPORT)
    // YAML key: ontology.enum-classes.trim-class-name-from-values
    private boolean trimClassNameFromValues = false;
  }
}
