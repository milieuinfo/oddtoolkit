package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.JavaGeneratorProperties;
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
  }

  private String getPackageName() {
    return generatorProperties.getPackageName();
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
        if (hasProperties) {
          builder.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
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

      builder.append("public ").append(typeDeclaration).append(" ").append(clazz.getName())
          .append(extendsClause).append(implementsClause)
          .append(" {\n");
      if (clazz instanceof Enum enumClazz) {
        enumClazz.getValues().forEach(value -> {
          builder.append("\t").append(value.getName()).append(" = \"").append(value.getUri()).append("\"").append(",\n");
        });
      }
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

          builder.append("\t@JsonProperty(\"").append(prop.getPropertyInfo().getName())
              .append("\")\n");

          builder.append("\t").append("private ")
              .append(dataType)
              .append(" ").append(prop.getName()).append(";\n");
        }
      });

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
      builder.append("\t@JsonProperty(\"").append(jsonName).append("\")\n");
      builder.append("\tprivate List<").append(targetType).append("> ")
          .append(fieldName).append(";\n");
    }
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
          getJavaPackageAndClassName(LocalDateTime.class);
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
