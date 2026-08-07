# Configuration

ODDToolkit is configured from a single file that you pass with `--config-file`. This page is the
reference for that file: how it is loaded, and what every key under `ontology:` means. The
`generators:` and `adapters:` sections are summarised here and documented key by key on
[Generators](./generators) and [Adapters](./adapters).

## How configuration is loaded

Point the CLI at a file:

```bash
java -jar target/oddtoolkit.jar --generator=sql --config-file=application.yml
```

The file extension decides the parser:

| Extension | Format |
|---|---|
| `.yml`, `.yaml` | YAML |
| `.json` | JSON |

Any other extension is rejected with a warning and the run continues with defaults. A missing file
is also only a warning, not an error — so check your path if a generator produces nothing.

The file has three top-level sections, all optional:

| Section | Purpose |
|---|---|
| `ontology` | Input files and how the ontology is interpreted |
| `generators` | Per-generator output paths and settings |
| `adapters` | Which pipeline steps run, and their settings |

### Key naming

Keys are **kebab-case**. Each configuration class carries a `@ConfigPrefix` naming its section, and
`ConfigurationBinder` binds that section with Jackson using the `KEBAB_CASE` naming strategy. So the
Java field `ontologyFilePath` is the YAML key `ontology-file-path`, and `trimClassNameFromValues` is
`trim-class-name-from-values`.

Unknown keys inside a bound section are not tolerated: a typo like `output-fil` aborts the run with
an "unrecognized field" error rather than being ignored. Section *names* are a different matter —
they are resolved by path, so a section at the wrong nesting level binds nothing at all and you
silently get defaults. If a setting seems to have no effect, check its indentation first.

### Precedence

There are two configuration tiers plus the code defaults:

1. **Command line** — `--ontology-file` and `--concepts-file` only. These overwrite
   `ontology.ontology-file-path` and `ontology.concepts-file-path` after the file is bound.
2. **Config file** — everything else.
3. **Defaults in code.**

No other option overrides configuration. There is no environment-variable tier: the only environment
variable the toolkit reads is `ODD_LOG_LEVEL`, which sets the log level and nothing else. See
[CLI](./cli) for the full option list.

## The `ontology` section

This section describes the inputs and how the RDF model is turned into classes, properties and
datatypes. It binds to `OntologyConfiguration`.

### Input files

| Key | Type | Default | Description |
|---|---|---|---|
| `ontology-file-path` | string | — | Path to the RDF file with classes and properties |
| `concepts-file-path` | string | — | Path to the RDF file with SKOS concept schemes |

```yaml
ontology:
  ontology-file-path: "docs/examples/riepr/ontology/ns/riepr/riepr.ttl"
  concepts-file-path: "docs/examples/riepr/ontology/id/concept/riepr/riepr.ttl"
```

Both can be overridden per run with `--ontology-file=` and `--concepts-file=`.

### `enum-classes`

An **object**, not a list. It names the classes whose individuals become enumeration values.

| Key | Type | Default | Description |
|---|---|---|---|
| `classes` | list of URI strings | `[]` | Class URIs to treat as enumerations |
| `trim-class-name-from-values` | boolean | `false` | Strip a redundant class-name prefix or suffix from value names (`TRANSPORT_PROCEDURE` becomes `TRANSPORT`) |

```yaml
ontology:
  enum-classes:
    classes:
      - "http://www.w3.org/ns/sosa/Procedure"
      - "http://www.w3.org/ns/adms#Status"
    trim-class-name-from-values: true
```

### `temporal-properties`

A flat list of property URIs that carry validity or versioning timestamps. Used when building
identity tables and metadata classes.

```yaml
ontology:
  temporal-properties:
    - "http://purl.org/dc/terms/created"
    - "http://purl.org/dc/terms/issued"
    - "http://purl.org/dc/terms/valid"
```

### `extra-properties`

A list of **objects**. Each one is injected into every extracted class that does not already have a
property with that URI. Use it for fields the ontology does not model but your schema needs.

| Key | Type | Default | Description |
|---|---|---|---|
| `uri` | string | — | Property URI; required, entries without it are skipped |
| `name` | string | — | Attribute/column name in generated output |
| `comment` | string | — | Documentation comment |
| `range` | URI string | — | Datatype or class URI |
| `identifier` | boolean | `false` | Treat as (part of) the primary key |
| `cardinality.min` | integer | — | Minimum cardinality |
| `cardinality.max` | integer | — | Maximum cardinality |

