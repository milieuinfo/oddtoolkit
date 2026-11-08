package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.JavaGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.JavaGeneratorProperties.DateTimeType;
import be.vlaanderen.omgeving.oddtoolkit.config.SchemaGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.Cardinality;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.jena.atlas.lib.Pair;
import org.jspecify.annotations.Nullable;

public class JavaGenerator extends SchemaGenerator {

  private static final Comparator<Pair<String, String>> DEPENDENCY_ORDER = Comparator
      .comparing((Pair<String, String> p) -> p != null ? p.getLeft() : null,
          Comparator.nullsLast(String::compareTo))
      .thenComparing(p -> p != null ? p.getRight() : null,
          Comparator.nullsLast(String::compareTo));

  // Generated with json-ld: true, so references to entities serialize as their URI.
  private static final String IRI_REFERENCE = "IriReferentie";
  private static final String IRI_REFERENCE_SERIALIZER = "IriReferentieSerializer";

  private final Map<Clazz, String> fileNames = new HashMap<>();
  private final Map<String, String> nameMapping = new HashMap<>();
  private final Set<String> expectedJavaFileNames = new HashSet<>();

  private final JavaGeneratorProperties generatorProperties;

  public JavaGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      JavaGeneratorProperties generatorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties,
        schemaGeneratorProperties);
    this.generatorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    prepareFileNames();
    generateFile(getClasses(), null);
    generateFile(getInterfaces(), "interface");
    generateFile(getEnums(), "enum");
    if (isJsonLd()) {
      generateIriReferenceFiles();
    }
  }

  private String getPackageName() {
    return generatorProperties.getPackageName();
  }

  private boolean isJsonLd() {
    return generatorProperties.isJsonLd();
  }

  private String getBasePath() {
    return generatorProperties.getOutputDirectory();
  }

  protected void prepareFileNames() {
    expectedJavaFileNames.clear();
    getClasses().forEach(clazz -> {
      String fileName = clazz.getName() + ".java";
      fileNames.put(clazz, fileName);
      expectedJavaFileNames.add(fileName);
    });
    getInterfaces().forEach(clazz -> {
      String fileName = "I" + clazz.getName() + ".java";
      fileNames.put(clazz, fileName);
      expectedJavaFileNames.add(fileName);
    });
    getEnums().forEach(clazz -> {
      String fileName = clazz.getName() + ".java";
      fileNames.put(clazz, fileName);
      expectedJavaFileNames.add(fileName);
    });
    if (isJsonLd()) {
      expectedJavaFileNames.add(IRI_REFERENCE + ".java");
      expectedJavaFileNames.add(IRI_REFERENCE_SERIALIZER + ".java");
    }
    getClasses().forEach(clazz -> nameMapping.put(clazz.getName(), clazz.getName()));
    getInterfaces().forEach(clazz -> nameMapping.put(clazz.getName(), "I" + clazz.getName()));
    getEnums().forEach(clazz -> nameMapping.put(clazz.getName(), clazz.getName()));
  }

  protected void generateFile(List<? extends Clazz> classes, @Nullable String type) {
    if (generatorProperties.isCleanupStaleFiles()) {
      cleanupStaleJavaFiles();
    }
    classes.forEach(clazz -> {
      Table equivalentTable = getTableByClazz(clazz, false);

      String typeDeclaration = "class";
      switch (type) {
        case "interface" -> {
          clazz.setName("I" + clazz.getName());
          typeDeclaration = "interface";
        }
        case "enum" -> {
          typeDeclaration = "enum";
        }
        case null -> {
        }
        default -> throw new IllegalStateException("Unexpected value: " + type);
      }

      String fileName = clazz.getName() + ".java";
      StringBuilder builder = new StringBuilder();
      builder.append("package ").append(getPackageName()).append(";\n\n");
      boolean isInterface = clazz instanceof Interface;
      boolean isEnum = clazz instanceof Enum;

      getDependencies(clazz)
          // Skip dependencies in the same package
          .stream()
          .filter(dep -> !dep.getLeft().equals(getPackageName()))
          .sorted(DEPENDENCY_ORDER)
          .forEach(
              dep -> builder.append("import ").append(dep.getLeft()).append('.')
                  .append(dep.getRight())
                  .append(";\n"));
      if (!isInterface) {
        boolean hasProperties = !clazz.getAttributes().isEmpty();
        if (hasProperties || (isJsonLd() && !isEnum)) {
          builder.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
        }
        if (isJsonLd() && !isEnum) {
          builder.append("import com.fasterxml.jackson.annotation.JsonIgnore;\n");
          builder.append("import com.fasterxml.jackson.annotation.JsonSetter;\n");
          builder.append("import com.fasterxml.jackson.databind.annotation.JsonSerialize;\n");
        }
        if (isJsonLd() && isEnum) {
          builder.append("import com.fasterxml.jackson.annotation.JsonCreator;\n");
          builder.append("import com.fasterxml.jackson.annotation.JsonValue;\n");
        }
        builder.append("import lombok.Getter;\n");
        builder.append("import lombok.Setter;\n");
        builder.append("import lombok.Builder;\n");
        builder.append("import lombok.NoArgsConstructor;\n");
        builder.append("import lombok.AllArgsConstructor;\n");
        builder.append("import lombok.EqualsAndHashCode;\n");
        builder.append("import jakarta.persistence.Table;\n");
        builder.append("import jakarta.persistence.Entity;\n");
        builder.append("import jakarta.persistence.Column;\n");
        builder.append("import jakarta.persistence.Id;\n");
        builder.append("import jakarta.persistence.IdClass;\n");
        builder.append("import jakarta.persistence.EmbeddedId;\n");
        builder.append("import jakarta.persistence.Embeddable;\n");
        builder.append("import jakarta.persistence.OneToOne;\n");
        builder.append("import jakarta.persistence.OneToMany;\n");
        builder.append("import jakarta.persistence.ManyToOne;\n");
        builder.append("import jakarta.persistence.ManyToMany;\n");
        builder.append("import jakarta.persistence.JoinColumn;\n");
        builder.append("import jakarta.persistence.JoinTable;\n");
        builder.append("import jakarta.persistence.JoinColumns;\n");
        builder.append("import java.io.Serializable;\n");
        builder.append("import java.util.List;\n");
      }
      builder.append("\n");

      builder.append("/**\n");
      builder.append(" * ").append(clazz.getName()).append("\n");
      builder.append(" * <a href=\"").append(clazz.getUri()).append("\">")
          .append(clazz.getClassInfo().getName()).append("</a>\n");
      if (clazz.getClassInfo().getComment() != null) {
        builder.append(" * ").append(clazz.getClassInfo().getComment()).append("\n");
      }
      builder.append(" **/\n");

      if (!isInterface && !isEnum) {
        List<Attribute> primaryKeys = clazz.getAttributes().stream()
            .filter(Attribute::isPrimaryKey)
            .toList();
        boolean isCompositePk = primaryKeys.size() > 1;

        builder.append("@Getter\n").append("@Setter\n");
        builder.append("@Entity(name = \"").append(clazz.getName()).append("\")\n");
        builder.append("@Builder(toBuilder = true)\n");
        builder.append("@NoArgsConstructor\n").append("@AllArgsConstructor\n");
        builder.append("@Table(name = \"").append(equivalentTable.getName()).append("\")\n");
        if (isCompositePk) {
          builder.append("@IdClass(").append(clazz.getName()).append(".Id.class)\n");
        }
      }

      String extendsClause = "";
      if (clazz.getExtendsClass() != null) {
        extendsClause = " extends " + nameMapping.get(clazz.getExtendsClass().getName());
      }
      String implementsClause = "";
      if (!clazz.getInterfaces().isEmpty()) {
        implementsClause = " implements " + clazz.getInterfaces().stream()
            .map(i -> nameMapping.get(i.getName()))
            .filter(Objects::nonNull)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
      }
      if (isJsonLd() && !isInterface && !isEnum && hasUriAttribute(clazz)) {
        implementsClause = implementsClause.isEmpty()
            ? " implements " + IRI_REFERENCE
            : implementsClause + ", " + IRI_REFERENCE;
      }

      builder.append("public ").append(typeDeclaration).append(" ").append(clazz.getName())
          .append(extendsClause).append(implementsClause)
          .append(" {\n");
      if (clazz instanceof Enum enumClazz) {
        // Enum constants can't be assigned a value with "NAME = \"...\"" (that's not valid Java
        // syntax, only e.g. TypeScript) -- this previously produced Java that never compiled
        // (documentatie/datamodel/generated/java/{Status,Procedure}.java were only ever packaged
        // as a plain classpath resource, never actually compiled, which is how this went
        // unnoticed). Each constant instead gets its URI via a constructor argument, exposed
        // through a getUri() accessor.
        List<EnumValue> values = enumClazz.getValues();
        for (int i = 0; i < values.size(); i++) {
          EnumValue value = values.get(i);
          boolean isLast = i == values.size() - 1;
          builder.append("\t").append(value.getName()).append("(\"").append(value.getUri())
              .append("\")").append(isLast ? ";\n" : ",\n");
        }
        if (!values.isEmpty()) {
          builder.append("\n\tprivate final String uri;\n\n");
          builder.append("\t").append(clazz.getName()).append("(String uri) {\n");
          builder.append("\t\tthis.uri = uri;\n");
          builder.append("\t}\n\n");
          if (isJsonLd()) {
            builder.append("\t@JsonValue\n");
          }
          builder.append("\tpublic String getUri() {\n");
          builder.append("\t\treturn uri;\n");
          builder.append("\t}\n");
          if (isJsonLd()) {
            appendEnumFromUri(builder, clazz.getName());
          }
        }
      }
      // Methods emitted after the fields (json-ld only).
      StringBuilder methods = new StringBuilder();
      clazz.getAttributes().forEach(prop -> {
        builder.append("\t/**\n");
        builder.append("\t * ").append(prop.getPropertyInfo().getName()).append("\n");
        builder.append("\t * <a href=\"").append(prop.getUri()).append("\">")
            .append(prop.getPropertyInfo().getName()).append("</a>\n");
        if (prop.getPropertyInfo().getComment() != null) {
          builder.append("\t * ").append(prop.getPropertyInfo().getComment()).append("\n");
        }
        builder.append("\t */\n");

        if (!isInterface && prop.isUnionType() && prop.getCardinality().equals(Cardinality.MANY_TO_MANY)) {
          appendUnionManyToManyFields(builder, clazz, equivalentTable, prop);
          return;
        }

        boolean isArray = prop.getCardinality().isToMany();
        String dataType = isArray ? "List<" + getJavaType(prop.getDataType()).getRight() + ">" : getJavaType(
            prop.getDataType()).getRight();
        if (isInterface) {
          boolean isBoolean = getJavaType(prop.getDataType()).getRight().equals("Boolean");
          String getterName =
              (isBoolean ? "is" : "get") + prop.getName().substring(0, 1).toUpperCase()
                  + prop.getName().substring(1);
          String setterName =
              "set" + prop.getName().substring(0, 1).toUpperCase() + prop.getName().substring(1);
          builder.append("\t")
              .append(dataType)
              .append(" ").append(getterName).append("();\n");
          builder.append("\t")
              .append("void ").append(setterName).append("(")
              .append(dataType)
              .append(" ").append(prop.getName()).append(");\n");
          builder.append("\n");
        } else {
          Column equivalentColumn = equivalentTable.getColumnByAttribute(prop);
          if (equivalentColumn == null) {
            if (prop.getCardinality().equals(Cardinality.MANY_TO_MANY)) {
              // Many-to-many relationship
              builder.append("\t@ManyToMany\n");

              // Try to get the join table information
              // For many-to-many, we need @JoinTable with joinColumns and inverseJoinColumns
              Table targetTable = getTableByClazz(prop.getRange(), false);
              if (targetTable != null) {
                boolean appendRelationName = shouldAppendRelationNameForSelfManyToMany(clazz,
                    targetTable);
                String joinTableName = resolveJoinTableName(equivalentTable.getName(),
                    targetTable.getName(), prop.getName(), appendRelationName);

                builder.append("\t@JoinTable(\n");
                builder.append("\t\tname = \"").append(joinTableName).append("\",\n");
                builder.append("\t\tjoinColumns = @JoinColumn(name = \"source_uuid\"),\n");
                builder.append("\t\tinverseJoinColumns = @JoinColumn(name = \"target_uuid\")\n");
                builder.append("\t)\n");
              }
            } else {
              // Other relationships (one-to-one, one-to-many, many-to-one)
              switch (prop.getCardinality()) {
                case ONE_TO_ONE -> builder.append("\t@OneToOne\n");
                case ONE_TO_MANY -> builder.append("\t@OneToMany\n");
                case MANY_TO_ONE -> builder.append("\t@ManyToOne\n");
                default -> {
                }
              }
            }
          } else if (prop.getRange() == null) {
            String columnName = equivalentColumn.getName();
            String mergeJoinTableAttributeName = getSchemaGeneratorProperties().getMergeJoinTables()
                .getAttributeName();

            if (prop.isPrimaryKey()) {
              builder.append("\t@Id\n");
            }

            if (isMergeJoinTablesColumn(equivalentTable, columnName, mergeJoinTableAttributeName)) {
              builder.append("\t@Column(name = \"").append(columnName)
                  .append("\", nullable = ").append(equivalentColumn.isNullable())
                  .append(", insertable = true, updatable = true)\n");
            } else {
              builder.append("\t@Column(name = \"").append(columnName)
                  .append("\", nullable = ").append(equivalentColumn.isNullable()).append(")\n");
            }
          } else if (!prop.getCardinality().equals(Cardinality.MANY_TO_MANY)) {
            Table rangeTable = getTableByClazz(prop.getRange(), false);
            if (rangeTable != null) {
              // Join column — add @Id when this FK is also part of the primary key
              if (prop.isPrimaryKey()) {
                builder.append("\t@Id\n");
              }
              Relation relation = equivalentTable.getRelationByAttribute(prop);
              if (relation != null && relation.getToColumn() != null) {
                builder.append("\t@JoinColumn(name = \"").append(relation.getToColumn().getName())
                    .append("\", nullable = ").append(equivalentColumn.isNullable()).append(")\n");
              }
            }
          }

          Attribute datatypeCompanion = isJsonLd() ? findDatatypeCompanion(clazz, prop) : null;
          if (isJsonLd() && datatypeCompanion != null && !isArray) {
            // Value and datatype serialize together as one typed literal, see appendTypedLiteralAccessors.
            builder.append("\t@JsonIgnore\n");
            appendTypedLiteralAccessors(methods, prop, datatypeCompanion);
          } else if (isJsonLd() && isDatatypeCompanion(clazz, prop)) {
            builder.append("\t@JsonIgnore\n");
          } else {
            if (isJsonLd() && isEntityReference(prop.getRange())) {
              builder.append("\t@JsonSerialize(").append(isArray ? "contentUsing" : "using")
                  .append(" = ").append(IRI_REFERENCE_SERIALIZER).append(".class)\n");
            }
            builder.append("\t@JsonProperty(\"").append(prop.getPropertyInfo().getName())
                .append("\")\n");
          }

          builder.append("\t").append("private ")
              .append(dataType)
              .append(" ").append(prop.getName()).append(";\n");
        }
      });

      if (isJsonLd() && !isInterface && !isEnum) {
        appendRdfTypeGetter(methods, clazz);
        builder.append(methods);
      }

      // Emit composite Id as a static inner class — keeps it scoped to its owner.
      if (!isInterface && !isEnum) {
        List<Attribute> primaryKeys = clazz.getAttributes().stream()
            .filter(Attribute::isPrimaryKey)
            .toList();
        if (primaryKeys.size() > 1) {
          builder.append("\n");
          appendCompositeIdInnerClass(builder, primaryKeys);
        }
      }

      builder.append("}\n");
      saveToFile(fileName, builder.toString());
    });
  }

  /**
   * Appends a composite primary-key {@code Id} static inner class to {@code builder}.
   * The inner class is annotated with {@code @Embeddable} and implements
   * {@link java.io.Serializable}, making it suitable for use with {@code @IdClass}.
   */
  private void appendCompositeIdInnerClass(StringBuilder builder, List<Attribute> primaryKeys) {
    builder.append("\t/** Composite primary-key class. */\n");
    builder.append("\t@Embeddable\n");
    builder.append("\t@Getter\n");
    builder.append("\t@EqualsAndHashCode\n");
    builder.append("\t@NoArgsConstructor\n");
    builder.append("\t@AllArgsConstructor\n");
    builder.append("\tpublic static class Id implements Serializable {\n");
    for (Attribute pk : primaryKeys) {
      Pair<String, String> javaType = getJavaType(pk.getDataType());
      builder.append("\t\tprivate ").append(javaType.getRight()).append(" ").append(pk.getName())
          .append(";\n");
    }
    builder.append("\t}\n");
  }

  private void appendUnionManyToManyFields(StringBuilder builder, Clazz ownerClazz,
      Table ownerTable, Attribute prop) {
    for (Clazz rangeClazz : prop.getRangeClasses()) {
      Table targetTable = getTableByClazz(rangeClazz, false);
      if (targetTable == null) {
        continue;
      }

      String targetType = nameMapping.getOrDefault(rangeClazz.getName(), rangeClazz.getName());
      String fieldName = prop.getName() + targetType;
      String jsonName = prop.getPropertyInfo().getName() + "_"
          + toSnakeCase(targetType).toLowerCase();

      boolean appendRelationName = shouldAppendRelationNameForSelfManyToMany(ownerClazz,
          targetTable);
      String joinTableName = resolveJoinTableName(ownerTable.getName(),
          targetTable.getName(), prop.getName(), appendRelationName);

      builder.append("\t@ManyToMany\n");
      builder.append("\t@JoinTable(\n");
      builder.append("\t\tname = \"").append(joinTableName).append("\",\n");
      builder.append("\t\tjoinColumns = @JoinColumn(name = \"source_uuid\"),\n");
      builder.append("\t\tinverseJoinColumns = @JoinColumn(name = \"target_uuid\")\n");
      builder.append("\t)\n");
      if (isJsonLd() && isEntityReference(rangeClazz)) {
        builder.append("\t@JsonSerialize(contentUsing = ").append(IRI_REFERENCE_SERIALIZER)
            .append(".class)\n");
      }
      builder.append("\t@JsonProperty(\"").append(jsonName).append("\")\n");
      builder.append("\tprivate List<").append(targetType).append("> ")
          .append(fieldName).append(";\n");
    }
  }

  /** Returns the {@code <name>_datatype} companion of an attribute with an rdfs:Datatype range. */
  private @Nullable Attribute findDatatypeCompanion(Clazz clazz, Attribute attribute) {
    String companionName = attribute.getName() + "_datatype";
    return clazz.getAttributes().stream()
        .filter(other -> other != attribute && companionName.equals(other.getName())
            && other.getPropertyInfo() == attribute.getPropertyInfo())
        .findFirst()
        .orElse(null);
  }

  private boolean isDatatypeCompanion(Clazz clazz, Attribute attribute) {
    return clazz.getAttributes().stream()
        .anyMatch(other -> findDatatypeCompanion(clazz, other) == attribute);
  }

  /**
   * An entity is a class with a Hydra URI template, or an interface/superclass that such a class
   * implements or extends. References to it serialize as its URI; anything else stays nested.
   */
  private boolean isEntityReference(@Nullable Clazz range) {
    if (range == null || range instanceof Enum) {
      return false;
    }
    if (hasUriTemplate(range)) {
      return true;
    }
    String rangeUri = range.getUri();
    return rangeUri != null && getClasses().stream()
        .filter(this::hasUriTemplate)
        .anyMatch(candidate -> isSubtypeOf(candidate, rangeUri));
  }

  private boolean hasUriTemplate(Clazz clazz) {
    return clazz.getClassInfo() != null && clazz.getClassInfo().getUriTemplate() != null;
  }

  private static boolean isSubtypeOf(Clazz clazz, String uri) {
    for (Clazz current = clazz; current != null; current = current.getExtendsClass()) {
      if (uri.equals(current.getUri())
          || current.getInterfaces().stream().anyMatch(i -> uri.equals(i.getUri()))) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasUriAttribute(Clazz clazz) {
    return clazz.getAttributes().stream()
        .anyMatch(attribute -> "uri".equals(attribute.getName()) && attribute.getRange() == null);
  }

  private static String capitalize(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  /**
   * A JSON-LD getter/setter pair for a value with a {@code _datatype} companion: a plain string
   * without datatype, otherwise a typed literal {@code {"@value": ..., "@type": ...}}.
   */
  private void appendTypedLiteralAccessors(StringBuilder methods, Attribute value,
      Attribute datatype) {
    String jsonName = value.getPropertyInfo().getName();
    String valueField = value.getName();
    String datatypeField = datatype.getName();
    String accessor = capitalize(valueField) + "JsonLd";
    methods.append("\n");
    methods.append("\t/** ").append(jsonName)
        .append(" as JSON-LD: a typed literal when the datatype is set. */\n");
    methods.append("\t@jakarta.persistence.Transient\n");
    methods.append("\t@JsonProperty(\"").append(jsonName).append("\")\n");
    methods.append("\tpublic Object get").append(accessor).append("() {\n");
    methods.append("\t\tif (").append(valueField).append(" == null || ").append(datatypeField)
        .append(" == null) {\n");
    methods.append("\t\t\treturn ").append(valueField).append(";\n");
    methods.append("\t\t}\n");
    methods.append("\t\tjava.util.Map<String, Object> literal = new java.util.LinkedHashMap<>();\n");
    methods.append("\t\tliteral.put(\"@value\", ").append(valueField).append(");\n");
    methods.append("\t\tliteral.put(\"@type\", ").append(datatypeField).append(");\n");
    methods.append("\t\treturn literal;\n");
    methods.append("\t}\n\n");
    methods.append("\t@JsonSetter(\"").append(jsonName).append("\")\n");
    methods.append("\tpublic void set").append(accessor).append("(Object value) {\n");
    methods.append("\t\tif (value instanceof java.util.Map<?, ?> literal) {\n");
    methods.append("\t\t\tObject literalValue = literal.get(\"@value\");\n");
    methods.append("\t\t\tObject literalType = literal.get(\"@type\");\n");
    methods.append("\t\t\tthis.").append(valueField)
        .append(" = literalValue != null ? literalValue.toString() : null;\n");
    methods.append("\t\t\tthis.").append(datatypeField)
        .append(" = literalType != null ? literalType.toString() : null;\n");
    methods.append("\t\t} else {\n");
    methods.append("\t\t\tthis.").append(valueField)
        .append(" = value != null ? value.toString() : null;\n");
    methods.append("\t\t\tthis.").append(datatypeField).append(" = null;\n");
    methods.append("\t\t}\n");
    methods.append("\t}\n");
  }

  /** {@code @type} with the class IRI; read-only, so an incoming {@code @type} is ignored. */
  private void appendRdfTypeGetter(StringBuilder methods, Clazz clazz) {
    methods.append("\n");
    methods.append("\t@jakarta.persistence.Transient\n");
    methods.append("\t@JsonProperty(value = \"@type\", access = JsonProperty.Access.READ_ONLY)\n");
    methods.append("\tpublic String getRdfType() {\n");
    methods.append("\t\treturn \"").append(clazz.getUri()).append("\";\n");
    methods.append("\t}\n");
  }

  private void appendEnumFromUri(StringBuilder builder, String enumName) {
    builder.append("\n");
    builder.append("\t@JsonCreator\n");
    builder.append("\tpublic static ").append(enumName).append(" fromUri(String uri) {\n");
    builder.append("\t\tfor (").append(enumName).append(" value : values()) {\n");
    builder.append("\t\t\tif (value.uri.equals(uri)) {\n");
    builder.append("\t\t\t\treturn value;\n");
    builder.append("\t\t\t}\n");
    builder.append("\t\t}\n");
    builder.append("\t\tthrow new IllegalArgumentException(\"Unknown ").append(enumName)
        .append(" URI: \" + uri);\n");
    builder.append("\t}\n");
  }

  /** The {@code IriReferentie} interface and the Jackson serializer that writes it as its URI. */
  private void generateIriReferenceFiles() {
    String reference = "package " + getPackageName() + ";\n\n"
        + "/**\n"
        + " * Implemented by every generated class with a URI, so that a reference to it can be\n"
        + " * serialized as that URI (see " + IRI_REFERENCE_SERIALIZER + ").\n"
        + " **/\n"
        + "public interface " + IRI_REFERENCE + " {\n"
        + "\tString getUri();\n"
        + "}\n";
    saveToFile(IRI_REFERENCE + ".java", reference);

    String serializer = "package " + getPackageName() + ";\n\n"
        + "import com.fasterxml.jackson.core.JsonGenerator;\n"
        + "import com.fasterxml.jackson.databind.SerializerProvider;\n"
        + "import com.fasterxml.jackson.databind.ser.std.StdSerializer;\n"
        + "import java.io.IOException;\n\n"
        + "/**\n"
        + " * Serializes a reference to an entity as its URI instead of as a nested object.\n"
        + " **/\n"
        + "public class " + IRI_REFERENCE_SERIALIZER + " extends StdSerializer<Object> {\n\n"
        + "\tpublic " + IRI_REFERENCE_SERIALIZER + "() {\n"
        + "\t\tsuper(Object.class);\n"
        + "\t}\n\n"
        + "\t@Override\n"
        + "\tpublic void serialize(Object value, JsonGenerator gen, SerializerProvider provider)\n"
        + "\t\t\tthrows IOException {\n"
        + "\t\tif (value instanceof " + IRI_REFERENCE + " reference) {\n"
        + "\t\t\tgen.writeString(reference.getUri());\n"
        + "\t\t} else {\n"
        + "\t\t\tprovider.defaultSerializeValue(value, gen);\n"
        + "\t\t}\n"
        + "\t}\n"
        + "}\n";
    saveToFile(IRI_REFERENCE_SERIALIZER + ".java", serializer);
  }

  protected List<Pair<String, String>> getDependencies(Clazz clazz) {
    Set<Pair<String, String>> dependencies = new HashSet<>(clazz.getAttributes().stream()
        .map(attr -> getJavaType(attr.getDataType()))
        .filter(pair -> !pair.getLeft().startsWith("java.lang"))
        .toList());

    clazz.getAttributes().forEach(attr -> {
      if (attr.isUnionType()) {
        attr.getRangeClasses().forEach(rangeClazz -> {
          String mappedName = nameMapping.getOrDefault(rangeClazz.getName(), rangeClazz.getName());
          dependencies.add(new Pair<>(getPackageName(), mappedName));
        });
      }
    });

    // Add extend and implement dependencies
    if (clazz.getExtendsClass() != null) {
      dependencies.add(
          new Pair<>(getPackageName(), nameMapping.get(clazz.getExtendsClass().getName())));
    }
    clazz.getInterfaces()
        .forEach(i -> dependencies.add(new Pair<>(getPackageName(), nameMapping.get(i.getName()))));
    return dependencies.stream()
        .sorted(DEPENDENCY_ORDER)
        .toList();
  }

  protected void saveToFile(String fileName, String content) {
    try {
      Path outputPath = Paths.get(getBasePath(), fileName);
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, content);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save file: " + fileName, e);
    }
  }

  private void cleanupStaleJavaFiles() {
    Path outputPath = Paths.get(getBasePath());
    
    // Check if the output directory exists before attempting to walk it
    if (!Files.exists(outputPath)) {
      return;
    }
    
    try (Stream<Path> files = Files.walk(outputPath)) {
      files.filter(Files::isRegularFile)
           .filter(p -> p.getFileName().toString().endsWith(".java"))
           .forEach(p -> {
             String fileName = p.getFileName().toString();
             if (!expectedJavaFileNames.contains(fileName)) {
               try {
                 Files.delete(p);
               } catch (IOException ignored) {
               }
             }
           });
    } catch (IOException e) {
      throw new RuntimeException("Failed to cleanup stale files", e);
    }
  }

  protected Pair<String, String> getJavaType(DataType dataType) {
    return switch (dataType.getUri()) {
      case "http://www.w3.org/2001/XMLSchema#string" -> getJavaPackageAndClassName(String.class);
      case "http://www.w3.org/2001/XMLSchema#integer" -> getJavaPackageAndClassName(Integer.class);
      case "http://www.w3.org/2001/XMLSchema#decimal", "http://www.w3.org/2001/XMLSchema#double",
          "http://www.w3.org/2001/XMLSchema#float" -> getJavaPackageAndClassName(Double.class);
      case "http://www.w3.org/2001/XMLSchema#boolean" -> getJavaPackageAndClassName(Boolean.class);
      case "http://www.w3.org/2001/XMLSchema#date" -> getJavaPackageAndClassName(LocalDate.class);
      case "http://www.w3.org/2001/XMLSchema#dateTime" ->
          getJavaPackageAndClassName(generatorProperties.getDateTimeType() == DateTimeType.OFFSET
              ? OffsetDateTime.class : LocalDateTime.class);
      default -> {
        String name = nameMapping.get(dataType.getName());
        if (name == null) {
          name = dataType.getName();
        }
        if (name == null || name.isBlank()) {
          name = "Object";
        }
        String packageName = getPackageName();
        yield new Pair<>(packageName, name);
      }
    };
  }

  protected Pair<String, String> getJavaPackageAndClassName(Class<?> clazz) {
    String packageName = clazz.getPackageName();
    String className = clazz.getSimpleName();
    return new Pair<>(packageName, className);
  }

  /**
   * Checks whether the given column is a merge-join-tables discriminator column. Such columns
   * appear only in JOIN tables and their name matches the configured attribute name.
   */
  private boolean isMergeJoinTablesColumn(Table table, String columnName,
      String mergeJoinTableAttributeName) {
    return table.getTableType() == TableType.JOIN
        && columnName.equals(mergeJoinTableAttributeName);
  }

  private boolean shouldAppendRelationNameForSelfManyToMany(Clazz clazz, Table targetTable) {
    Table sourceTable = getTableByClazz(clazz, false);
    if (sourceTable == null || targetTable == null
        || !sourceTable.getName().equals(targetTable.getName())) {
      return false;
    }

    long selfRelationCount = clazz.getAttributes().stream()
        .filter(attribute -> {
          Table rangeTable = getTableByClazz(attribute.getRange(), false);
          return rangeTable != null && sourceTable.getName().equals(rangeTable.getName());
        })
        .map(attribute -> attribute.getName())
        .distinct()
        .count();
    return selfRelationCount > 1;
  }

}
