package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.config.DiagramGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.ODCSGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.config.SchemaGeneratorProperties;
import be.vlaanderen.omgeving.oddtoolkit.model.Cardinality;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates ODCS (Open Data Contract Standard) compliant contracts from the ontology.
 *
 * <p>ODCS is a specification for documenting data contracts in a machine-readable format.
 * This generator creates a JSON representation of the ontology as an ODCS contract, mapping
 * ontology classes to JSON Schema definitions and properties to schema fields.</p>
 *
 * @see <a href="https://opendatamesh-initiative.org/odcs-spec">ODCS Specification</a>
 */
@Getter
public class ODCSGenerator extends SchemaGenerator {
  private static final Logger logger = LoggerFactory.getLogger(ODCSGenerator.class);

  private static final String SCHEMA_URI = "https://raw.githubusercontent.com/opendatamesh-initiative"
      + "/odcs-spec/main/schemas/odcs-v3.1.0.json";
  private static final String JSON_SCHEMA_URI = "https://json-schema.org/draft/2020-12/schema";

  private final ODCSGeneratorProperties odcsGeneratorProperties;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Constructs an ODCSGenerator with the given ontology information and configuration.
   *
   * @param ontologyInfo        the ontology model to generate contracts from
   * @param conceptSchemeInfo   the concept scheme providing vocabulary mappings (may be null)
   * @param adapters            list of adapters to run before generation
   * @param odcsGeneratorProperties ODCS-specific configuration properties
   *                             output file, contract name/version/description, owner/contact info
   * @param schemaGeneratorProperties configuration for table/column extraction
   * @param diagramGeneratorProperties shared diagram and style configuration
   */
  public ODCSGenerator(OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters,
      ODCSGeneratorProperties odcsGeneratorProperties,
      SchemaGeneratorProperties schemaGeneratorProperties,
      DiagramGeneratorProperties diagramGeneratorProperties) {
    super(ontologyInfo, conceptSchemeInfo, adapters, diagramGeneratorProperties,
        schemaGeneratorProperties);
    this.odcsGeneratorProperties = odcsGeneratorProperties;
  }

  @Override
  public String getName() {
    return "odcs";
  }

  @Override
  public String getDescription() {
    return "Generates ODCS (Open Data Contract Standard) contracts from the ontology";
  }

  @Override
  public void run() {
    super.run();
    ObjectNode contract = generateODCS();

    if (odcsGeneratorProperties.getOutputFile() != null) {
      logger.info("Writing ODCS contract to {}", odcsGeneratorProperties.getOutputFile());
      saveToFile(odcsGeneratorProperties.getOutputFile(), contract);
    } else {
      logger.info("ODCS contract:");
      System.out.println(contract.toPrettyString());
    }
  }

  /**
   * Generates the complete ODCS contract structure.
   *
   * @return the root {@link ObjectNode} representing the full ODCS contract JSON
   */
  private ObjectNode generateODCS() {
    ObjectNode contract = objectMapper.createObjectNode();
    contract.put("$schema", SCHEMA_URI);
    contract.set("info", createInfo());
    contract.set("apis", createApis());
    contract.set("schemas", createSchemas());
    return contract;
  }

  /**
   * Creates the metadata info section of the ODCS contract.
   * Populates name, version, description, owner (optional), contact (optional),
   * timestamps, and publication status.
   *
   * @return the {@code "info"} node for the ODCS contract
   */
  private ObjectNode createInfo() {
    ObjectNode info = objectMapper.createObjectNode();
    String name = resolveContractName();
    String version = resolveContractVersion();
    String description = resolveContractDescription();
    info.put("name", name);
    info.put("version", version);
    info.put("description", description);

    if (odcsGeneratorProperties.getOwnerName() != null
        && !odcsGeneratorProperties.getOwnerName().isBlank()) {
      ObjectNode owner = objectMapper.createObjectNode();
      owner.put("name", odcsGeneratorProperties.getOwnerName());
      if (odcsGeneratorProperties.getOwnerEmail() != null) {
        owner.put("email", odcsGeneratorProperties.getOwnerEmail());
      }
      info.set("owner", owner);
    }

    if (odcsGeneratorProperties.getContactName() != null
        && !odcsGeneratorProperties.getContactName().isBlank()) {
      ObjectNode contact = objectMapper.createObjectNode();
      contact.put("name", odcsGeneratorProperties.getContactName());
      if (odcsGeneratorProperties.getContactEmail() != null) {
        contact.put("email", odcsGeneratorProperties.getContactEmail());
      }
      info.set("contact", contact);
    }

    String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
    info.put("issued", now);
    info.put("updated", now);
    info.put("status", "PUBLISHED");
    return info;
  }

