package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.ClassDiagramProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassDiagramGenerator extends DiagramGenerator {
  private static final Logger logger = LoggerFactory.getLogger(
      ClassDiagramGenerator.class);

  private final ClassDiagramProperties generatorProperties;

  public ClassDiagramGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      ClassDiagramProperties generatorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties);
    this.generatorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    String classDiagram = generate("classDiagram");
    if (getOutputFile() != null) {
      logger.info("Writing class diagram to {}", getOutputFile());
      saveDiagram(classDiagram);
    } else {
      System.out.println(classDiagram);
    }
  }

  @Override
  protected void renderContent(StringBuilder builder, String type) {
    for (Clazz classInfo : getClasses()) {
      generateClass(builder, classInfo, ClassType.CLASS);
    }
    for (Interface interfaceInfo : getInterfaces()) {
      generateClass(builder, interfaceInfo, ClassType.INTERFACE);
    }
    for (Enum enumInfo : getEnums()) {
      generateClass(builder, enumInfo, ClassType.ENUM);
    }
    emitStyleDefinitions(builder);
  }

  protected void generateClass(StringBuilder builder, Clazz classInfo, ClassType type) {
    builder.append("%% ").append(classInfo.getUri()).append("\n");
    String style = getStyleForClass(classInfo.getClassInfo());
    builder.append("class ").append(classInfo.getName());
    if (style != null) {
      builder.append(":::").append(style);
    }
    builder.append(" {\n");
    if (type != ClassType.CLASS) {
      builder.append("  <<").append(type.getValue()).append(">>\n");
    }
    if (type == ClassType.ENUM) {
      List<EnumValue> enumValues = getEnum(classInfo.getClassInfo()).getValues();
      for (EnumValue enumValue : enumValues) {
        builder.append("  ").append(enumValue.getName()).append("\n");
      }
    } else {
      for (Attribute attribute : classInfo.getAttributes()) {
        generateProperty(builder, attribute);
      }
    }
    builder.append("}\n");
    generateRelations(builder, classInfo);
  }

  protected void generateRelations(StringBuilder builder, Clazz classInfo) {
    for (Attribute attribute : classInfo.getAttributes()) {
      if (attribute.getRange() != null) {
        Clazz domainClass = attribute.getRange();
        builder.append("%% ").append(attribute.getPropertyInfo().getUri()).append("\n");
        builder.append(classInfo.getName()).append(" --> ")
            .append(domainClass.getName())
            .append(" : ").append(attribute.getName()).append("\n");
      }
    }
    for (Interface superInterface : classInfo.getInterfaces()) {
      builder.append(superInterface.getName()).append(" <|-- ")
          .append(classInfo.getName()).append("\n");
    }
    if (classInfo.getExtendsClass() != null) {
      builder.append(classInfo.getExtendsClass().getName()).append(" <|-- ")
          .append(classInfo.getName()).append("\n");
    }
  }

  protected void generateProperty(StringBuilder builder, Attribute propertyInfo) {
    String dataTypeName = propertyInfo.getDataType().getName();
    if (propertyInfo.getCardinality().isToMany()) {
      dataTypeName += "[]";
    }
    if (propertyInfo.isPrimaryKey()) {
      dataTypeName = "+" + dataTypeName;
    }

    builder.append("  ").append(dataTypeName).append(" ").append(propertyInfo.getName()).append("\n");
  }
}
