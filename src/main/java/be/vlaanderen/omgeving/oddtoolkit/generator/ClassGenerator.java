package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.model.AbstractInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Cardinality;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ClassInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyConceptInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.Scope;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class ClassGenerator extends BaseGenerator {

  private static final Logger logger = LoggerFactory.getLogger(ClassGenerator.class);

  protected List<Clazz> classes = new ArrayList<>();
  protected List<Interface> interfaces = new ArrayList<>();
  protected List<Enum> enums = new ArrayList<>();

  private static final Comparator<Clazz> CLAZZ_ORDER = Comparator
      .comparing((Clazz c) -> c != null ? c.getUri() : null, Comparator.nullsLast(String::compareTo))
      .thenComparing(c -> c != null ? c.getName() : null, Comparator.nullsLast(String::compareTo));

  private static final Comparator<EnumValue> ENUM_VALUE_ORDER = Comparator
      .comparing((EnumValue v) -> v != null ? v.getUri() : null, Comparator.nullsLast(String::compareTo))
      .thenComparing(v -> v != null ? v.getName() : null, Comparator.nullsLast(String::compareTo));

  public ClassGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo, List<AbstractAdapter<?>> adapters) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
  }

  /**
   * Returns the name and label for a class. If a {@link ClassConceptInfo} is mapped to this class,
   * its name/label takes precedence over the ontology-level values.
   */
  protected Pair<String, String> getClassNameAndLabel(ClassInfo classInfo) {
    String name = classInfo.getName();
    String label = classInfo.getLabel() != null ? classInfo.getLabel() : name;
    ClassConceptInfo cci = getClassConceptForClass(classInfo.getUri());
    return getStringPair(name, label, cci);
  }

  protected Pair<String, String> getPropertyNameAndLabel(PropertyInfo propertyInfo) {
    String name = propertyInfo.getName();
    String label = propertyInfo.getLabel() != null ? propertyInfo.getLabel() : name;
    PropertyConceptInfo pci = getPropertyConceptForProperty(propertyInfo.getUri());
    return getStringPair(name, label, pci);
  }

  private Pair<String, String> getStringPair(String name, String label, ConceptInfo ci) {
    if (ci != null) {
      if (ci.getName() != null) {
        name = ci.getName();
      }
      if (ci.getLabel() != null) {
        label = ci.getLabel();
      }
    }
    return new Pair<>(name, label);
  }

  public List<Clazz> getClasses() {
    return classes;
  }

  /**
   * Returns all classes including interfaces (for backward compatibility with callers that need the combined list).
   */
  public List<Clazz> getAllGeneratedClasses() {
    List<Clazz> allClasses = new ArrayList<>();
    allClasses.addAll(classes);
    allClasses.addAll(interfaces);
    allClasses.addAll(enums);
    return allClasses;
  }

  protected OntologyInfo getOntologyInfo() {
    return ontologyInfo;
  }

  protected be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration getOntologyConfiguration() {
    return ontologyInfo.getConfig();
  }

  @Override
  public void run() {
    super.run();
    // Reset internal state so repeated invocations produce a clean result.
    classes = new ArrayList<>();
    interfaces = new ArrayList<>();
    enums = new ArrayList<>();
    extractClasses();
    extractInterfaces();
    extractEnums();
    extractMetadataClasses();
    extractRelations();
    applyFilters();
    applySurrogateKeys();
    applyIdentifierFallbacks();
    updateRanges();
    extractDataTypes();
    stabilizeOrdering();
  }

  /**
   * When {@code ontology.surrogate-keys.enabled} is set, replaces composite primary keys
   * (classes with more than one identifier attribute) with a single generated surrogate key,
   * demoting the original identifier attributes to regular, non-key attributes.
   */
  private void applySurrogateKeys() {
    be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration.SurrogateKeys config =
        getOntologyConfiguration().getSurrogateKeys();
    if (config == null || !config.isEnabled()) {
      return;
    }
    List<Clazz> completeList = new ArrayList<>();
    completeList.addAll(classes);
    completeList.addAll(interfaces);
    completeList.forEach(clazz -> applySurrogateKey(clazz, config));
  }

  private void applySurrogateKey(Clazz clazz,
      be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration.SurrogateKeys config) {
    List<Attribute> primaryKeys = clazz.getAttributes().stream()
        .filter(Attribute::isPrimaryKey)
        .toList();
    if (primaryKeys.size() <= 1) {
      return;
    }
    primaryKeys.forEach(attribute -> attribute.setPrimaryKey(false));

    String namespace = clazz.getClassInfo().getResource().getNameSpace();
    PropertyInfo surrogatePropertyInfo = new PropertyInfo(clazz.getClassInfo().getScope(),
        getOntologyInfo().getModel().createResource(namespace + config.getName()));
    surrogatePropertyInfo.setName(config.getName());
    surrogatePropertyInfo.setIdentifier(true);
    surrogatePropertyInfo.setCardinalityTo(new PropertyInfo.Cardinality());
    surrogatePropertyInfo.getCardinalityTo().setMin(1);
    surrogatePropertyInfo.getCardinalityTo().setMax(1);
    surrogatePropertyInfo.setCardinalityFrom(new PropertyInfo.Cardinality());

    Attribute surrogateAttribute = new Attribute();
    surrogateAttribute.setName(config.getName());
    surrogateAttribute.setPropertyInfo(surrogatePropertyInfo);
    surrogateAttribute.setCardinality(Cardinality.ONE_TO_ONE);
    surrogateAttribute.setDomain(clazz);
    surrogateAttribute.setPrimaryKey(true);
    surrogateAttribute.setNullable(false);
    surrogateAttribute.setDataType(new DataType(null, config.getDatatype()));

    List<Attribute> updatedAttributes = new ArrayList<>();
    updatedAttributes.add(surrogateAttribute);
    updatedAttributes.addAll(clazz.getAttributes());
    clazz.setAttributes(updatedAttributes);
  }

  /**
   * A class only gets an identifier attribute via a {@code hydra:search} template, or via
   * {@code ontology.extra-properties}/{@code ontology.override-properties} configuration. When
   * none of those apply, every attribute ends up with {@code primaryKey=false} and the generated
   * table would silently end up without a {@code PRIMARY KEY}. Falls back to the class's "uri"
   * attribute (present on every class via the conventional {@code uri} extra property) and logs
   * a warning so the gap stays visible instead of failing silently downstream (e.g. missing
   * PRIMARY KEY clauses, or foreign keys that can't resolve to this table at all).
   */
  private void applyIdentifierFallbacks() {
    List<Clazz> completeList = new ArrayList<>();
    completeList.addAll(classes);
    completeList.addAll(interfaces);
    completeList.forEach(this::applyIdentifierFallback);
  }

  private void applyIdentifierFallback(Clazz clazz) {
    boolean hasPrimaryKey = clazz.getAttributes().stream().anyMatch(Attribute::isPrimaryKey);
    if (hasPrimaryKey) {
      return;
    }
    Attribute uriAttribute = clazz.getAttributes().stream()
        .filter(attribute -> "uri".equalsIgnoreCase(attribute.getName()))
        .findFirst()
        .orElse(null);
    if (uriAttribute == null) {
      logger.warn("Class '{}' ({}) has no identifier property and no 'uri' attribute to fall "
              + "back to; the generated table will have no primary key. Configure a "
              + "hydra:search template or an identifier in "
              + "ontology.extra-properties/override-properties.",
          clazz.getName(), clazz.getUri());
      return;
    }
    uriAttribute.setPrimaryKey(true);
    uriAttribute.setNullable(false);
    logger.warn("Class '{}' ({}) has no identifier property; falling back to '{}' as the "
            + "primary key. Configure a hydra:search template or an identifier in "
            + "ontology.extra-properties/override-properties to use a proper identifier instead.",
        clazz.getName(), clazz.getUri(), uriAttribute.getName());
  }

  private void stabilizeOrdering() {
    classes = classes.stream().sorted(CLAZZ_ORDER).toList();
    interfaces = interfaces.stream().sorted(CLAZZ_ORDER).toList();
    enums = enums.stream().sorted(CLAZZ_ORDER).toList();

    classes.forEach(this::sortClazzInternals);
    interfaces.forEach(this::sortClazzInternals);
    enums.forEach(this::sortClazzInternals);

    enums.forEach(enumClazz -> enumClazz.setValues(enumClazz.getValues().stream()
        .sorted(ENUM_VALUE_ORDER)
        .toList()));
  }

  private void sortClazzInternals(Clazz clazz) {
    clazz.setAttributes(clazz.getAttributes().stream()
        .sorted(attributeOrder(clazz))
        .toList());
    clazz.setInterfaces(clazz.getInterfaces().stream()
        .sorted(CLAZZ_ORDER)
        .toList());
  }

  /**
   * Build a comparator for the attributes of the given class.
   * Bucket 0 – primary keys, ordered by their position in the class's Hydra URI-template
   *           (keys that don't appear in the template sort after those that do).
   * Bucket 1 – extra-properties (in config list order).
   * Bucket 2 – temporal properties (in config list order).
   * Bucket 3 – everything else (stable URI / name order).
   */
  private Comparator<Attribute> attributeOrder(Clazz clazz) {
    Map<String, Integer> extraOrder = new HashMap<>();
    List<be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration.ExtraProperty> extras =
        getOntologyConfiguration().getExtraProperties();
    for (int i = 0; i < extras.size(); i++) {
      String uri = extras.get(i).getUri();
      if (uri != null) {
        extraOrder.putIfAbsent(uri, i);
      }
    }

    Map<String, Integer> temporalOrder = new HashMap<>();
    List<String> temporals = getOntologyConfiguration().getTemporalProperties();
    for (int i = 0; i < temporals.size(); i++) {
      String uri = temporals.get(i);
      if (uri != null) {
        temporalOrder.putIfAbsent(uri, i);
      }
    }

    // Build a PK ordering map from the URI template of this class, when available.
    Map<String, Integer> pkTemplateOrder = buildPkTemplateOrder(clazz);

    return Comparator
        .comparingInt((Attribute attribute) -> attributeBucket(attribute, extraOrder, temporalOrder))
        .thenComparingInt(attribute -> pkTemplateIndex(attribute, pkTemplateOrder))
        .thenComparingInt(attribute -> orderIndex(attribute, extraOrder))
        .thenComparingInt(attribute -> orderIndex(attribute, temporalOrder))
        .thenComparing(attribute -> attribute != null ? attribute.getUri() : null,
            Comparator.nullsLast(String::compareTo))
        .thenComparing(attribute -> attribute != null ? attribute.getName() : null,
            Comparator.nullsLast(String::compareTo));
  }

  /**
   * Build a {propertyUri → template-variable-position} map for the attributes of the given class.
   * The position is derived from the left-to-right order of variable names in the URI template
   * string (e.g. "{a}/{b}" gives a=0, b=1).
   * Returns an empty map when no URI template is set or the template string is absent.
   */
  private Map<String, Integer> buildPkTemplateOrder(Clazz clazz) {
    if (clazz == null || clazz.getClassInfo() == null) {
      return Map.of();
    }
    be.vlaanderen.omgeving.oddtoolkit.model.UriTemplate uriTemplate =
        clazz.getClassInfo().getUriTemplate();
    if (uriTemplate == null || uriTemplate.getTemplate() == null
        || uriTemplate.getVariables() == null || uriTemplate.getVariables().isEmpty()) {
      return Map.of();
    }

    String template = uriTemplate.getTemplate();
    Map<String, String> variableToPropertyUri = uriTemplate.getVariables();

    // Extract variable names in left-to-right appearance order from the template string.
    Map<String, Integer> variablePosition = new HashMap<>();
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("\\{([^}]+)}").matcher(template);
    int pos = 0;
    while (matcher.find()) {
      String varName = matcher.group(1);
      variablePosition.putIfAbsent(varName, pos++);
    }

    // Invert: propertyUri → position
    Map<String, Integer> pkOrder = new HashMap<>();
    variableToPropertyUri.forEach((varName, propertyUri) -> {
      Integer idx = variablePosition.get(varName);
      if (idx != null) {
        pkOrder.put(propertyUri, idx);
      }
    });
    return pkOrder;
  }

  /** Returns the template-position index for a PK attribute, or MAX_VALUE when not in template. */
  private int pkTemplateIndex(Attribute attribute, Map<String, Integer> pkTemplateOrder) {
    if (attribute == null || !attribute.isPrimaryKey()) {
      return Integer.MAX_VALUE; // only meaningful for bucket-0 attributes
    }
    String uri = attribute.getUri();
    return uri != null ? pkTemplateOrder.getOrDefault(uri, Integer.MAX_VALUE) : Integer.MAX_VALUE;
  }

  private int attributeBucket(Attribute attribute, Map<String, Integer> extraOrder,
      Map<String, Integer> temporalOrder) {
    // Primary keys always come first, before extra-properties and temporal fields.
    if (attribute != null && attribute.isPrimaryKey()) {
      return 0;
    }
    String uri = attribute != null ? attribute.getUri() : null;
    if (uri != null && extraOrder.containsKey(uri)) {
      return 1;
    }
    if (uri != null && temporalOrder.containsKey(uri)) {
      return 2;
    }
    return 3;
  }

  private int orderIndex(Attribute attribute, Map<String, Integer> orderMap) {
    String uri = attribute != null ? attribute.getUri() : null;
    return uri != null ? orderMap.getOrDefault(uri, Integer.MAX_VALUE) : Integer.MAX_VALUE;
  }

  public void applyFilters() {
    filterEnums();
    filterInterfaces();
    filterInterfaceProperties();
    filterInheritedProperties();
    filterSuperClasses();
    getAllGeneratedClasses().forEach(this::filterInverseProperties);
  }

  private void updateRanges() {
    List<Clazz> completeList = new ArrayList<>();
    completeList.addAll(classes);
    completeList.addAll(interfaces);
    completeList.addAll(enums);
    completeList.forEach(clazz -> clazz.getAttributes().forEach(attribute -> {
      if (attribute.getRangeClasses() != null && !attribute.getRangeClasses().isEmpty()) {
        List<Clazz> updatedRangeClasses = attribute.getRangeClasses().stream()
            .map(rangeClazz -> {
              ClassInfo nearestClass = getNearestClass(rangeClazz.getClassInfo());
              return nearestClass != null ? findNeareast(nearestClass) : rangeClazz;
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (!updatedRangeClasses.isEmpty()) {
          attribute.setRangeClasses(updatedRangeClasses);
        }
      }
      if (attribute.getRange() != null) {
        ClassInfo nearestClass = getNearestClass(attribute.getRange().getClassInfo());
        if (nearestClass != null) {
          attribute.setRange(findNeareast(nearestClass));
          attribute.setDataType(
              new DataType(attribute.getRange().getName(), attribute.getRange().getUri()));
        }
      }
    }));
  }

  private <T extends Clazz> T createClass(ClassInfo c, Class<T> classType)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    Pair<String, String> nameAndLabel = getClassNameAndLabel(c);
    T clazz = classType.getConstructor().newInstance();
    clazz.setClassInfo(c);
    clazz.setName(nameAndLabel.getLeft());
    List<Attribute> attributes = new ArrayList<>();
    c.getProperties()
        .forEach(p -> {
          Pair<String, String> propertyNameAndLabel = getPropertyNameAndLabel(p);
          Attribute attribute = new Attribute();
          attribute.setPropertyInfo(p);
          attribute.setName(propertyNameAndLabel.getLeft());
          attribute.setCardinality(getCardinality(p));
          attribute.setDomain(clazz);
          attribute.setPrimaryKey(p.isIdentifier());
          attribute.setNullable(!p.isIdentifier() && (attribute.getCardinality() == null || !attribute.getCardinality()
              .isMinOne()));
          // Check if the property is a relation to another class
          if (!p.getRange().isEmpty() && p.getRange().getFirst().equals(RDFS.Datatype.getURI())) {
            // Set datatype to string
            attribute.setDataType(new DataType("String", XSD.xstring.getURI()));
            // Create an additional attribute
            Attribute datatypeAttribute = new Attribute();
            datatypeAttribute.setPropertyInfo(p);
            // Derive from the original attribute name so this doesn't collide with it
            // (e.g. when the property itself happens to be named "datatype").
            datatypeAttribute.setName(attribute.getName() + "_datatype");
            datatypeAttribute.setDomain(clazz);
            datatypeAttribute.setCardinality(Cardinality.ONE_TO_ONE);
            datatypeAttribute.setDataType(new DataType("String", XSD.xstring.getURI()));
            attributes.add(datatypeAttribute);
          } else if (p.getRange() != null && p.getRange().stream()
              .noneMatch(uri -> getOntologyClasses().stream()
                  .anyMatch(otherClass -> otherClass.getUri().equals(uri)))) {
            // Determine a data type based on XSD type or default to VARCHAR
            String dataType = p.getRange() != null && !p.getRange().isEmpty() ?
                p.getRange().getFirst() : XSD.xstring.getURI();
            attribute.setDataType(new DataType(null, dataType));
          }
          attributes.add(attribute);
        });
    clazz.setAttributes(attributes);
    return clazz;
  }

  protected void extractDataTypes() {
    List<Clazz> completeList = new ArrayList<>();
    completeList.addAll(classes);
    completeList.addAll(interfaces);
    completeList.forEach(clazz -> clazz.getAttributes().forEach(attribute -> {
      if (attribute.getDataType().getName() == null) {
        attribute.getDataType().setName(getReadableDataType(attribute.getDataType().getUri()));
      }
    }));
  }

  protected void extractRelations() {
    for (Clazz clazz : classes) {
      for (Attribute attribute : clazz.getAttributes()) {
        if (attribute.getPropertyInfo() != null
            && attribute.getPropertyInfo() instanceof PropertyInfo propertyInfo) {
          if (propertyInfo.getRange() != null && !propertyInfo.getRange().isEmpty()) {
            List<Clazz> rangeClasses = new ArrayList<>();
            for (String rangeUri : propertyInfo.getRange()) {
              ClassInfo rangeClass = getNearestClass(rangeUri);
              if (rangeClass != null) {
                Clazz rangeClazz = findNeareast(rangeClass);
                if (rangeClazz != null) {
                  rangeClasses.add(rangeClazz);
                }
              }
            }

            if (!rangeClasses.isEmpty()) {
              rangeClasses = dedupeAncestorRanges(rangeClasses, propertyInfo);
              attribute.setRangeClasses(rangeClasses);
              // For backwards compatibility, set range to the first class
              attribute.setRange(rangeClasses.getFirst());
              attribute.setDataType(
                  new DataType(attribute.getRange().getName(), attribute.getRange().getUri()));
            }
          }
        }
      }
    }
  }

  /**
   * When a property's range spans both a class and one of its own subclasses (e.g. two separate
   * OWL restrictions on the same property, one {@code someValuesFrom ssn:System} and another
   * {@code someValuesFrom :Installatie} where {@code :Installatie rdfs:subClassOf ssn:System}),
   * only the most general (ancestor) class is kept. A descendant is always also an instance of
   * its ancestor, so the ancestor's table is a valid target for every value, while keeping both
   * would attach two competing relations to the same physical column.
   */
  private List<Clazz> dedupeAncestorRanges(List<Clazz> rangeClasses, PropertyInfo propertyInfo) {
    List<Clazz> result = new ArrayList<>();
    for (Clazz candidate : rangeClasses) {
      if (result.stream().anyMatch(existing -> isStrictAncestor(existing, candidate))) {
        logger.debug("Dropping redundant range '{}' for property '{}': it is a subclass already "
                + "covered by a more general range in the same restriction.",
            candidate.getUri(), propertyInfo.getUri());
        continue;
      }
      List<Clazz> supersededByCandidate = result.stream()
          .filter(existing -> isStrictAncestor(candidate, existing))
          .toList();
      supersededByCandidate.forEach(existing -> logger.debug(
          "Dropping redundant range '{}' for property '{}': it is a subclass already covered by "
              + "the more general range '{}'.",
          existing.getUri(), propertyInfo.getUri(), candidate.getUri()));
      result.removeAll(supersededByCandidate);
      result.add(candidate);
    }
    return result;
  }

  private boolean isStrictAncestor(Clazz ancestor, Clazz descendant) {
    if (ancestor == descendant || ancestor.getClassInfo() == null || descendant.getClassInfo() == null
        || ancestor.getUri() == null) {
      return false;
    }
    return descendant.getClassInfo().isSubClassOf(ancestor.getUri());
  }

  @SuppressWarnings("unchecked")
  private <T extends Clazz> T findNeareast(ClassInfo classInfo) {
    for (Clazz clazz : classes) {
      if (clazz.getUri().equals(classInfo.getUri())) {
        return (T) clazz;
      }
    }
    for (Interface interfaceInfo : interfaces) {
      if (interfaceInfo.getUri().equals(classInfo.getUri())) {
        return (T) interfaceInfo;
      }
    }
    for (Enum enumInfo : enums) {
      if (enumInfo.getUri().equals(classInfo.getUri())) {
        return (T) enumInfo;
      }
    }
    return null;
  }

  protected void extractClasses() {
    this.classes.addAll(getOntologyClasses()
        .stream()
        .map(c -> {
          try {
            return createClass(c, Clazz.class);
          } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                   IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .toList());
  }

  protected void extractInterfaces() {
    this.interfaces.addAll(getAllClasses()
        .stream()
        .filter(c -> c.getScope() == Scope.EXTERNAL)
        .map(c -> {
          try {
            return createClass(c, Interface.class);
          } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                   IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .toList());
    // Update the classes to set the interfaces they implement based on the super classes that are interfaces
    this.classes.forEach(clazz -> {
      List<Interface> implementedInterfaces = interfaces.stream()
          .filter(i -> clazz.getClassInfo().getSuperClasses() != null && clazz.getClassInfo()
              .getSuperClasses().stream()
              .anyMatch(ci -> ci.getUri().equals(i.getUri())))
          .toList();
      clazz.setInterfaces(implementedInterfaces);
    });
  }

  protected void extractEnums() {
    this.enums.addAll(getAllClasses()
        .stream()
        .filter(c -> getOntologyConfiguration().getEnumClasses().getClasses().contains(c.getUri()))
        .map(c -> {
          try {
            return createClass(c, Enum.class);
          } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                   IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .toList());
  }

  protected void extractMetadataClasses() {
    var metadataConfig = getOntologyConfiguration().getMetadataClasses();
    if (metadataConfig == null || metadataConfig.getClasses().isEmpty()) {
      return;
    }

    String suffix = metadataConfig.getSuffix();
    String keyProperty = metadataConfig.getKey();
    String valueProperty = metadataConfig.getValue();
    List<String> temporalProperties = getOntologyConfiguration().getTemporalProperties();

    for (String classUri : metadataConfig.getClasses()) {
      // Find the corresponding class
      classes.stream()
          .filter(c -> c.getUri().equals(classUri))
          .findFirst().ifPresent(
              sourceClass -> createMetadataClass(sourceClass, suffix, keyProperty, valueProperty,
                  temporalProperties));

    }
  }

  private void createMetadataClass(Clazz sourceClass, String suffix, String keyProperty,
                                    String valueProperty, List<String> temporalProperties) {
    Clazz metadataClass = new Clazz();
    metadataClass.setName(sourceClass.getName() + suffix);
    metadataClass.setClassInfo(sourceClass.getClassInfo());

    List<Attribute> metadataAttributes = new ArrayList<>();

    // Add key attribute
    Attribute keyAttr = new Attribute();
    keyAttr.setName("key");
    PropertyInfo keyPropInfo = new PropertyInfo(sourceClass.getClassInfo().getScope(),
        getOntologyInfo().getModel().createResource(keyProperty));
    keyPropInfo.setName("key");
    keyPropInfo.setCardinalityTo(new PropertyInfo.Cardinality());
    keyPropInfo.getCardinalityTo().setMin(1);
    keyPropInfo.getCardinalityTo().setMax(1);
    keyPropInfo.setCardinalityFrom(new PropertyInfo.Cardinality());
    keyAttr.setPropertyInfo(keyPropInfo);
    keyAttr.setCardinality(Cardinality.ONE_TO_ONE);
    keyAttr.setDomain(metadataClass);
    keyAttr.setDataType(new DataType("String", XSD.xstring.getURI()));
    metadataAttributes.add(keyAttr);

    // Add value attribute
    Attribute valueAttr = new Attribute();
    valueAttr.setName("value");
    PropertyInfo valuePropInfo = new PropertyInfo(sourceClass.getClassInfo().getScope(),
        getOntologyInfo().getModel().createResource(valueProperty));
    valuePropInfo.setName("value");
    valuePropInfo.setCardinalityTo(new PropertyInfo.Cardinality());
    valuePropInfo.getCardinalityTo().setMin(1);
    valuePropInfo.getCardinalityTo().setMax(1);
    valuePropInfo.setCardinalityFrom(new PropertyInfo.Cardinality());
    valueAttr.setPropertyInfo(valuePropInfo);
    valueAttr.setCardinality(Cardinality.ONE_TO_ONE);
    valueAttr.setDomain(metadataClass);
    valueAttr.setDataType(new DataType("String", XSD.xstring.getURI()));
    metadataAttributes.add(valueAttr);

    // Add temporal properties
    if (temporalProperties != null && !temporalProperties.isEmpty()) {
      for (String temporalPropUri : temporalProperties) {
        // Find if source class has this temporal property
        sourceClass.getAttributes().stream()
            .filter(attr -> attr.getUri() != null && attr.getUri().equals(temporalPropUri))
            .findFirst()
            .ifPresent(sourceAttr -> {
              Attribute temporalAttr = new Attribute();
              temporalAttr.setName(sourceAttr.getName());
              temporalAttr.setPropertyInfo(sourceAttr.getPropertyInfo());
              temporalAttr.setCardinality(sourceAttr.getCardinality());
              temporalAttr.setDomain(metadataClass);
              temporalAttr.setDataType(sourceAttr.getDataType());
              temporalAttr.setRange(sourceAttr.getRange());
              metadataAttributes.add(temporalAttr);
            });
      }
    }

    // Link metadata -> source class (many metadata entries can point to one source entity).
    Attribute sourceReferenceAttr = new Attribute();
    String sourceReferenceName = sourceClass.getName().substring(0, 1).toLowerCase()
        + sourceClass.getName().substring(1);
    sourceReferenceAttr.setName(sourceReferenceName);

    String relationUri = sourceClass.getUri() + "/metadata-reference";
    PropertyInfo relationInfo = new PropertyInfo(sourceClass.getClassInfo().getScope(),
        getOntologyInfo().getModel().createResource(relationUri));
    relationInfo.setName(sourceReferenceName);
    relationInfo.setCardinalityTo(new PropertyInfo.Cardinality());
    relationInfo.getCardinalityTo().setMin(1);
    relationInfo.getCardinalityTo().setMax(1);
    relationInfo.setCardinalityFrom(new PropertyInfo.Cardinality());
    relationInfo.getRange().add(sourceClass.getUri());

    sourceReferenceAttr.setPropertyInfo(relationInfo);
    sourceReferenceAttr.setCardinality(Cardinality.MANY_TO_ONE);
    sourceReferenceAttr.setDomain(metadataClass);
    sourceReferenceAttr.setRange(sourceClass);
    sourceReferenceAttr.setDataType(new DataType(sourceClass.getName(), sourceClass.getUri()));
    metadataAttributes.add(sourceReferenceAttr);

    metadataClass.setAttributes(metadataAttributes);
    this.classes.add(metadataClass);
  }

  protected void filterInverseProperties(Clazz clazz) {
    // Filter inverse properties and copy the cardinality
    // Then we can remove the inverse properties from the class and only keep the original properties
    clazz.getAttributes()
        .stream()
        .filter(a -> a.getPropertyInfo() != null
            && a.getPropertyInfo() instanceof PropertyInfo propertyInfo
            && propertyInfo.getInverseOf() != null)
        .forEach(attribute -> {
          PropertyInfo propertyInfo = (PropertyInfo) attribute.getPropertyInfo();
          classes
              .stream()
              .flatMap(c -> c.getAttributes().stream())
              .filter(p -> p.getUri().equals(propertyInfo.getInverseOf()))
              .findFirst()
              .ifPresent(inverseAttribute -> {
                PropertyInfo inverseProperty = (PropertyInfo) inverseAttribute.getPropertyInfo();
                propertyInfo.setCardinalityFrom(inverseProperty.getCardinalityTo());
                attribute.setCardinality(getCardinality(propertyInfo));
                inverseProperty.setCardinalityFrom(propertyInfo.getCardinalityTo());
                inverseAttribute.setCardinality(getCardinality(inverseProperty));

                // Check that one of the properties has a comment, if not we can keep either one of them
                if ((propertyInfo.getComment() == null || propertyInfo.getComment().isEmpty()) && (
                    inverseProperty.getComment() == null || inverseProperty.getComment()
                        .isEmpty())) {
                  // Keep the property with the lower URI (arbitrary choice to keep one of them)
                  if (propertyInfo.getUri().compareTo(inverseProperty.getUri()) < 0) {
                    propertyInfo.setComment("Inverse property of " + inverseProperty.getUri());
                  } else {
                    inverseProperty.setComment("Inverse property of " + propertyInfo.getUri());
                  }
                }
              });
        });
    // If the class is an interface then remove all inverse properties
    if (isInterface(clazz.getClassInfo())) {
      clazz.setAttributes(clazz.getAttributes().stream()
          .filter(a -> a.getPropertyInfo() == null || (
              a.getPropertyInfo() instanceof PropertyInfo propertyInfo
                  && propertyInfo.getInverseOf() == null))
          .toList());
      return;
    }
    // Remove inverse properties if they don't have a comment or many to one
    List<Attribute> remainingProperties = clazz.getAttributes()
        .stream()
        .filter(
            p -> {
              PropertyInfo propertyInfo = (PropertyInfo) p.getPropertyInfo();
              return (propertyInfo.getInverseOf() == null || (propertyInfo.getComment() != null
                  && !propertyInfo.getComment().isEmpty()))
                  && !p.getCardinality().equals(Cardinality.ONE_TO_MANY);
            })
        .toList();
    clazz.setAttributes(remainingProperties);
  }

  protected void filterInterfaces() {
    this.interfaces = interfaces
        .stream()
        .filter(i -> classes.stream()
            .flatMap(c -> c.getAttributes().stream())
            .anyMatch(p -> p.getRange() != null && p.getRange().getUri().equals(i.getUri())))
        .toList();
    this.interfaces = interfaces
        .stream()
        .filter(i -> classes.stream()
            .filter(c -> c.getInterfaces() != null && c.getInterfaces().stream()
                .anyMatch(ci -> ci.getUri().equals(i.getUri())))
            .count() > 1)
        .toList();
    this.classes = classes
        .stream()
        .peek(c -> {
          List<Interface> filteredInterfaces = c.getInterfaces().stream()
              .filter(i -> interfaces.stream()
                  .anyMatch(keptInterface -> keptInterface.getUri().equals(i.getUri())))
              .toList();
          c.setInterfaces(filteredInterfaces);
        })
        .toList();
  }

  protected void filterInterfaceProperties() {
    // Filter the properties of the interfaces to only include properties
    // used by ALL concrete classes that implement the interface
    this.interfaces = interfaces
        .stream()
        .map(i -> {
          List<Clazz> implementingClasses = classes.stream()
              .filter(c -> c.getInterfaces() != null && c.getInterfaces().stream()
                  .anyMatch(ci -> ci.getUri().equals(i.getUri())))
              .toList();
          if (implementingClasses.isEmpty()) {
            return i;
          }
          List<Attribute> filteredProperties = i.getAttributes().stream()
              .filter(p -> implementingClasses.stream()
                  .allMatch(c -> c.getAttributes().stream()
                      .anyMatch(cp -> cp.getPropertyInfo().getUri()
                          .equals(p.getPropertyInfo().getUri()))))
              .toList();
          i.setAttributes(filteredProperties);
          return i;
        })
        .toList();
  }

  protected void filterInheritedProperties() {
    this.classes = classes
        .stream()
        .map(c -> {
          List<ClassInfo> superClasses = c.getClassInfo().getSuperClasses()
              .stream().filter(this::isConcreteClass)
              .toList();
          if (superClasses.isEmpty()) {
            return c;
          }
          List<PropertyInfo> inheritedProperties = superClasses.stream()
              .flatMap(sc -> getAllClasses().stream()
                  .filter(other -> other.getUri().equals(sc.getUri()))
                  .flatMap(other -> other.getProperties().stream()))
              .toList();
          List<Attribute> filteredProperties = c.getAttributes().stream()
              .filter(p -> inheritedProperties.stream()
                  .noneMatch(ip -> ip.getUri().equals(p.getPropertyInfo().getUri())))
              .toList();
          c.setAttributes(filteredProperties);
          return c;
        })
        .toList();
  }

  protected void filterSuperClasses() {
    this.classes = classes
        .stream()
        .map(c -> {
          List<ClassInfo> superClasses = c.getClassInfo().getSuperClasses();
          if (superClasses == null) {
            return c;
          }
          Clazz superClass = superClasses.stream()
              .filter(sc -> superClasses.stream()
                  .map(other -> getAllClasses().stream()
                      .filter(cc -> cc.getUri().equals(other.getUri()))
                      .findFirst()
                      .orElse(null)).filter(Objects::nonNull)
                  .filter(other -> !other.equals(sc))
                  .noneMatch(other -> other.isSubClassOf(sc.getUri())))
              .filter(sc -> classes.stream()
                  .anyMatch(cc -> cc.getUri().equals(sc.getUri())))
              .map(this::getClass)
              .findFirst()
              .orElse(null);
          c.setExtendsClass(superClass);
          return c;
        })
        .toList();
  }


  protected void filterEnums() {
    this.classes = classes
        .stream()
        .filter(c -> !isEnum(c.getClassInfo()))
        .toList();
    this.enums.forEach(enumInfo -> {
      enumInfo.setAttributes(new ArrayList<>());
      List<ClassInfo> subclasses = new ArrayList<>();
      getSubClasses(enumInfo.getClassInfo())
          .stream()
          // Skip subclasses that have domain-specific properties (they're not pure enum values)
          .filter(c -> c.getProperties().isEmpty() || c.getProperties().stream()
              .allMatch(p -> p.isIdentifier() || getOntologyConfiguration().getExtraProperties()
                  .stream()
                  .anyMatch(ep -> ep.getUri().equals(p.getUri()))))
          .forEach(subclasses::add);
      enumInfo.getClassInfo().getIndividuals()
          .stream()
          .map(i -> new ClassInfo(enumInfo.getClassInfo().getScope(), i))
          .forEach(subclasses::add);
      enumInfo.setValues(subclasses
          .stream().map(clazz -> {
            EnumValue enumValue = new EnumValue();
            enumValue.setPropertyInfo(clazz);
            // Set to UPPER_SNAKE_CASE and optionally trim enum-class prefix/suffix tokens.
            enumValue.setName(toEnumValueName(clazz.getName(), enumInfo.getName()));
            return enumValue;
          })
          .toList());
      this.classes = classes
          .stream()
          .filter(c -> !subclasses.contains(c.getClassInfo()))
          .toList();
      this.interfaces = interfaces
          .stream()
          .filter(i -> !subclasses.contains(i.getClassInfo()))
          .toList();
    });
  }

  /**
   * Returns the nearest concrete class, interface, or enum for the given class URI,
   * or {@code null} if none is found.
   */
  public ClassInfo getNearestClass(String classUri) {
    if (classUri == null) {
      return null;
    }
    ClassInfo classInfo = getAllClasses()
        .stream()
        .filter(c -> c.getUri().equals(classUri))
        .findFirst()
        .orElse(null);
    if (classInfo == null) {
      return null;
    }
    return getNearestClass(classInfo);
  }

  protected String toEnumValueName(String enumValueName, String enumClassName) {
    String enumConstantName = toSnakeCase(enumValueName).toUpperCase();

    if (!getOntologyConfiguration().getEnumClasses().isTrimClassNameFromValues()) {
      return enumConstantName;
    }

    String enumClassToken = toSnakeCase(enumClassName).toUpperCase();
    return trimRedundantEnumClassToken(enumConstantName, enumClassToken);
  }

  private String trimRedundantEnumClassToken(String enumConstantName, String enumClassToken) {
    if (enumConstantName == null || enumConstantName.isBlank() || enumClassToken == null
        || enumClassToken.isBlank()) {
      return enumConstantName;
    }

    String trimmedName = enumConstantName;
    String prefixToken = enumClassToken + "_";
    String suffixToken = "_" + enumClassToken;

    if (trimmedName.startsWith(prefixToken)) {
      trimmedName = trimmedName.substring(prefixToken.length());
    }
    if (trimmedName.endsWith(suffixToken)) {
      trimmedName = trimmedName.substring(0, trimmedName.length() - suffixToken.length());
    }

    // Keep original value if trimming would result in an empty enum constant name.
    return trimmedName.isBlank() ? enumConstantName : trimmedName;
  }

  /**
   * Returns the nearest concrete class, interface, or enum for the given class, walking up the
   * hierarchy when the class itself is not directly represented.
   */
  public ClassInfo getNearestClass(ClassInfo classInfo) {
    if (isConcreteClass(classInfo) || isInterface(classInfo) || isEnum(classInfo)) {
      return classInfo;
    }
    for (Interface interfaceInfo : interfaces) {
      if (interfaceInfo.getClassInfo().isSubClassOf(classInfo.getUri())) {
        return interfaceInfo.getClassInfo();
      }
    }
    for (Enum enumInfo : enums) {
      if (enumInfo.getClassInfo().isSubClassOf(classInfo.getUri())) {
        return enumInfo.getClassInfo();
      }
    }
    for (Clazz concreteClassInfo : classes) {
      if (concreteClassInfo.getClassInfo().isSubClassOf(classInfo.getUri())) {
        return concreteClassInfo.getClassInfo();
      }
    }
    return null;
  }

  public boolean isConcreteClass(ClassInfo classInfo) {
    return classes
        .stream().map(Clazz::getClassInfo)
        .anyMatch(uri -> uri.getUri().equals(classInfo.getUri()));
  }

  public boolean isInterface(ClassInfo classInfo) {
    return interfaces.stream().map(Interface::getClassInfo)
        .anyMatch(uri -> uri.getUri().equals(classInfo.getUri()));
  }

  public boolean isEnum(ClassInfo classInfo) {
    return enums.stream().map(Enum::getClassInfo)
        .anyMatch(uri -> uri.getUri().equals(classInfo.getUri()));
  }

  public List<ClassInfo> getSubClasses(ClassInfo classInfo) {
    return getAllClasses().stream()
        .filter(c -> c.isSubClassOf(classInfo.getUri()))
        .toList();
  }

  protected String getReadableDataType(String dataTypeUri) {
    if (dataTypeUri == null) {
      return "String";
    }
    // Try to find a class, interface, enum first
    Clazz clazz = getClass(getNearestClass(dataTypeUri));
    if (clazz != null) {
      return clazz.getName();
    }
    // Extract the local name from the URI
    Resource resource = getOntologyInfo().getModel().createResource(dataTypeUri);
    // Convert to a readable name (e.g. "string" -> "String")
    String localName = resource.getLocalName();
    if (localName == null) {
      localName = dataTypeUri;
    }
    return localName.substring(0, 1).toUpperCase() + localName.substring(1);
  }

  public enum ClassType {
    CLASS(null),
    INTERFACE("interface"),
    ENUM("enumerable");

    @Getter
    private final String value;

    ClassType(String value) {
      this.value = value;
    }
  }

  protected Clazz getClass(ClassInfo classInfo) {
    if (classInfo == null) {
      return null;
    }
    List<Clazz> completeList = new ArrayList<>();
    completeList.addAll(classes);
    completeList.addAll(interfaces);
    completeList.addAll(enums);
    return completeList.stream()
        .filter(c -> c.getUri().equals(classInfo.getUri()))
        .findFirst()
        .orElse(null);
  }

  protected Enum getEnum(ClassInfo classInfo) {
    return enums.stream()
        .filter(e -> e.getUri().equals(classInfo.getUri()))
        .findFirst()
        .orElse(null);
  }

  protected Cardinality getCardinality(PropertyInfo property) {
    boolean fromIsMany = property.getCardinalityFrom().getMax() == null
        || property.getCardinalityFrom().getMax() > 1;
    boolean toIsMany = property.getCardinalityTo().getMax() == null
        || property.getCardinalityTo().getMax() > 1;

    if (!fromIsMany && !toIsMany) {
      return Cardinality.ONE_TO_ONE;
    } else if (!fromIsMany) {
      return Cardinality.ONE_TO_MANY;
    } else if (!toIsMany) {
      return Cardinality.MANY_TO_ONE;
    } else {
      return Cardinality.MANY_TO_MANY;
    }
  }

  @Getter
  @Setter
  public static class Clazz {

    private List<Interface> interfaces = new ArrayList<>();
    private @Nullable Clazz extendsClass;
    private ClassInfo classInfo;
    private String name;
    private List<Attribute> attributes = new ArrayList<>();

    public Clazz() {
    }

    public Clazz(Clazz clazz) {
      this.classInfo = clazz.getClassInfo();
      this.name = clazz.getName();
      this.attributes = new ArrayList<>(clazz.getAttributes());
      this.interfaces = new ArrayList<>(clazz.getInterfaces());
      this.extendsClass = clazz.getExtendsClass();
    }

    public String getUri() {
      return classInfo != null ? classInfo.getUri() : null;
    }

    public String toString() {
      return name;
    }

    public Clazz copy() {
      return new Clazz(this);
    }
  }

  @Getter
  @Setter
  public static class Interface extends Clazz {

  }

  @Getter
  @Setter
  public static class Enum extends Clazz {

    private List<EnumValue> values = new ArrayList<>();

    public Enum() {}

    public Enum(Enum enumInfo) {
      super(enumInfo);
      this.values = new ArrayList<>(enumInfo.getValues());
    }

    public Enum copy() {
      return new Enum(this);
    }
  }

  @Getter
  @Setter
  public static class EnumValue extends Attribute {

  }

  @Getter
  @Setter
  protected static class Attribute {

    private AbstractInfo propertyInfo;
    private String name;
    private Cardinality cardinality;
    private Clazz domain;
    private Clazz range;
    private List<Clazz> rangeClasses = new ArrayList<>();
    private DataType dataType;
    private boolean primaryKey;
    private boolean nullable;

    public String getUri() {
      return propertyInfo != null ? propertyInfo.getUri() : null;
    }

    public String toString() {
      return name;
    }

    public boolean equals(Attribute attribute) {
      return attribute.getUri().equals(getUri()) && attribute.getDomain().getUri()
          .equals(getDomain().getUri()) && attribute.getRange() != null && getRange() != null
          && attribute.getRange().getUri().equals(getRange().getUri());
    }

    /**
     * Returns true if this attribute has a union type (multiple range classes).
     */
    public boolean isUnionType() {
      return rangeClasses != null && rangeClasses.size() > 1;
    }
  }

  @Getter
  @Setter
  protected static class DataType {

    private String name;
    private String uri;

    public DataType(String name, String uri) {
      this.name = name;
      this.uri = uri;
    }

    public String toString() {
      return name != null ? name : uri;
    }
  }
}