The field is `identifier`. There is no `isIdentifier` key.

```yaml
ontology:
  extra-properties:
    # Keep the subject URI in the database for reference
    - name: "uri"
      uri: "http://example.org/vocab/uri"
      comment: "URI"
      range: "http://www.w3.org/2001/XMLSchema#string"
      cardinality:
        min: 1
        max: 1
```

### `override-properties`

A list of **objects**, each keyed by the property `uri` it patches. Every other key is optional; only
the ones you set are applied, everywhere that property occurs.

| Key | Type | Description |
|---|---|---|
| `uri` | string | Property URI to match; required |
| `name` | string | Replacement attribute/column name |
| `comment` | string | Replacement documentation comment |
| `range` | URI string | Replacement range (datatype or class URI) |
| `datatype` | URI string | Fallback for `range`; used only when `range` is absent |
| `identifier` | boolean | Mark or unmark the property as an identifier |
| `cardinality.min` | integer | Replacement minimum cardinality |
| `cardinality.max` | integer | Replacement maximum cardinality |

```yaml
ontology:
  override-properties:
    - uri: "https://data.riepr.omgeving.vlaanderen.be/ns/riepr#localId"
      name: "uuid"
      comment: "UUID"
      identifier: true
      range: "http://www.w3.org/2001/XMLSchema#string"
      cardinality:
        min: 1
        max: 1
    - uri: "http://www.w3.org/2000/01/rdf-schema#label"
      range: "http://www.w3.org/2001/XMLSchema#string"
      cardinality:
        max: 1
    # Setting the range to rdfs:Datatype creates a separate attribute for the datatype
    - uri: "http://www.w3.org/2004/02/skos/core#notation"
      range: "http://www.w3.org/2000/01/rdf-schema#Datatype"
```

### `override-datatypes`

A list of `{uri, override}` pairs. Both sides are **RDF datatype URIs** — this rewrites the range of
every property that uses `uri`, it does not map to SQL types. Entries with a blank `uri` or
`override` are skipped.

```yaml
ontology:
  override-datatypes:
    - uri: "http://www.w3.org/2000/01/rdf-schema#Literal"
      override: "http://www.w3.org/2001/XMLSchema#string"
```

### `metadata-classes`

Generates a companion key/value class next to each listed class, for properties that are modelled as
free-form annotations rather than fixed columns. Nothing happens while `classes` is empty.

| Key | Type | Default | Description |
|---|---|---|---|
| `suffix` | string | `"Metadata"` | Appended to the source class name to name the companion class |
| `key` | URI string | — | Property URI used as the `key` attribute |
| `value` | URI string | — | Property URI used as the `value` attribute |
| `classes` | list of URI strings | `[]` | Class URIs that get a metadata companion |

```yaml
ontology:
  metadata-classes:
    suffix: "Metadata"
    key: "http://www.w3.org/2004/02/skos/core#notation"
    value: "http://www.w3.org/1999/02/22-rdf-syntax-ns#value"
    classes:
      - "https://data.riepr.omgeving.vlaanderen.be/ns/riepr#Proces"
```

The companion class gets a `key` and a `value` attribute, a reference back to the source class, and
a copy of every `temporal-properties` entry the source class actually has.

### `surrogate-keys`

When a class ends up with more than one identifier property — typically a natural key combined with
a temporal or versioning property — replace the composite key with one generated key. Disabled by
default.

| Key | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | `false` | Turn surrogate keys on |
| `name` | string | `"id"` | Name of the generated attribute/column |
| `datatype` | URI string | `http://www.w3.org/2001/XMLSchema#string` | Datatype of the generated key |

```yaml
ontology:
  surrogate-keys:
    enabled: true
    name: "id"
    datatype: "http://www.w3.org/2001/XMLSchema#string"
```

## The `generators` section

Each generator reads its own subsection. The key is the config prefix, which is not always the same
as the generator name you pass to `--generator`: for example `--generator=sql` reads
`generators.sql-generator`, while `--generator=class-diagram` reads `generators.class-diagram`.

