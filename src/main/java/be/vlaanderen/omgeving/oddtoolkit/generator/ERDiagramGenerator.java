package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.ERDiagramProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.SchemaGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.Cardinality;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.util.List;
import lombok.Getter;

@Getter
public class ERDiagramGenerator extends SchemaGenerator {

  private final ERDiagramProperties generatorProperties;

  public ERDiagramGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      ERDiagramProperties generatorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties, schemaGeneratorProperties);
    this.generatorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    String diagram = generate("erDiagram");
    if (getOutputFile() != null) {
      saveDiagram(diagram);
    } else {
      System.out.println(diagram);
    }
  }

  @Override
  protected String getOutputFile() {
    return generatorProperties != null ? generatorProperties.getOutputFile() : null;
  }

  @Override
  protected void renderContent(StringBuilder builder, String type) {
    builder.append("direction LR\n\n");
    generateTables(builder);
    emitStyleDefinitions(builder);
  }

  private void generateTables(StringBuilder builder) {
    getTables().forEach(table -> {
      builder.append("%% ").append(table.getUri()).append("\n");
      builder.append(table.getName()).append(" {\n");
      table.getColumns().forEach(column -> {
        builder.append("  ").append(column.getName()).append(" ").append(column.getDataType());
        builder.append(" ");
        boolean hasFlag = false;
        if (column.isPrimaryKey()) {
          builder.append("PK");
          hasFlag = true;
        }
        if (column.isForeignKey()) {
          if (hasFlag) {
            builder.append(",");
          }
          builder.append("FK");
        }
        builder.append("\n");
      });
      builder.append("}\n\n");
      if (table.getDiagramStyle() != null) {
        builder.append("class ").append(table.getName()).append(" ").append(table.getDiagramStyle()).append("\n");
      }
      generateRelations(builder, table);
    });
  }

  private void generateRelations(StringBuilder builder, Table table) {
    table.getRelations().forEach(relation -> {
      PropertyInfo relationProperty = (PropertyInfo) relation.getFromColumn().getPropertyInfo();
      Cardinality cardinality = relation.getCardinality();
      if (relationProperty != null) {
        builder.append("%% ").append(relation.getFromColumn().getPropertyInfo().getUri())
            .append("\n");
      }
      String cardinalityString = cardinality.name();
      String cardinalityFromString = cardinalityString.split("_TO_")[0].toLowerCase();
      String cardinalityToString = cardinalityString.split("_TO_")[1].toLowerCase();
      builder.append(table.getName()).append(" ").append(cardinalityFromString).append(" to ")
          .append(cardinalityToString).append(" ").append(relation.getTo().getName());
      builder.append(" : ").append(relation.getName() != null ? relation.getName() : "\"\"")
          .append("\n\n");
    });
  }
}
