package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration;
import be.vlaanderen.omgeving.oddtoolkit.config.OntologyConfiguration.ExtraProperty;
import be.vlaanderen.omgeving.oddtoolkit.config.SchemaGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.Cardinality;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.PropertyInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.atlas.lib.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public abstract class SchemaGenerator extends DiagramGenerator {
  private static final Logger logger = LoggerFactory.getLogger(SchemaGenerator.class);
  private static final String DEFAULT_IDENTIFIER_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#id";
  private static final String DEFAULT_JOIN_TABLE_NAME_PATTERN = "rel_{source_table}_{target_table}";
  private static final String SOURCE_TABLE_PLACEHOLDER = "{source_table}";
  private static final String TARGET_TABLE_PLACEHOLDER = "{target_table}";
  private static final String RELATION_NAME_PLACEHOLDER = "{relation_name}";
  private static final String COLUMN_PLACEHOLDER = "{column}";

  private final List<Enum> schemaEnums = new ArrayList<>();
  private final List<Table> tables = new ArrayList<>();
  private final OntologyConfiguration ontologyConfiguration;
  private final SchemaGeneratorProperties schemaGeneratorProperties;

  public SchemaGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo, List<AbstractAdapter<?>> adapters,
      DiagramGeneratorProperties diagramGeneratorProperties,
      SchemaGeneratorProperties generatorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties);
    this.ontologyConfiguration = ontologyInfo.getConfig();
    this.schemaGeneratorProperties = generatorProperties;
  }

  @Override
  public void run() {
    super.run();
    schemaEnums.clear();
    tables.clear();
    updateEnums();
    extractTables();
    extractTableRelations();
    stabilizeSchemaOrdering();
    validateSchema();
  }

  /**
   * Validates the generated tables before any output is written, so structural problems
   * (e.g. two columns ending up with the same name) are reported with the offending table
   * and likely cause instead of being silently emitted as invalid SQL/code.
   *
   * @throws IllegalStateException describing every problem found, if any
   */
  protected void validateSchema() {
    List<String> issues = new ArrayList<>();
    for (Table table : tables) {
      Map<String, List<String>> columnsByNormalizedName = new LinkedHashMap<>();
      for (Column column : table.getColumns()) {
        String normalizedName = column.getName() == null ? "" : column.getName().toLowerCase();
        columnsByNormalizedName.computeIfAbsent(normalizedName, k -> new ArrayList<>())
            .add(column.getName());
      }
      columnsByNormalizedName.forEach((normalizedName, columnNames) -> {
        if (columnNames.size() > 1) {
          String cause = table.getTableType() == TableType.JOIN
              ? " This commonly happens on join tables for self-referencing relations where "
                  + "the source/target column name patterns resolve to the same name."
              : "";
          issues.add("table '" + table.getName() + "' has " + columnNames.size()
              + " columns named '" + columnNames.getFirst() + "'." + cause);
        }
      });
    }
    if (!issues.isEmpty()) {
      String message = "Schema generation produced an invalid schema:\n  - "
          + String.join("\n  - ", issues);
      logger.error(message);
      throw new IllegalStateException(message);
    }
  }

  private void stabilizeSchemaOrdering() {
    Comparator<Table> tableOrder = Comparator
        .comparing((Table table) -> table != null ? table.getName() : null,
            Comparator.nullsLast(String::compareTo));
    Comparator<Relation> relationOrder = Comparator
        .comparing((Relation relation) -> relation != null ? relation.getName() : null,
            Comparator.nullsLast(String::compareTo))
        .thenComparing(relation -> relation != null && relation.getTo() != null ? relation.getTo().getName() : null,
            Comparator.nullsLast(String::compareTo));
    Comparator<Enum> enumOrder = Comparator
        .comparing((Enum value) -> value != null ? value.getName() : null,
            Comparator.nullsLast(String::compareTo));
    Comparator<EnumValue> enumValueOrder = Comparator
        .comparing((EnumValue value) -> value != null ? value.getName() : null,
            Comparator.nullsLast(String::compareTo));

    schemaEnums.sort(enumOrder);
    schemaEnums.forEach(e -> e.setValues(e.getValues().stream().sorted(enumValueOrder).toList()));

    tables.sort(tableOrder);
    tables.forEach(table -> table.setRelations(table.getRelations().stream().sorted(relationOrder).toList()));
  }

  private void updateEnums() {
    this.schemaEnums.addAll(getEnums().stream()
        .filter(e -> e.getName() != null)
        .map(Enum::copy)
        .toList());
    this.schemaEnums
        .forEach(e -> e.setName(toSnakeCase(e.getName()).toLowerCase()));
  }

  private List<String> getIdentifierColumnUris() {
    LinkedHashSet<String> identifierUris = new LinkedHashSet<>();
    ontologyConfiguration.getExtraProperties().stream()
        .filter(ExtraProperty::isIdentifier)
        .map(ExtraProperty::getUri)
        .map(SchemaGenerator::normalizeUri)
        .filter(uri -> uri != null)
        .forEach(identifierUris::add);
    ontologyConfiguration.getOverrideProperties().stream()
        .filter(overrideProperty -> overrideProperty != null
            && Boolean.TRUE.equals(overrideProperty.getIdentifier()))
        .map(OntologyConfiguration.OverrideProperty::getUri)
        .map(SchemaGenerator::normalizeUri)
        .filter(uri -> uri != null)
        .forEach(identifierUris::add);
    if (identifierUris.isEmpty()) {
      identifierUris.add(DEFAULT_IDENTIFIER_URI);
    }
    return List.copyOf(identifierUris);
  }

  private Column findIdentifierColumn(Table table, boolean preferNonForeignKey) {
    if (table == null) {
      return null;
    }

    for (String identifierUri : getIdentifierColumnUris()) {
      Column column = table.getColumnByUri(identifierUri);
      if (column != null && (!preferNonForeignKey || !column.isForeignKey())) {
        return column;
      }
    }
    if (preferNonForeignKey) {
      for (String identifierUri : getIdentifierColumnUris()) {
        Column column = table.getColumnByUri(identifierUri);
        if (column != null) {
          return column;
        }
      }
    }

    List<Column> primaryKeys = table.getColumns().stream()
        .filter(Column::isPrimaryKey)
        .toList();
    List<Column> preferredPrimaryKeys = preferNonForeignKey
        ? primaryKeys.stream().filter(column -> !column.isForeignKey()).toList()
        : primaryKeys;

    if (preferredPrimaryKeys.size() == 1) {
      return preferredPrimaryKeys.getFirst();
    }
    if (preferNonForeignKey && preferredPrimaryKeys.isEmpty() && primaryKeys.size() == 1) {
      return primaryKeys.getFirst();
    }
    return null;
  }

  private Column resolveIdentifierColumn(Table table, String context) {
    Column identifierColumn = findIdentifierColumn(table, false);
    if (identifierColumn != null) {
      return identifierColumn;
    }
    throw new IllegalStateException(
        "Cannot determine identifier column for table '" + table.getName() + "' while "
            + context
            + ". Configure an identifier via ontology.extra-properties or ontology.override-properties, or ensure the table has a single primary key.");
  }

  private static String normalizeUri(String uri) {
    if (uri == null) {
      return null;
    }
    String normalized = uri.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private String getIdentifierColumnUri() {
    return this.ontologyConfiguration.getExtraProperties()
        .stream()
        .filter(ExtraProperty::isIdentifier)
        .findFirst()
        .map(ExtraProperty::getUri)
        .orElse("http://www.w3.org/1999/02/22-rdf-syntax-ns#id");
  }

  protected void extractTableRelations() {
    // Extract inheritance relations and tables
    tables.forEach(this::extractInheritance);
    // Extract identity tables if enabled
    if (schemaGeneratorProperties.getIdentityTables().isEnabled()) {
      extractIdentityTables();
    }
    // Extract column relations (after inheritance to ensure correct names)
    tables.forEach(this::extractColumnRelations);

    // Create join tables for many-to-many relations
    new ArrayList<>(tables).forEach(this::extractManyToManyRelations);

    if (schemaGeneratorProperties.getIdentityTables().isEnabled()) {
      updateIdentifierReferences();
    }
  }

  private void updateIdentifierReferences() {
    // Relations pointing at a class with a composite key must reference its identity table
    // instead, since that table (not the composite-keyed one) holds the stable identifier.
    // Outgoing relations from the composite-keyed table are left untouched: the referencing
    // column physically lives on that table, not on the identity table, so there is nothing to
    // redirect there.
    tables.stream().filter(t -> t.getExtendsClass() != null
            && ((Table) t.getExtendsClass()).getTableType() == TableType.IDENTITY)
        .forEach(table -> tables.forEach(otherTable -> otherTable.getRelations().forEach(relation -> {
          if (relation.getTo().equals(table)) {
            Table parentTable = (Table) table.getExtendsClass();
            Column parentIdentifierColumn = findIdentifierColumn(parentTable, false);
            if (parentIdentifierColumn == null) {
              return;
            }
            relation.setTo(parentTable);
            relation.setToColumn(parentIdentifierColumn);
          }
        })));
  }

  private void extractStyles(Table table) {
    // Get style for the table
    String style = getStyleForClass(table.getClassInfo());
    table.setDiagramStyle(style);
  }

  private void extractTables() {
    this.classes
        .forEach(concreteClass -> addTable(new Table(concreteClass)));
    this.interfaces
        .forEach(interfaceClass -> addTable(new Table(interfaceClass)));
    tables.forEach(this::extractStyles);
    tables.sort(Comparator.comparingInt(t -> -t.getClassInfo().getSuperClasses().size()));
  }

  private void extractIdentityTables() {
    // Create identity tables if a class has a composite identifier
    List<Table> identityTables = new ArrayList<>();
    tables.forEach(table -> {
      int primaryKeys = table.getColumns().stream().filter(Column::isPrimaryKey).toList().size();
      // Only create a table if pks > 1 and there is no super class with an identity table
      if (primaryKeys > 1) {
        Table identityTable = new Table(table);
        identityTable.setDiagramStyle(table.getDiagramStyle());
        identityTable.setTableType(TableType.IDENTITY);
        identityTable.setName(table.getName() + "_" + schemaGeneratorProperties.getIdentityTables()
            .getTableNameSuffix());
        identityTable.setColumns(new ArrayList<>());
        // Add the identifier column to the identity table
        Column identifierColumn = resolveIdentifierColumn(table,
            "creating an identity table for '" + table.getName() + "'");
        Column identityColumnCopy = identifierColumn.copy();
        identityColumnCopy.setDomain(identityTable);
        identityTable.addColumn(identityColumnCopy);
        identityTables.add(identityTable);
        // Update the original table to reference the identity table
        identifierColumn.setRange(identityTable);
        identifierColumn.setForeignKey(true);
        // Move extends relation to the identity table
        identityTable.setExtendsClass(table.getExtendsClass());
        table.setExtendsClass(identityTable);
        // Copy identifier column relations
        List<Relation> relationsToUpdate = new ArrayList<>();
        table.getRelations().forEach(relation -> {
          if (relation.getFromColumn().equals(identifierColumn)) {
            relationsToUpdate.add(relation);
          }
        });
        // Delete from original table and add to identity table
        relationsToUpdate.forEach(relation -> table.getRelations().remove(relation));
        identityTable.getRelations().addAll(relationsToUpdate.stream().peek(rel -> {
          rel.setFromColumn(identityColumnCopy);
          rel.setFrom(identityTable);
        }).toList());
        // Create a relation to the identity table
        Relation relation = new Relation();
        relation.setFrom(table);
        relation.setTo(identityTable);
        relation.setFromColumn(identifierColumn);
        relation.setToColumn(identityColumnCopy);
        relation.setCardinality(Cardinality.MANY_TO_ONE);
        table.getRelations().add(relation);
      }
    });
    identityTables.forEach(this::addTable);
    tables.sort(Comparator.comparingInt(t -> -t.getClassInfo().getSuperClasses().size()));
  }

  private String getJoinTableName(Relation relation, boolean appendRelationName) {
    Table tableFrom = getTableByClazz(relation.getFrom(), false);
    Table tableTo = getTableByClazz(relation.getTo(), false);
    String relationName = relation.getName() != null ? relation.getName() : "relation";
    return resolveJoinTableName(tableFrom.getName(), tableTo.getName(), relationName,
        appendRelationName);
  }

  protected String resolveJoinTableName(String sourceTableName, String targetTableName,
      String relationName, boolean appendRelationName) {
    String normalizedRelationName = normalizeJoinTableRelationName(relationName, targetTableName);

    String pattern = schemaGeneratorProperties.getJoinTableNamePattern();
    if (pattern == null || pattern.isBlank()) {
      pattern = DEFAULT_JOIN_TABLE_NAME_PATTERN;
    }
    boolean patternContainsRelationPlaceholder = pattern.contains(RELATION_NAME_PLACEHOLDER);
    String resolvedPattern = pattern
        .replace(SOURCE_TABLE_PLACEHOLDER, sourceTableName)
        .replace(TARGET_TABLE_PLACEHOLDER, targetTableName)
        .replace(RELATION_NAME_PLACEHOLDER, normalizedRelationName);

    if (appendRelationName && !patternContainsRelationPlaceholder) {
      resolvedPattern = resolvedPattern + "_" + normalizedRelationName;
    }

    return toSnakeCase(resolvedPattern).toLowerCase();
  }

  private String normalizeJoinTableRelationName(String relationName, String targetTableName) {
    String normalizedRelationName =
        relationName == null || relationName.isBlank() ? "relation" : relationName;
    String targetSuffix = "_" + targetTableName;
    if (normalizedRelationName.endsWith(targetSuffix)
        && normalizedRelationName.length() > targetSuffix.length()) {
      return normalizedRelationName.substring(0,
          normalizedRelationName.length() - targetSuffix.length());
    }
    return normalizedRelationName;
  }

  protected String resolveJoinTableName(String sourceTableName, String targetTableName,
      String relationName) {
    return resolveJoinTableName(sourceTableName, targetTableName, relationName, false);
  }

  protected String resolveJoinTableName(String sourceTableName, String targetTableName) {
    return resolveJoinTableName(sourceTableName, targetTableName, "relation", false);
  }

  /**
   * Resolves the column name inside a join table using the configured pattern.
   *
   * <p>Supported placeholders: {@code {source_table}}, {@code {target_table}}, {@code {column}}.
   *
   * @param pattern         the name pattern (e.g. {@code "source_{column}"} or
   *                        {@code "{source_table}_{column}"})
   * @param sourceTableName the name of the source (from) table
   * @param targetTableName the name of the target (to) table
   * @param columnName      the original identifier column name
   * @return the resolved, snake_cased column name
   */
  protected String resolveJoinColumnName(String pattern, String sourceTableName,
      String targetTableName, String columnName) {
    String resolvedPattern = (pattern == null || pattern.isBlank())
        ? "source_{column}"
        : pattern;
    return toSnakeCase(resolvedPattern
        .replace(SOURCE_TABLE_PLACEHOLDER, sourceTableName)
        .replace(TARGET_TABLE_PLACEHOLDER, targetTableName)
        .replace(COLUMN_PLACEHOLDER, columnName)).toLowerCase();
  }

  /**
   * Disambiguates a join column name that collides with the other join column of the same
   * relation. This happens for self-referencing relations (e.g. {@code Proces volgt_op Proces}),
   * where {@code {source_table}} and {@code {target_table}} resolve to the same table name and
   * therefore produce identical column names. The colliding name is qualified with the relation
   * name instead (e.g. {@code proces_id} -> {@code volgt_op_proces_id}).
   */
  protected String disambiguateSelfReferencingColumnName(String columnName, String otherColumnName,
      String relationName, String targetTableName) {
    if (!columnName.equals(otherColumnName)) {
      return columnName;
    }
    String relationQualifier = normalizeJoinTableRelationName(relationName, targetTableName);
    return toSnakeCase(relationQualifier + "_" + columnName).toLowerCase();
  }

  private boolean shouldAppendRelationNameForSelfRelation(Relation relation,
      List<Relation> relations) {
    if (relation.getFrom() == null || relation.getTo() == null
        || relation.getFrom().getClassInfo() == null || relation.getTo().getClassInfo() == null) {
      return false;
    }

    boolean selfRelation = relation.getFrom().getClassInfo().equals(relation.getTo().getClassInfo());
    if (!selfRelation) {
      return false;
    }

    String targetTableName = getTableByClazz(relation.getTo(), false).getName();
    long relationCountBetweenSameTables = relations.stream()
        .filter(r -> r.getFrom() != null && r.getTo() != null)
        .filter(r -> r.getFrom().getClassInfo() != null && r.getTo().getClassInfo() != null)
        .filter(r -> relation.getFrom().getClassInfo().equals(r.getFrom().getClassInfo())
            && relation.getTo().getClassInfo().equals(r.getTo().getClassInfo()))
        .map(r -> normalizeJoinTableRelationName(r.getName(), targetTableName))
        .distinct()
        .count();
    return relationCountBetweenSameTables > 1;
  }

  private Table createJoinTable(Relation relation, boolean appendRelationName) {
    Table joinTable = new Table();
    joinTable.setTableType(TableType.JOIN);
    joinTable.setDiagramStyle(relation.getFrom().getDiagramStyle());
    joinTable.setClassInfo(relation.getFrom().getClassInfo());
    joinTable.setName(getJoinTableName(relation, appendRelationName));
    // Add all primary key columns from both tables to the join table
    List<Column> joinColumns = new ArrayList<>();
    // Get identity columns if enabled
    if (schemaGeneratorProperties.getIdentityTables().isEnabled()) {
      Column fromIdentifierColumn = findIdentifierColumn(relation.getFrom(), false);
      Column toIdentifierColumn = findIdentifierColumn(relation.getTo(), false);
      if (fromIdentifierColumn == null || toIdentifierColumn == null) {
        return null;
      }

      Column fromIdentityColumn = fromIdentifierColumn.copy();
      fromIdentityColumn.setName(resolveJoinColumnName(
          schemaGeneratorProperties.getJoinTableColumns().getSourceColumnNamePattern(),
          relation.getFrom().getName(), relation.getTo().getName(),
          fromIdentifierColumn.getName()));
      Column toIdentityColumn = toIdentifierColumn.copy();
      toIdentityColumn.setName(disambiguateSelfReferencingColumnName(
          resolveJoinColumnName(
              schemaGeneratorProperties.getJoinTableColumns().getTargetColumnNamePattern(),
              relation.getFrom().getName(), relation.getTo().getName(),
              toIdentifierColumn.getName()),
          fromIdentityColumn.getName(), relation.getName(), relation.getTo().getName()));
      joinColumns.add(fromIdentityColumn);
      joinColumns.add(toIdentityColumn);

      // Set all columns as FK in the join table
      joinColumns.forEach(c -> c.setForeignKey(true));

      // Include one pair of temporal columns if both tables have them
      List<Column> temporalColumns = relation.getFrom().getColumns().stream()
          .filter(c -> ontologyConfiguration.getTemporalProperties().contains(c.getUri()))
          .map(Column::copy)
          .toList();
      if (!temporalColumns.isEmpty()) {
        joinColumns.addAll(temporalColumns);
      }

      // Add logical delete column
      Column logicalDeleteColumn = new Column();
      logicalDeleteColumn.setName("deleted");
      logicalDeleteColumn.setDataType(new DataType("BOOLEAN", "http://www.w3.org/2001/XMLSchema#boolean"));
      joinTable.addColumn(logicalDeleteColumn);

      joinTable.setColumns(joinColumns);
      // Create relations from original tables to join table
      createJoinTableRelation(relation.getName(), relation.getFrom(), joinTable,
          List.of(fromIdentityColumn));
      createJoinTableRelation(relation.getName(), relation.getTo(), joinTable,
          List.of(toIdentityColumn));
    } else {
      List<Column> leftColumns = relation.getFrom().getColumns().stream()
          .filter(Column::isPrimaryKey)
          .map(Column::copy)
          .peek(c -> c.setName(relation.getFrom().getName() + "_" + c.getName()))
          .toList();
      List<Column> rightColumns = relation.getTo().getColumns().stream()
          .filter(Column::isPrimaryKey)
          .map(Column::copy)
          .peek(c -> c.setName(relation.getTo().getName() + "_" + c.getName()))
          // If the left column has the same name as the right column, rename the right column to avoid conflicts
          .peek(c -> {
            if (leftColumns.stream().anyMatch(lc -> lc.getName().equals(c.getName()))) {
              c.setName(relation.getTo().getName() + "_" + c.getName());
            }
          })
          .toList();
      if (leftColumns.isEmpty() || rightColumns.isEmpty()) {
        throw new IllegalStateException(
            "Cannot create join table for relation " + relation.getName()
                + " because one of the tables does not have a primary key");
      }
      joinColumns.addAll(leftColumns);
      joinColumns.addAll(rightColumns);

      // Set all columns as FK in the join table
      joinColumns.forEach(c -> c.setForeignKey(true));
      // Create relations from original tables to join table
      createJoinTableRelation(relation.getName(), relation.getFrom(), joinTable,
          leftColumns);
      createJoinTableRelation(relation.getName(), relation.getTo(), joinTable,
          rightColumns);
    }
    joinTable.setColumns(joinColumns);
    addTable(joinTable);
    return joinTable;
  }

  private void cleanRelation(Relation relation) {
    Relation inverseRelation = relation.getTo().getRelations().stream()
        .filter(r -> r.getTo().equals(relation.getFrom()) && r.getToColumn().equals(
            relation.getFromColumn()) && r.getFromColumn().equals(relation.getToColumn()))
        .findFirst()
        .orElse(null);
    // Remove columns
    relation.getFrom().getColumns()
        .stream()
        .filter(c -> c.equals(relation.getFromColumn()) || c.equals(relation.getToColumn()))
        .filter(Column::isForeignKey)
        .forEach(c -> relation.getFrom().removeColumn(c));
    relation.getTo().getColumns()
        .stream()
        .filter(c -> c.equals(relation.getFromColumn()) || c.equals(relation.getToColumn()))
        .filter(Column::isForeignKey)
        .forEach(c -> relation.getTo().removeColumn(c));
    // Remove the original relation
    relation.getFrom().getRelations().remove(relation);
    relation.getTo().getRelations().remove(inverseRelation);
  }

  private void extractManyToManyRelations(Table table) {
    deduplicateManyToManyRelations(table);

    // First determine if there are relations that need to be merged
    if (schemaGeneratorProperties.getMergeJoinTables().isEnabled()) {
      // Find all relations that are many-to-many to the same target
      List<Relation> relations = new ArrayList<>(table.getRelations());
      relations
          .stream()
          .filter(r -> r.getCardinality() == Cardinality.MANY_TO_MANY)
          .map(Relation::getTo)
          .distinct()
          .forEach(targetTable -> {
            List<Relation> relationsToTarget = table.getRelations().stream()
                .filter(r -> r.getCardinality() == Cardinality.MANY_TO_MANY && r.getTo().equals(
                    targetTable))
                .toList();
            if (relationsToTarget.size() > 1 && !isExcludedFromMerge(table, targetTable)) {
              // Create new enum type
              Enum enumType = new Enum();
              enumType.setName(
                  toSnakeCase(table.getName() + "_" + targetTable.getName() + "_merge_type"));
              enumType.setValues(relationsToTarget.stream().map(r -> {
                String relationName = r.getName() != null ? r.getName() : "relation";
                EnumValue enumValue = new EnumValue();
                enumValue.setName(toSnakeCase(relationName).toUpperCase());
                return enumValue;
              }).toList());
              schemaEnums.add(enumType);
              Relation relation = new Relation(relationsToTarget.getFirst());
              relation.setName(toSnakeCase(table.getName() + "_" + targetTable.getName()));

              Table joinTable = createJoinTable(relation, false);
              if (joinTable == null) {
                return;
              }
              // Add merge type column
              Column mergeTypeColumn = new Column();
              mergeTypeColumn.setName(
                  schemaGeneratorProperties.getMergeJoinTables().getAttributeName());
              mergeTypeColumn.setDataType(new DataType(enumType.getName(), enumType.getUri()));
              mergeTypeColumn.setForeignKey(false);
              mergeTypeColumn.setNullable(false);
              mergeTypeColumn.setPrimaryKey(true);
              mergeTypeColumn.setDomain(joinTable);
              joinTable.addColumn(mergeTypeColumn);
              // Remove columns of the original relation(s)
              relationsToTarget.forEach(this::cleanRelation);
            }
          });
    }
    // Extract many-to-many relations
    List<Relation> relations = new ArrayList<>(table.getRelations());
    relations.forEach(relation -> {
      if (relation.getCardinality() == Cardinality.MANY_TO_MANY) {
        boolean appendRelationName = shouldAppendRelationNameForSelfRelation(relation, relations);
        Table joinTable = createJoinTable(relation, appendRelationName);
        if (joinTable != null) {
          cleanRelation(relation);
        }
      }
    });
  }

  private void deduplicateManyToManyRelations(Table table) {
    List<Relation> deduplicated = new ArrayList<>();
    for (Relation relation : table.getRelations()) {
      if (relation.getCardinality() != Cardinality.MANY_TO_MANY) {
        deduplicated.add(relation);
        continue;
      }

      String relationName = relation.getName();
      String fromName = relation.getFrom() != null ? relation.getFrom().getName() : null;
      String toName = relation.getTo() != null ? relation.getTo().getName() : null;

      boolean alreadyPresent = deduplicated.stream().anyMatch(existing ->
          existing.getCardinality() == Cardinality.MANY_TO_MANY
              && same(existing.getName(), relationName)
              && same(existing.getFrom() != null ? existing.getFrom().getName() : null, fromName)
              && same(existing.getTo() != null ? existing.getTo().getName() : null, toName));
      if (!alreadyPresent) {
        deduplicated.add(relation);
      }
    }
    table.setRelations(deduplicated);
  }

  private boolean same(String first, String second) {
    return first == null ? second == null : first.equals(second);
  }

  /**
   * Utility to convert XSD data types to SQL data types.
   *
   * @param type The XSD data type URI (e.g., "http://www.w3.org/2001/XMLSchema#string")
   * @return The corresponding SQL data type (e.g., "VARCHAR")
   */
  protected static String xsdToSQL(String type) {
    if (type == null) {
      return "VARCHAR"; // Default to VARCHAR if type is unknown
    }
    return switch (type) {
      case "http://www.w3.org/2001/XMLSchema#string" -> "VARCHAR";
      case "http://www.w3.org/2001/XMLSchema#integer" -> "INT";
      case "http://www.w3.org/2001/XMLSchema#decimal" -> "DECIMAL";
      case "http://www.w3.org/2001/XMLSchema#boolean" -> "BOOLEAN";
      case "http://www.w3.org/2001/XMLSchema#date" -> "DATE";
      case "http://www.w3.org/2001/XMLSchema#dateTime" -> "TIMESTAMP";
      default -> "VARCHAR"; // Fallback for unrecognized types
    };
  }

  protected void addTable(Table table) {
    if (tables.stream().anyMatch(t -> t.getName().equals(table.getName()))) {
      throw new IllegalArgumentException("Duplicate table name: " + table.getName());
    }
    tables.add(table);
  }

  protected enum TableType {
    REGULAR, IDENTITY, JOIN
  }

  @Getter
  @Setter
  protected static class Table extends Clazz {

    private List<Relation> relations = new ArrayList<>();
    private String diagramStyle;
    private TableType tableType = TableType.REGULAR;

    public Table() {
    }

    public Table(Clazz clazz) {
      super(clazz);
      setClassInfo(clazz.getClassInfo());
      setName(toSnakeCase(clazz.getName()));
      setAttributes(new ArrayList<>());
      if (clazz.getExtendsClass() != null) {
        setExtendsClass(new Table(clazz.getExtendsClass()));
      } else if (!clazz.getInterfaces().isEmpty()) {
        setExtendsClass(new Table(clazz.getInterfaces().getFirst()));
      }
      List<Column> columns = clazz.getAttributes()
          .stream()
          .map(Column::new)
          .toList();
      addAllColumns(columns);
    }

    public List<Column> getColumns() {
      return super.getAttributes().stream()
          .filter(a -> a instanceof Column)
          .map(a -> (Column) a)
          .toList();
    }

    public void addColumn(Column column) {
      getAttributes().add(column);
    }

    public void addAllColumns(List<Column> columns) {
      getAttributes().addAll(columns);
    }

    public void removeColumn(Column column) {
      getAttributes().remove(column);
    }

    public void setColumns(List<Column> columns) {
      getAttributes().removeIf(a -> a instanceof Column);
      getAttributes().addAll(columns);
    }

    public Column getColumnByAttribute(Attribute attribute) {
      return getColumns().stream()
          .filter(c -> c.getPropertyInfo() != null && c.getPropertyInfo()
              .equals(attribute.getPropertyInfo()))
          .findFirst()
          .orElse(null);
    }

    public Column getColumnByUri(String propertyUri) {
      return getColumns().stream()
          .filter(
              c -> c.getPropertyInfo() != null && c.getPropertyInfo().getUri().equals(propertyUri))
          .findFirst()
          .orElse(null);
    }

    public Relation getRelationByAttribute(Attribute attribute) {
      return getRelations().stream()
          .filter(r -> r.getFromColumn() != null && r.getFromColumn().getPropertyInfo() != null
              && r.getFromColumn().getPropertyInfo().equals(attribute.getPropertyInfo()) &&
              r.getFromColumn().getDomain().equals(attribute.getDomain()))
          .findFirst()
          .orElse(null);
    }

    public String toString() {
      return "Table{name='" + getName() + "', columns=" + getColumns() + ", relations=" + relations
          + "}";
    }
  }

  @Getter
  @Setter
  protected static class Column extends Attribute {

    private boolean foreignKey;

    public Column() {
    }

    public Column(Attribute attribute) {
      setPropertyInfo(attribute.getPropertyInfo());
      setDataType(new DataType(attribute.getDataType().getName(), attribute.getDataType().getUri()));
      setDomain(attribute.getDomain());
      setName(toSnakeCase(attribute.getName()));
      setRange(attribute.getRange());
      setRangeClasses(new ArrayList<>(attribute.getRangeClasses()));
      setPrimaryKey(attribute.isPrimaryKey());
      setNullable(attribute.isNullable());
      setCardinality(attribute.getCardinality());
      getDataType().setName(xsdToSQL(getDataType().getUri()));
    }

    public String toString() {
      return "Column{name='" + getName() + "', dataType='" + getDataType() + "', primaryKey="
          + isPrimaryKey()
          + ", nullable=" + isNullable() + "}";
    }

    public Column copy() {
      Column column = new Column(this);
      column.foreignKey = foreignKey;
      return column;
    }
  }

  @Getter
  @Setter
  protected static class Relation {

    private String name;
    private Table from;
    private Table to;
    private Column fromColumn;
    private Column toColumn;
    private Cardinality cardinality;

    public Relation() {}

    public Relation(Relation relation) {
      this.name = relation.name;
      this.from = relation.from;
      this.to = relation.to;
      this.fromColumn = relation.fromColumn;
      this.toColumn = relation.toColumn;
      this.cardinality = relation.cardinality;
    }

    public String toString() {
      return "Relation{name='" + name + "', from=" + from.getName() + ", to=" + to.getName()
          + ", cardinality=" + cardinality + "}";
    }
  }

  private boolean isExcludedFromMerge(Table sourceTable, Table targetTable) {
    String sourceUri = sourceTable.getClassInfo() != null ? sourceTable.getClassInfo().getUri() : null;
    String targetUri = targetTable.getClassInfo() != null ? targetTable.getClassInfo().getUri() : null;
    return schemaGeneratorProperties.getMergeJoinTables().getExcludedPairs().stream()
        .anyMatch(excludedPair -> excludedPair.matches(sourceUri, targetUri));
  }

  private void createJoinTableRelation(String name, Table targetTable, Table joinTable,
      List<Column> joinColumns) {
    joinColumns.forEach(joinColumn -> {
      Relation toRelation = new Relation();
      toRelation.setFrom(joinTable);
      toRelation.setTo(targetTable);
      toRelation.setFromColumn(joinColumn);
      toRelation.setToColumn(targetTable.getColumnByUri(joinColumn.getUri()));
      toRelation.setCardinality(Cardinality.MANY_TO_ONE);
      toRelation.setName(name);
      joinTable.getRelations().add(toRelation);
    });
  }

  private void extractInheritance(Table table) {
    Table parentTable = (Table) table.getExtendsClass();
    if (parentTable == null) {
      return;
    }
    Column targetColumn = findIdentifierColumn(parentTable, false);
    if (targetColumn == null) {
      table.setExtendsClass(parentTable);
      return;
    }

    Column column = table.getColumnByUri(targetColumn.getUri());
    if (column == null) {
      column = new Column();
      column.setPropertyInfo(targetColumn.getPropertyInfo());
      column.setDomain(table);
      table.addColumn(column);
    }
    column.setName(toSnakeCase(parentTable.getName()) + "_" + targetColumn.getName());
    column.setForeignKey(true);
    column.setPrimaryKey(true);
    column.setDataType(new DataType(targetColumn.getDataType().getName(),
        targetColumn.getDataType().getUri()));
    column.setCardinality(Cardinality.MANY_TO_ONE);

    Relation relation = new Relation();
    relation.setFrom(table);
    relation.setFromColumn(column);
    relation.setTo(parentTable);
    relation.setToColumn(targetColumn);
    relation.setCardinality(Cardinality.ONE_TO_MANY);
    table.getRelations().add(relation);
    table.setExtendsClass(parentTable);
  }

  private void extractColumnRelations(Table table) {
    for (Attribute attribute : table.getAttributes()) {
      List<Clazz> targetRanges = resolveAttributeRanges(attribute);
      for (Clazz nearestClass : targetRanges) {
        Table targetTable = getTableByClazz(nearestClass, false);
        if (targetTable == null) {
          continue;
        }
        String targetName = targetTable.getName();
        targetTable = getTableByClazz(targetTable, true);

        Column column = table.getColumnByAttribute(attribute);
        Column targetColumn = findIdentifierColumn(targetTable, false);
        if (targetColumn == null) {
          continue;
        }
        if (column != null) {
          boolean relationExists = table.getRelations().stream()
              .anyMatch(r -> r.getFromColumn().equals(column) && r.getToColumn().equals(targetColumn));
          if (relationExists) {
            continue;
          }
          column.setDataType(targetColumn.getDataType());
          column.setForeignKey(true);
        }

        Pair<String, String> propertyNameAndLabel = getPropertyNameAndLabel(
            (PropertyInfo) attribute.getPropertyInfo());
        Relation relation = new Relation();
        relation.setName(toSnakeCase(propertyNameAndLabel.getLeft()) + "_" + targetName);
        relation.setFrom(table);
        relation.setTo(targetTable);
        relation.setFromColumn(column);
        relation.setToColumn(targetColumn);
        relation.setCardinality(attribute.getCardinality());
        table.getRelations().add(relation);
      }
    }
  }

  private List<Clazz> resolveAttributeRanges(Attribute attribute) {
    if (attribute.getRangeClasses() != null && !attribute.getRangeClasses().isEmpty()) {
      return attribute.getRangeClasses();
    }
    if (attribute.getPropertyInfo() instanceof PropertyInfo propertyInfo
        && propertyInfo.getRange() != null && !propertyInfo.getRange().isEmpty()) {
      List<Clazz> resolved = propertyInfo.getRange().stream()
          .map(this::getNearestClass)
          .map(this::getClass)
          .filter(clazz -> clazz != null)
          .distinct()
          .toList();
      if (!resolved.isEmpty()) {
        return resolved;
      }
    }
    if (attribute.getRange() != null) {
      return List.of(attribute.getRange());
    }
    return List.of();
  }

  protected Table getTableByClazz(Clazz clazz) {
    return getTableByClazz(clazz, false);
  }

  protected Table getTableByClazz(Clazz clazz, boolean identifier) {
    if (clazz == null) {
      return null;
    }
    List<Table> candidateTables = tables.stream()
        .filter(t -> t.getClassInfo().equals(clazz.getClassInfo()))
        .toList();
    return candidateTables.stream()
        .filter(t -> !identifier || isPreferredIdentifierTable(t))
        .findFirst()
        .orElse(candidateTables.isEmpty() ? null : candidateTables.getFirst());
  }

  private boolean isPreferredIdentifierTable(Table table) {
    Column identifierColumn = findIdentifierColumn(table, true);
    return identifierColumn == null || !identifierColumn.isForeignKey();
  }
}
