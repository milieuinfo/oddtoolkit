# Configuration

ODDToolkit reads configuration from a YAML/JSON file and optional CLI overrides.

## Resolution order

Highest to lowest priority:

1. CLI flags (`--generator`, `--ontology-file`, `--concepts-file`, ...)
2. Environment variables (`ODD_*`)
3. Config file (`--config-file`)
4. Defaults in code

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

- `ontology-file-path`
- `concepts-file-path`
- `enum-classes`
- `temporal-properties`
- `extra-properties`
- `override-properties`
- `override-datatypes`

Example:

```yaml
ontology:
  ontology-file-path: "path/to/ontology.ttl"
  concepts-file-path: "path/to/concepts.ttl"
  temporal-properties:
    - "http://purl.org/dc/terms/created"
    - "http://purl.org/dc/terms/modified"
  override-datatypes:
    - uri: "http://www.w3.org/2000/01/rdf-schema#Literal"
      override: "http://www.w3.org/2001/XMLSchema#string"
```

## `generators` section

Generator names used by CLI:

- `class`
- `class-diagram`
- `er-diagram`
- `sql`
- `shacl`
- `java`
- `typescript`
- `bikeshed`
- `all` (special CLI mode)

Some generator-specific property blocks use `*-generator` names (for example `sql-generator`, `java-generator`, `typescript-generator`, `shacl-generator`, `schema-generator`, `bikeshed-generator`).

Example with adapter pinning:

```yaml
generators:
  java:
    adapters:
      - "ontology-load"
      - "ontology-class-extract"
      - "ontology-property-extract"
      - "ontology-property-override"
  java-generator:
    output-directory: "target/generated/java"
    package-name: "be.vlaanderen.omgeving.generated"
```

### Bikeshed generator

The `bikeshed` generator produces a [Bikeshed](https://tabatkins.github.io/bikeshed/) (`.bs`) specification source file documenting all classes and their properties. The resulting file can be processed by the `bikeshed` CLI tool or the [online API](https://api.csswg.org/bikeshed/) to produce a W3C-style HTML specification.

```yaml
generators:
  bikeshed-generator:
    output-file: "target/ontology.bs"
    title: "My Ontology"
    status: "ED"                      # LS | ED | WD | CR | PR | REC
    shortname: "my-ontology"          # slug used in the TR URL (optional)
    editor-name: "Jane Doe"
    editor-email: "jane@example.org"
    editor-affiliation: "My Organisation"
    abstract-text: "This specification describes…"  # optional; falls back to rdfs:comment
```

Run it from the CLI:

```bash
java -jar target/oddtoolkit.jar \
  --generator=bikeshed \
  --config-file=config.yml
```

Then convert the generated `.bs` file to HTML:

```bash
# Using the bikeshed CLI (pip install bikeshed)
bikeshed spec target/ontology.bs target/ontology.html

# Or using the online API
curl https://api.csswg.org/bikeshed/ -F file=@target/ontology.bs > target/ontology.html
```

## `adapters` section

Adapters can be enabled/disabled and can have their own settings.

```yaml
adapters:
  ontology-reasoner:
    enabled: true
    reasoner-type: "owl"
    reasoner-materialize: true

  ontology-extract-external:
    enabled: true
    cache-enabled: true
    cache-dir: "target/cache/external"
```

## CLI overrides

`--ontology-file` and `--concepts-file` override the values from `ontology` in your config file.

```bash
java -jar target/oddtoolkit.jar \
  --generator=class-diagram \
  --config-file=config.yml \
  --ontology-file=/tmp/ontology.ttl
```
