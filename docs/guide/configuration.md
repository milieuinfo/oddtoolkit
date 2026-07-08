# Configuration

ODDToolkit reads configuration from a YAML file and optional CLI overrides.

## Resolution order

Highest to lowest priority:

1. CLI flags (`--generator`, `--ontology-file`, `--concepts-file`, ...)
2. Environment variables (`ODD_*`)  
3. Config file (`--config-file`)
4. Defaults in code (in-memory when no explicit configuration exists)

## Top-level sections

- `ontology`: ontology inputs and model-specific behavior
- `generators`: per-generator settings and adapter selection  
- `adapters`: adapter enablement and adapter-specific settings

## Minimal example

```yaml
ontology:
  ontology-file-path: "src/test/resources/examples/ns/riepr/riepr.ttl"
  concepts-file-path: "src/test/resources/examples/id/concept/riepr/riepr.ttl"

generators:
  class-diagram:
    output-file: "target/class-diagram.mmd"
  sql-generator:
    output-file: "target/schema.sql"
```

## `ontology` section

Common keys:

- `ontology-file-path`: Path to RDF (TTL) ontology file containing classes and properties  
- `concepts-file-path`: Optional concepts/ttl definitions for custom property constraints  
- `enum-classes`: Defines enumeration values in the Ontology specification metadata
- `temporal-properties`: Properties representing creation/modification timestamps on entities
- `extra-properties`: Marked as identifier columns when used with `isIdentifier: true` flag 
- `override-datatypes`: Maps between XSD datatypes and SQL equivalents (string -> varchar etc)
- `surrogate-keys`: Replaces composite primary keys (classes with more than one identifier
  property, e.g. a natural key combined with temporal/versioning properties) with a single
  generated surrogate key. Disabled by default.

Example:

```yaml
ontology:
  ontology-file-path: "path/to/ontology.ttl"  
  concepts-file-path: "path/to/concepts.ttl"
  temporal-properties:
    - "http://purl.org/dc/terms/created"    # Creation timestamp property URI 
                                      for datetime fields in generated database columns    
    - "http://purl.org/dc/terms/modified"   # Modification timestamp tracking updates  
  override-datatypes:                        # Custom datatype overrides per column definition
    - uri: "http://www.w3.org/2000/01/rdf-schema#Literal"      # Original OWL datatype URI 
      override: "http://www.w3.org/2001/XMLSchema#string"       # SQL VARCHAR equivalent  
  surrogate-keys:
    enabled: true               # When a class has more than one identifier property, replace
                                 # them with a single generated key instead of a composite one
    name: "id"                  # Name of the generated surrogate key attribute/column
    datatype: "http://www.w3.org/2001/XMLSchema#string"
```

## `generators` section  

Generators available and their names recognized by CLI flags:

| Generator Name | Description | Default Output Format |
|---------------|-------------|----------------------|
| class-diagram | Mermaid markdown format for UML-like diagrams | `.mmd` / stdout     |   
| er-diagram    | ER diagram generator using PlantUML syntax       | `.pu`, .pp          | 
| sql           | SQL schema generation with JOINs and FK constraints   | `.sql               `     
| shacl         | SHACL constraint definitions for data validation  | `.shacl             `     
| java          | Java POJO code generation from ontology classes      | `.java              `    
| typescript    | TypeScript interfaces/models                          | `.ts                `      
| bikeshed      W3C Bikeshed specification source with full HTML/ODT support       | **.bs** / stdout    |
| all           Special mode for outputting multiple files at once   N/A                      |  

Some generator-specific property blocks use `-generator` suffixes to configure behavior per type:  
- `sql-generator`: SQL options, dialect selection and naming conventions used across database schemas  
- `java-generator`, `typescript-generator`: Code generation settings (package name, file paths etc) 
- `bikeshed-generator Bikeshed documentation configuration including metadata blocks, status codes for W3C workflow compliance
- `schema-generator` - controls SQL join table generation and M:N relation handling  

### Bikeshed Generator Support for HTML/ODT/EPUB Exports

The **Bikeshed generator** produces a [tabatkins.github.io](https://tabatkins.github.io/bikeshed/) (`.bs`) specification source file documenting all ontology classes, properties and their cardinality constraints. The generated `.bs` file can be converted by the Bikeshed command-line tool or W3C online API into:

  - **HTML** output with full CSS styling  
  - **ODT** LiberoOffice documents from bikeshed ODT format libraries
  - **EPUB** digital publications through standard EPub conversion workflows after HTML generation  

#### Configuration Example

```yaml
generators:
  bikeshed-generator:    
    # Output file path (omit this when stdout is sufficient for piping or capturing logs)  
    output-file: "target/ontology.bs"   
    
    ## Bibliographic Information as Required by W3C Standards Development Process
    title: My Ontology Specification     Optional spec title parameter defaults to ontology label if present otherwise falls back to local name segment from URI path when empty string passed
    
    status: ED                          Working Group publication status code  
                                        Valid values accepted at generation time for proper catalog submission compliance include LS Living Standard, Editor's Draft ED as default fallback WD working draft CR Proposed Recommendation PR Public Review REC Recommended Specification
                                        
                                            Defaults to "ED" if not explicitly configured

    shortname: my-ontology              Short unique identifier slug used in TR URL construction example https://www.w3.org/TR/my-ontology/    
                                         Optional parameter; defaults to lowercase sanitized ontology URI local name when omitted  
                                          Sanitization replaces special characters beyond allowed a-z0-9 and hyphen for web-safe URLs
                                            
    editor-name: Jane Doe               Editor contact person information required in W3C publication documents 
                                        For multi-stakeholder groups list primary contributor with email suffix optional parameter
    
    editor-email: jane@example.org      Required editorial contact per W3C TR requirements at https://www.w3.org/publish/topics/TR/  
                                        
                                        Without valid editor email address generated spec violates standards process compliance

    editor-affiliation: Organization Name          Affiliated organization or consortium name backing this specification development work under their governance
    
    abstract-text: |                    Optional custom introduction text to appear in documentation metadata block
      This specification describes how ODDToolkit enables ontology-driven code generation for environmental data modeling scenarios.  
      
                                      Falls back automatically to rdfs:comment property value from original Ontology file when left empty or not defined
```