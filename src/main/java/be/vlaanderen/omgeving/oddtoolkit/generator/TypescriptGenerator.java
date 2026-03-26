package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.TypescriptGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.Cardinality;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public class TypescriptGenerator extends ClassGenerator {

  private static final Comparator<String> STRING_ORDER = Comparator.nullsLast(String::compareTo);

  private final Map<Clazz, String> fileNames = new HashMap<>();
  private final Map<String, String> nameMapping = new HashMap<>();

  private final TypescriptGeneratorProperties typescriptGeneratorProperties;

  public TypescriptGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      TypescriptGeneratorProperties typescriptGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
    this.typescriptGeneratorProperties = typescriptGeneratorProperties;
  }

  @Override
  public void run() {
    super.run();
    prepareFileNames();
    generateFile(getClasses(), null);
    generateFile(getInterfaces(), "interface");
    generateFile(getEnums(), "enum");
  }

  private String getBasePath() {
    return typescriptGeneratorProperties.getOutputDirectory();
  }

  protected void prepareFileNames() {
    // Prepare file names for all classes, interfaces and enums
    getClasses().forEach(
        clazz -> fileNames.put(clazz, clazz.getName().toLowerCase() + ".model.ts"));
    getInterfaces().forEach(
        clazz -> fileNames.put(clazz, clazz.getName().toLowerCase() + ".interface.ts"));
    getEnums().forEach(clazz -> fileNames.put(clazz, clazz.getName().toLowerCase() + ".enum.ts"));
    // Prepare name mapping for all classes, interfaces and enums
    getClasses().forEach(clazz -> nameMapping.put(clazz.getName(), clazz.getName()));
    getInterfaces().forEach(clazz -> nameMapping.put(clazz.getName(), clazz.getName()));
    getEnums().forEach(clazz -> nameMapping.put(clazz.getName(), clazz.getName()));
  }

  protected void generateFile(List<? extends Clazz> classes, @Nullable String type) {
    classes.forEach(clazz -> {
      String typeDeclaration = "class";
      String originalName = clazz.getName();

      switch (type) {
        case "interface" -> {
          clazz.setName(clazz.getName());
          typeDeclaration = "interface";
        }
        case "enum" -> typeDeclaration = "enum";
        case null -> {
          // Default case for classes
        }
        default -> throw new IllegalStateException("Unexpected value: " + type);
      }

      String fileName = fileNames.get(clazz);
      if (fileName == null) {
        // Try to find by original name
        for (Map.Entry<Clazz, String> entry : fileNames.entrySet()) {
          if (entry.getKey().getName().equals(originalName)) {
            fileName = entry.getValue();
            break;
          }
        }
      }

      final String finalFileName = fileName;
      StringBuilder builder = new StringBuilder();

      boolean isInterface = clazz instanceof Interface;
      boolean isEnum = clazz instanceof Enum;

      // Begin with imports
      Set<String> imports = new HashSet<>();

      // Add typed-json imports if not enum
      if (!isEnum) {
        boolean hasProperties = !clazz.getAttributes().isEmpty();
        if (hasProperties && !isInterface) {
          imports.add("import { jsonObject, jsonMember, jsonArrayMember } from 'typedjson';");
        }
      }

      // Add dependency imports
      getDependencies(clazz).forEach(dep -> {
        // Find the file name for this dependency
        Clazz depClazz = null;

        // Search in classes
        for (Clazz c : getClasses()) {
          String mappedName = nameMapping.get(c.getName());
          if (c.getName().equals(dep) || (mappedName != null && mappedName.equals(dep))) {
            depClazz = c;
            break;
          }
        }

        // Search in interfaces if not found
        if (depClazz == null) {
          for (Interface i : getInterfaces()) {
            String mappedName = nameMapping.get(i.getName());
            if ((i.getName()).equals(dep) || (mappedName != null && mappedName.equals(dep))) {
              depClazz = i;
              break;
            }
          }
        }

        // Search in enums if not found
        if (depClazz == null) {
          for (Enum e : getEnums()) {
            String mappedName = nameMapping.get(e.getName());
            if (e.getName().equals(dep) || (mappedName != null && mappedName.equals(dep))) {
              depClazz = e;
              break;
            }
          }
        }

        if (depClazz != null) {
          String depFileName = fileNames.get(depClazz);
          if (depFileName != null && finalFileName != null && !finalFileName.equals(depFileName)) {
            String depFileNameWithoutExt = depFileName.replace(".ts", "");
            imports.add("import { " + dep + " } from './" + depFileNameWithoutExt + "';");
          }
        }
      });

      // Write imports
      imports.stream()
          .sorted(STRING_ORDER)
          .forEach(imp -> builder.append(imp).append("\n"));
      if (!imports.isEmpty()) {
        builder.append("\n");
      }

      // Add JSDoc comments
      builder.append("/**\n");
      builder.append(" * ").append(clazz.getName()).append("\n");
      builder.append(" * @see {@link ").append(clazz.getUri()).append("}\n");
      if (clazz.getClassInfo().getComment() != null) {
        builder.append(" * ").append(clazz.getClassInfo().getComment()).append("\n");
      }
      builder.append(" */\n");

      // Add decorators for classes (not interfaces or enums)
      if (!isInterface && !isEnum) {
        builder.append("@jsonObject\n");
      }

      // Determine if it extends or implements other classes/interfaces
      String extendsClause = "";
      if (clazz.getExtendsClass() != null) {
        String extendsName = nameMapping.get(clazz.getExtendsClass().getName());
        if (extendsName != null) {
          extendsClause = " extends " + extendsName;
        }
      }
      String implementsClause = "";
      if (!clazz.getInterfaces().isEmpty()) {
        implementsClause = " implements " + clazz.getInterfaces().stream()
            .map(i -> nameMapping.get(i.getName()))
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
      }

      builder.append("export ").append(typeDeclaration).append(" ").append(clazz.getName())
          .append(extendsClause).append(implementsClause)
          .append(" {\n");

      if (clazz instanceof Enum enumClazz) {
        // For enums, we can add the enum values as constants
        List<EnumValue> values = enumClazz.getValues();
        for (int i = 0; i < values.size(); i++) {
          EnumValue value = values.get(i);
          // Add JSDoc comment with URI
          builder.append("\t").append(value.getName()).append(" = '").append(value.getUri())
              .append("'");
          if (i < values.size() - 1) {
            builder.append(",");
          }
          builder.append("\n");
        }
      } else {
        clazz.getAttributes().forEach(prop -> {
          // Add JSDoc comments for the property
          builder.append("\t/**\n");
          builder.append("\t * ").append(prop.getPropertyInfo().getName()).append("\n");
          builder.append("\t * @see {@link ").append(prop.getUri()).append("}\n");
          if (prop.getPropertyInfo().getComment() != null) {
            builder.append("\t * ").append(prop.getPropertyInfo().getComment()).append("\n");
          }
          builder.append("\t */\n");

          boolean isArray = prop.getCardinality().isToMany();
          TypeScriptType tsType = getTypeScriptType(prop.getDataType());
          String dataType = tsType.typeName() + (isArray ? "[]" : "");

          // Check if property is optional (not required)
          boolean isOptional = !prop.getCardinality().equals(Cardinality.ONE_TO_ONE) &&
              !prop.getCardinality().equals(Cardinality.ONE_TO_MANY) && !prop.isPrimaryKey();

          if (isInterface) {
            // Interface: just declare the property
            builder.append("\t").append(prop.getName());
            if (isOptional) {
              builder.append("?");
            }
            builder.append(": ").append(dataType).append(";\n");
          } else {
            // Class: add @jsonMember or @jsonArrayMember decorator and property
            if (isArray) {
              // Use @jsonArrayMember for arrays
              builder.append("\t@jsonArrayMember(() => ").append(tsType.jsonType())
                  .append(", { name: '")
                  .append(prop.getPropertyInfo().getName()).append("'").append(" })\n");
            } else if (tsType.needsConstructor) {
              // Use @jsonMember for single values
              builder.append("\t@jsonMember(").append("() => ").append(tsType.jsonType()).append(", { name: '")
                  .append(prop.getPropertyInfo().getName()).append("'").append(" })\n");
            } else {
              // Use @jsonMember for single values
              builder.append("\t@jsonMember(").append("{ name: '")
                  .append(prop.getPropertyInfo().getName()).append("'").append(" })\n");
            }
            builder.append("\t").append(prop.getName());
            if (isOptional) {
              builder.append("?");
            }
            builder.append(": ").append(dataType).append(";\n");
          }
          builder.append("\n");
        });
      }
      builder.append("}\n");

      // Save to file
      if (finalFileName != null) {
        saveToFile(finalFileName, builder.toString());
      }
    });
  }


  protected List<String> getDependencies(Clazz clazz) {
    // For TypeScript, we determine dependencies based on the data types of the attributes
    Set<String> dependencies = new HashSet<>();

    clazz.getAttributes().stream()
        .map(attr -> getTypeScriptType(attr.getDataType()))
        .filter(TypeScriptType::isCustomType)
        .map(TypeScriptType::typeName)
        .forEach(dependencies::add);

    // Add extend and implement dependencies
    if (clazz.getExtendsClass() != null) {
      String extendsDep = nameMapping.get(clazz.getExtendsClass().getName());
      if (extendsDep != null) {
        dependencies.add(extendsDep);
      }
    }

    clazz.getInterfaces().forEach(i -> {
      String interfaceDep = nameMapping.get(i.getName());
      if (interfaceDep != null) {
        dependencies.add(interfaceDep);
      }
    });

    return dependencies.stream()
        .sorted(STRING_ORDER)
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

  protected TypeScriptType getTypeScriptType(DataType dataType) {
    // Map XSD types to TypeScript types
    return switch (dataType.getUri()) {
      case "http://www.w3.org/2001/XMLSchema#string" -> new TypeScriptType("string", false);
      case "http://www.w3.org/2001/XMLSchema#integer" -> new TypeScriptType("number", false);
      case "http://www.w3.org/2001/XMLSchema#decimal",
           "http://www.w3.org/2001/XMLSchema#double",
           "http://www.w3.org/2001/XMLSchema#float" -> new TypeScriptType("number", false);
      case "http://www.w3.org/2001/XMLSchema#boolean" -> new TypeScriptType("boolean", false);
      case "http://www.w3.org/2001/XMLSchema#date",
           "http://www.w3.org/2001/XMLSchema#dateTime" -> new TypeScriptType("Date", true);
      default -> {
        // Custom type - use the mapped name
        final String name = nameMapping.containsKey(dataType.getName()) ? nameMapping.get(dataType.getName())
              : dataType.getName();
        boolean isEnum = getEnums().stream().anyMatch(e -> e.getName().equals(name));
        yield new TypeScriptType(name, isEnum ? "String" : name, true);
      }
    };
  }

  /**
   * Record to represent TypeScript type information
   *
   * @param typeName         the TypeScript type name
   * @param jsonType         the type to use in @jsonMember (e.g. for Date it should be 'Date' even if the property type is string)
   * @param needsConstructor whether this type needs a constructor in @jsonMember
   */
  protected record TypeScriptType(String typeName, String jsonType, boolean needsConstructor) {

    public TypeScriptType(String typeName, boolean needsConstructor) {
      this(typeName, null, needsConstructor);
    }

    public TypeScriptType {
      if (jsonType == null || jsonType.isBlank()) {
        jsonType = typeName;
      }
    }

    public boolean isCustomType() {
      return needsConstructor &&
          !typeName.equals("Date") &&
          !typeName.equalsIgnoreCase("string") &&
          !typeName.equalsIgnoreCase("number") &&
          !typeName.equalsIgnoreCase("boolean");
    }
  }
}
