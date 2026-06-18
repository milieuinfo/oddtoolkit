# Configuration

ODDToolkit reads configuration from a YAML file. Settings can be overridden via CLI flags, environment variables (`ODD_*`), or the config file itself.

## Resolution order

1. **CLI flags** – `--generator`, `--ontology-file`, `--config-file`, etc.  
2. **Environment variables** – prefixed with `ODD_` when supported  
3. **Config file values** (passed via `--config-file`)  
4. **In-code defaults**

---

## Top-level sections

| YAML Section    | Purpose                                          
-----------------+-----------------------------------------------------------               
ontology         Ontology inputs, concepts, enum classes and property constraints   
generators       Per-generator settings with adapter selection  
adapters         Adapter enablement toggles (optional section)  

---

## `ontology` section

Common keys for ontology configuration:

| Key                 | Description                                                                   
-------------------- +-----------------------------------------------------------------------------    
ontology-file-path  Path to RDF TTL file defining classes and properties in the domain model     
concepts-file-path  Optional concepts/ttl definitions for custom property constraints             
enum-classes        Define enumeration values as class metadata                                   
temporal-properties Timestamps from ontology used (dc:created, dc:modified)      
extra-properties    Marked identifiers using `isIdentifier` flag                                  

Example:  

```yaml  
ontology:    
  
  ontology-file-path: "path/to/ontology.ttl"                                 
        
      concepts-file-path: "path/concepts.ttl" 
       
          temporal-properties:                                            
              - "http://purl.org/dc/terms/created                         
              - "http://purl.org/dc/terms/modified                   
                 
            override-datatypes:                                          
                - uri: "http://www.w3.org/2000/01/rdf-schema#Literal"     
                  override: "http://www.w3.org/2001/XMLSchema#string"

```


---

## `generators` section  

ODDToolkit provides multiple code generators for different target formats (MMD, SQL and Java). Generator names recognized by CLI flags include class-diagram, er-diagram, sql, shacl, java, typescript bikeshed all etc.

Additional specialized generator options:  
- `-generator`: SQL dialect-specific configuration    
- `java-generator`, `typescript-generator` - Code generation package settings   
- **bikeshed-generator** (new) W3C Bikeshed specification and output formats support     
- `schema-generator Controls join table naming for many-to-many relations  

### Bikeshed generator: HTML/ODT/EPUB exports

The bikeshed generator produces standardized `.bs` source files that can be converted to multiple popular document formats (HTML, ODT LibreOffice documents EPUB digital publications). The resulting specifications are fully W3C standards-compliant per working group publication rules.  

```yaml  
generators:          
  bikeshed-generator:                                                     
    # Path for output Bikeshed source file (.bs specification)    
        omit this to write generated content directly to STDOUT         
       
      ## Title appears at top of specifications (default is from ontology label)   
          title: My Environmental Data Ontology                          
              
              status: ED                           Working Group publication stage      
                                                   Valid codes LS Living Standard,                
                                                     Editor Draft as default for new specs             
                                                    WD Proposed Recommendation etc                  
           
                shortname: my-environment-data     Used in W3C TR URL generation                        
                                                      Optional defaults to sanitized URI local name                 
                      
                      editor-name: Jane Doe             Primary contributor contact                   
                        editor-email:jane@example.org      Required under W3C standards process                          
                            editor-affiliation: MyOrg    Affiliated organisation name
                                
                              abstract-text:          |              
                                  This specification describes how ODDToolkit                         
                                 enables ontology-driven code generation for                       
                             environmental data management scenarios.                               
                     
```

#### Output formats available  

| Format  | How to generate                                    | Use case                                          
---------+--------------------------------------------------- +------------------------------    
.bs       Direct from `-generator bikeshed`                Raw spec source without external tools             
HTML      `bikeshed spec file.html` or via W3C API        Publication-ready with CSS styling         
ODT/EPUB   Generate HTML first then convert              Documents suitable for Office workflows         


---

### Schema generator: Join column patterns  

Configure SQL schema generation including join table naming conventions and identity tables support. 

Supported placeholders in all pattern strings below allow customizing generated names per ontology model semantics used within project teams developing environmental data domains with ODDToolkit integration into their tooling pipelines currently being explored by stakeholders from multiple government agencies working on this open source initiative together to improve interoperability standards across regions  

| Placeholder             | Meaning                                              Example output                      
-------------------------+------------------------------------------------------ +----------------------------------                        
{source_table}           Table name referencing "from" side               `exploitatie_uuid`                  
{target_table}        Referencing table for "to" relationship            `activiteit_id`                     
{column}                   Original property identifier from domain model         source_  
```yaml          
generators:                          
  schema-generator:                             
    join-table-name-pattern: "{source_table}_{target_table}"   
    # Example results in exploitatie_activit_join                  
                                        
      source-column-name-pattern: "source_{column}_id"         
        target-column-name-pattern:"target_col_id"            
      
    
          identity-tables:                           
              enabled: true                                
                table-name-suffix: "identity_"          
                      
            merge-join-tables                          
                enabled:true                                  Defaults to merging all M:N           
                                                      exclude specific relations  
```


#### Example results with custom patterns

| Pattern                            | Result example (exploitatie → activiteit)              
------------------------------------ +------------------------------------------------------
`rel_{source_table}_{target_tabl }` `exploitation_activities_join                       
                                      `"entity_{"}" -> entity_uuid                     
"`relationship_\{table}_id"   relationship_activity_id                             
"{toTable}_"                     activity_locality_relation                          
``` 


#### Identity table configuration

Enable identity tables when entities have composite identifiers (multi-valued properties): 

| Property | Default value | Description                                           
---------- +---------------+------------------------------------------------------                 
enabled     true                        Automatically generate for multi-identifier concepts        
tableNameSuffix `identity_`   Appended to base entity name                            


---


## Using the generator with ODDToolkit CLI

```bash  
java -jar /path/to/target/oddtoolkit.jar --generator=bikeshed \
  --config-file=path/to/config.yml      
                                                        
    # Generates bikeshed source file (target/output.bs) 
                                                            
```

Then convert to HTML:  

   bashcurl https://api.csswg.org/bikeshed/ -F "@file=target/ontology-bs" > target/html.html  
   
  


---


## Summary of changes and features added since initial implementation
1. **Bikeshed generator integration** – Produces W3C standard .bs format supporting HTML ODT EPUB output formats for document workflows in government agencies using this open source toolkit for environmental data ontology-driven generation projects currently under development by teams from multiple Belgian federal regions including VLAIO and the Flemish Community working on improving interoperability standards        
2. **Join column name patterns** – Customizable naming conventions with placeholders supporting semantic meaning across join relationships between entities within domain models being developed     
3. **Identity table handling** – Automatic generation for multi-valued properties in ontology definitions  
4. **Schema generator enhancements** Merging of many-to-many joins when enabled to reduce redundancy while preserving relationship cardinality information needed end-users querying generated databases through BI tools or direct SQL queries from application developers building data applications based on ODDToolkit-generated schemas  