  /**
   * Resolves the contract name from configuration, falling back to the ontology name,
   * then to a hardcoded default.
   */
  private String resolveContractName() {
    String name = odcsGeneratorProperties.getContractName();
    if (name != null && !name.isBlank()) {
      return name;
    }
    if (ontologyInfo.getName() != null) {
      return ontologyInfo.getName();
    }
    return "Data Contract";
  }

  /**
   * Resolves the contract version from configuration, defaulting to {@code 1.0.0}.
   */
  private String resolveContractVersion() {
    String version = odcsGeneratorProperties.getContractVersion();
    return (version != null && !version.isBlank()) ? version : "1.0.0";
  }

  /**
   * Resolves the contract description from configuration, falling back to the ontology comment.
   */
  private String resolveContractDescription() {
    String desc = odcsGeneratorProperties.getContractDescription();
    if (desc != null && !desc.isBlank()) {
      return desc;
    }
    if (ontologyInfo.getComment() != null) {
      return ontologyInfo.getComment();
    }
    return "Data contract generated from ontology schema";
  }

  /**
   * Creates the APIs section of the ODCS contract.
   * References the schemas defined in this contract.
   *
   * @return an {@link ArrayNode} containing API references (empty if no tables exist)
   */
  private ArrayNode createApis() {
    ArrayNode apis = objectMapper.createArrayNode();
    if (!getTables().isEmpty()) {
      ObjectNode api = objectMapper.createObjectNode();
      api.put("$ref", "#/components/apis/schema");
      apis.add(api);
    }
    return apis;
  }

  /**
   * Creates the schemas section of the ODCS contract, defining all tables and their columns.
   *
   * @return an {@link ObjectNode} mapping table names to their JSON Schema definitions
   */
  private ObjectNode createSchemas() {
    ObjectNode schemas = objectMapper.createObjectNode();
    for (Table table : getTables()) {
      ObjectNode tableSchema = createTableSchema(table);
      String schemaKey = sanitizeSchemaKey(table.getName());
      schemas.set(schemaKey, tableSchema);
    }
    return schemas;
  }

  /**
   * Creates a JSON Schema definition for a single table.
   * Each column is emitted as a property, and primary keys are listed in the "required" array.
   *
   * @param table the {@link Table} to convert
   * @return an {@link ObjectNode} representing the table's JSON Schema object definition
   */
  private ObjectNode createTableSchema(Table table) {
    ObjectNode schema = objectMapper.createObjectNode();
    schema.put("$schema", JSON_SCHEMA_URI);
    schema.put("type", "object");
    schema.put("title", table.getName());

    String classUri = table.getClassInfo() != null ? table.getClassInfo().getUri() : null;
    if (classUri != null) {
      schema.put("$id", classUri);
    }

    ObjectNode properties = objectMapper.createObjectNode();
    for (Column column : table.getColumns()) {
      ObjectNode columnSchema = createColumnSchema(column);
      String propKey = sanitizePropertyKey(column.getName());
      properties.set(propKey, columnSchema);
    }
    if (!properties.isEmpty()) {
      schema.set("properties", properties);
    }

    ArrayNode required = objectMapper.createArrayNode();
    for (Column column : table.getColumns()) {
      if (column.isPrimaryKey()) {
        required.add(sanitizePropertyKey(column.getName()));
      }
    }
    if (!required.isEmpty()) {
      schema.set("required", required);
    }

    return schema;
  }

