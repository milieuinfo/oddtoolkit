package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.SQLGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.SchemaGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;

@Getter
public class SQLGenerator extends SchemaGenerator {

  private static final Comparator<Table> TABLE_ORDER = Comparator
      .comparing((Table t) -> t != null ? t.getName() : null, Comparator.nullsLast(String::compareTo));

  private static final Comparator<Relation> RELATION_ORDER = Comparator
      .comparing((Relation r) -> r != null && r.getFromColumn() != null ? r.getFromColumn().getName() : null,
          Comparator.nullsLast(String::compareTo))
      .thenComparing(r -> r != null && r.getTo() != null ? r.getTo().getName() : null,
          Comparator.nullsLast(String::compareTo));

  private static final Comparator<Enum> ENUM_ORDER = Comparator
      .comparing((Enum e) -> e != null ? e.getName() : null, Comparator.nullsLast(String::compareTo));

  private static final Comparator<EnumValue> ENUM_VALUE_ORDER = Comparator
      .comparing((EnumValue v) -> v != null ? v.getName() : null, Comparator.nullsLast(String::compareTo));

  private final SQLGeneratorProperties sqlGeneratorProperties;

  public SQLGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      SQLGeneratorProperties generatorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties,
        schemaGeneratorProperties);
    this.sqlGeneratorProperties = generatorProperties;
  }

  @Override
  public String getName() {
    return "sql";
  }

  @Override
  public String getDescription() {
    return "Generates SQL schema from the ontology";
  }

  @Override
  public void run() {
    super.run();
    String generated = generateSQL();
    if (getOutputFile() != null) {
      saveToFile(getOutputFile(), generated);
    } else {
      System.out.println(generated);
    }
  }

  private String generateSQL() {
    StringBuilder sb = new StringBuilder();
    sb.append("-- Auto-generated SQL schema from ODDToolkit\n");
    sb.append("-- Ontology: ").append(ontologyInfo.getUri()).append("\n");
    sb.append("-- Generated: ").append(java.time.ZonedDateTime.now()).append("\n\n");
    generateEnumTypes(sb);
    generateTables(sb);
    return sb.toString();
  }

  private void generateTables(StringBuilder sb) {
    getTables().stream()
        .sorted(TABLE_ORDER)
        .forEach(table -> {
      sb.append("-- ").append(table.getUri()).append("\n");
      if (table.getTableType() != TableType.REGULAR) {
        sb.append("-- ").append("Table type: ").append(table.getTableType()).append("\n");
        if (table.getTableType() == TableType.JOIN) {
          Relation relation = table.getRelationByAttribute(table.getColumns().getFirst());
          sb.append("-- ").append("Original relation: ").append(relation.getName()).append("\n");
        }
      }
      sb.append("CREATE TABLE ").append(table.getName()).append(" (\n");
      int i = 0;
      for (Column column : table.getColumns()) {
        if (i++ > 0) {
          sb.append(",\n");
        }
        if (column.isForeignKey()) {
          Relation relation = table.getRelationByAttribute(column);
          sb.append("  -- ").append("Foreign key referencing ")
              .append(relation.getTo().getName()).append("(").append(relation.getToColumn().getName())
              .append(")").append("\n");
        }
        sb.append("  ").append(column.getName()).append(" ").append(column.getDataType());
      }
      // Create constraints for primary keys and foreign keys
      List<Column> primaryKeys = table.getColumns().stream().filter(Column::isPrimaryKey).toList();
      if (!primaryKeys.isEmpty()) {
        sb.append(",\n  PRIMARY KEY (");
        int j = 0;
        for (Column pk : primaryKeys) {
          if (j++ > 0) {
            sb.append(", ");
          }
          sb.append(pk.getName());
        }
        sb.append(")");
      }
      sb.append("\n);\n\n");

      // Create comments ON TABLE and ON COLUMN for documentation
      sb.append("COMMENT ON TABLE ").append(table.getName()).append(" IS '")
          .append(table.getUri()).append("';\n");
      for (Column column : table.getColumns()) {
        if (column.getPropertyInfo() != null) {
          sb.append("COMMENT ON COLUMN ").append(table.getName()).append(".").append(column.getName())
              .append(" IS '").append(column.getPropertyInfo().getUri()).append("';\n");
        }
      }
      sb.append("\n");
      sb.append("----------------------------------------------------------------------\n\n");
    });

    sb.append("-- Foreign key constraints\n\n");

    // Create foreign key constraints after all tables are created to avoid referencing tables that are not yet defined
    getTables().stream()
        .sorted(TABLE_ORDER)
        .forEach(table -> {
      List<Column> foreignKeys = table.getColumns().stream().filter(Column::isForeignKey).toList();
      for (Column fk : foreignKeys) {
        table.getRelations()
            .stream()
            .filter(relation -> relation.getFromColumn().equals(fk))
            .sorted(RELATION_ORDER)
            .forEach(relation -> sb.append("ALTER TABLE ").append(table.getName())
              .append(" ADD FOREIGN KEY (").append(fk.getName()).append(") REFERENCES ")
              .append(relation.getTo().getName()).append("(")
              .append(relation.getToColumn().getName()).append(");\n"));
      }
    });
  }

  private void generateEnumTypes(StringBuilder sb) {
    getSchemaEnums().stream()
        .sorted(ENUM_ORDER)
        .forEach(type -> {
          if (type.getClassInfo() != null) {
            sb.append("-- ").append(type.getUri()).append("\n");
          }
          sb.append("CREATE TYPE ").append(type.getName()).append(" AS ENUM (\n");
          int i = 0;
          for (EnumValue value : type.getValues().stream().sorted(ENUM_VALUE_ORDER).toList()) {
            if (i++ > 0) {
              sb.append(",\n");
            }
            sb.append("  '").append(value).append("'");
          }
          sb.append("\n);\n\n");
        });
  }

  @Override
  protected String getOutputFile() {
    return sqlGeneratorProperties != null ? sqlGeneratorProperties.getOutputFile() : null;
  }
}