```yaml
generators:
  class-diagram:
    output-file: "target/class-diagram.mmd"
  er-diagram:
    output-file: "target/er-diagram.mmd"
  sql-generator:
    output-file: "target/schema.sql"
  java-generator:
    output-directory: "target/java"
    package-name: "com.example.model"

  # Shared by every schema-based generator (sql, java, er-diagram, odcs)
  schema-generator:
    join-table-name-pattern: "{source_table}_{target_table}"

  # Shared by every diagram-based generator
  diagram-generator:
    export-png: false
```

Two subsections are shared rather than owned by a single generator:

- `generators.schema-generator` — join-table naming, join-table merging and identity tables, used by
  `sql`, `java`, `er-diagram` and `odcs`.
- `generators.diagram-generator` — PNG export and Mermaid styling, used by the diagram and schema
  generators.

### Restricting the pipeline per generator

A generator can be limited to a subset of adapters with an `adapters` list. This list is looked up
under the **generator name**, not under the settings prefix — so it goes in `generators.sql`, not in
`generators.sql-generator`:

```yaml
generators:
  sql-generator:
    output-file: "target/schema.sql"
  sql:
    adapters:
      - ontology-load
      - ontology-class-extract
      - ontology-property-extract
```

Leave the list out and the generator runs every enabled adapter, ordered by their declared
dependencies. Names that are unknown or disabled are skipped with a warning.

The lookup keys are `class`, `class-diagram`, `er-diagram`, `sql`, `shacl`, `java`, `typescript`,
`bikeshed` and `odcs`. The `data-frame` generator has no key of its own; it reuses
`generators.class.adapters`.

::: warning
For `class-diagram` and `er-diagram` the generator name and the settings prefix are the same key.
Those two sections are bound to typed classes that reject unknown keys, so adding `adapters:` there
aborts the run with an "unrecognized field" error. Only the generators whose settings live under a
`-generator` prefix can take an `adapters` list.
:::

For the complete key list, defaults and output formats per generator, see
[Generators](./generators).

## The `adapters` section

Adapters are the pipeline steps that load and shape the ontology model before generation. Every
adapter is enabled by default and can be switched off individually:

```yaml
adapters:
  ontology-reasoner:
    enabled: false
```

The 13 adapters, in pipeline order, are `ontology-load`, `ontology-extract-external`,
`ontology-reasoner`, `ontology-class-extract`, `ontology-uri-template`,
`ontology-property-extract`, `ontology-property-override`, `ontology-property-extra`,
`ontology-datatype-override`, `ontology-individuals-extract`, `concept-scheme-load`,
`concept-scheme-extract` and `concept-class-extract`.

Two of them have settings beyond `enabled`:

```yaml
adapters:
  ontology-reasoner:
    enabled: true
    reasoner-type: "owl"
    rules-file: "src/test/resources/examples/reasoner.rules"
  ontology-extract-external:
    cache-enabled: true
    cache-dir: "target/cache/external"
    mirrors:
      - uri: "http://www.w3.org/ns/prov#"
        mirrors:
          - "https://www.w3.org/ns/prov-o"
```

See [Adapters](./adapters) for every key, its default, and what disabling each adapter costs you.

## A minimal working config

Copy this, change the two paths, and it runs:

```yaml
ontology:
  ontology-file-path: "docs/examples/riepr/ontology/ns/riepr/riepr.ttl"
  concepts-file-path: "docs/examples/riepr/ontology/id/concept/riepr/riepr.ttl"

generators:
  class-diagram:
    output-file: "target/class-diagram.mmd"
  sql-generator:
    output-file: "target/schema.sql"
```

```bash
java -jar target/oddtoolkit.jar --generator=sql --config-file=application.yml
```

## The full worked example

`src/test/resources/application.yml` in the repository is a real, working configuration that
exercises nearly every feature on this page: enum classes, extra and override properties, datatype
overrides, all ten generators, schema and diagram settings, the reasoner, and external ontology
mirrors. Use it as the reference when you are unsure about nesting.

Next: [Generators](./generators) for output settings, [Adapters](./adapters) for the pipeline, and
[Ontology metadata](./ontology-metadata) for the RDF annotations the toolkit reads from your
ontology itself.