  /**
   * Creates a JSON Schema property definition for a single column.
   * Converts the parent class SQL type name to the corresponding JSON Schema type.
   *
   * @param column the {@link Column} to convert
   * @return an {@link ObjectNode} representing the property's JSON Schema definition
   */
  private ObjectNode createColumnSchema(Column column) {
    ObjectNode node = objectMapper.createObjectNode();

    String jsonType;
    if (column.getDataType() != null) {
      jsonType = sqlTypeToJsonSchema(column.getDataType().getName());
    } else {
      jsonType = "string";
    }
    node.put("type", jsonType);

    Cardinality cardinality = column.getCardinality();
    if (cardinality != null && cardinality.isMinOne()) {
      node.put("minItems", 1);
    }

    String propUri = column.getPropertyInfo() != null ? column.getPropertyInfo().getUri() : null;
    if (propUri != null) {
      node.put("$id", propUri);
    }

    return node;
  }

  /**
   * Converts a SQL type name (as produced by the parent {@link SchemaGenerator})
   * to its corresponding JSON Schema data type.
   */
  private static String sqlTypeToJsonSchema(String sqlType) {
    if (sqlType == null || sqlType.isBlank()) {
      return "string";
    }
    String upper = sqlType.toUpperCase();
    switch (upper) {
      case "VARCHAR":
      case "CHAR":
        return "string";
      case "INT":
      case "INTEGER":
      case "BIGINT":
        return "integer";
      case "DECIMAL":
      case "FLOAT":
      case "DOUBLE":
        return "number";
      case "BOOLEAN":
        return "boolean";
      default:
        return "string";
    }
  }

  /**
   * Sanitizes a table name for use as a schema key in the ODCS contract.
   */
  private String sanitizeSchemaKey(String name) {
    if (name == null || name.isBlank()) {
      return "unknown";
    }
    return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
  }

  /**
   * Sanitizes a column name for use as a property key in JSON Schema.
   */
  private String sanitizePropertyKey(String name) {
    if (name == null || name.isBlank()) {
      return "unknown_column";
    }
    return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
  }

  /**
   * Saves the ODCS contract JSON to the configured output file, creating parent directories.
   *
   * @param outputFile the path to write the contract to
   * @param contract   the root JSON node of the ODCS contract
   * @throws RuntimeException if the file cannot be written
   */
  private void saveToFile(String outputFile, ObjectNode contract) {
    try {
      Path outputPath = Paths.get(outputFile);
      Files.createDirectories(outputPath.getParent());
      try (FileWriter writer = new FileWriter(outputFile)) {
        writer.write(contract.toPrettyString());
      }
      logger.info("Successfully wrote ODCS contract to {}", outputFile);
    } catch (IOException e) {
      logger.error("Error writing ODCS contract to file {}: {}", outputFile, e.getMessage(), e);
      throw new RuntimeException("Failed to write ODCS contract to file: " + outputFile, e);
    }
  }

  @Override
  public void validate() throws IllegalStateException {
    super.validate();

    // Validate that we can determine a contract name
    String contractName = resolveContractName();
    if (contractName == null) {
      throw new IllegalStateException("Cannot determine contract name for ODCS generation.");
    }

    // Validate that we can determine a description
    String contractDescription = resolveContractDescription();
    if (contractDescription == null) {
      throw new IllegalStateException(
          "Cannot determine contract description for ODCS generation.");
    }

    // Run adapters first so that tables are populated before checking emptiness
    List<Table> tables = getTables();
    if (tables.isEmpty()) {
      throw new IllegalStateException("ODCS generation failed: the source schema contains no tables.");
    }

    logger.info("ODCS generator validation successful. Contract name '{}', {} table(s) found.",
        contractName, tables.size());
  }
}