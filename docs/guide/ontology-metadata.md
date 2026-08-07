# Shaping Your Ontology

This page explains which RDF/OWL constructs ODDToolkit actually reads, and which `ontology:` config
keys let you correct or augment an ontology you do not control.

All examples come from the shipped RIE-IEPR example
(`docs/examples/riepr/ontology/ns/riepr/riepr.ttl`) and the working configuration in
`src/test/resources/application.yml`.

For the plain key-by-key reference, see [Configuration](./configuration). For the pipeline stages
that do this work, see [Adapters](./adapters).

## What the toolkit reads

The ontology is loaded with Apache Jena from `ontology.ontology-file-path`. An optional second file,
`ontology.concepts-file-path`, holds a SKOS concept scheme used for naming.

Only these RDF terms influence generation:

| Term | Effect |
|---|---|
| `rdf:type owl:Class` | Declares a class |
| `rdfs:subClassOf` a named class | Superclass; drives inheritance and interface detection |
| `rdfs:subClassOf [ a owl:Restriction ]` | Declares a property on the class |
| `owl:onProperty` | The property a restriction applies to |
| `owl:someValuesFrom`, `owl:allValuesFrom`, `owl:onClass`, `owl:onDataRange` | Range of the property |
| `owl:unionOf`, RDF collections | Union ranges (multiple range classes) |
| `owl:minCardinality`, `owl:maxCardinality`, `owl:cardinality` | Cardinality |
| `owl:minQualifiedCardinality`, `owl:qualifiedCardinality` | Lower bound only |
| `rdf:type owl:ObjectProperty` / `owl:DatatypeProperty` | Marks a resource as a property |
| `rdfs:range` on a property | Range, when the property is declared directly |
| `rdfs:label` | Label of a class or property |
| `rdfs:comment` | Comment of a class, property, or restriction |
| `owl:inverseOf` | Pairs inverse properties and merges their cardinalities |
| `hydra:search` (and `hydra:template`, `hydra:mapping`, `hydra:variable`, `hydra:property`) | URI template and identifiers |
| `owl:equivalentClass` / `owl:equivalentProperty` in the concepts file | Maps a `skos:Concept` onto an ontology term |
| `rdf:type` pointing at an enum class | Individuals become enum values |

Anything else in your Turtle is carried along in the Jena model but does not reach the generators.

## Classes

A class is any resource typed `owl:Class`. Classes in the ontology's own namespace get scope
`ONTOLOGY` and become concrete classes. Superclasses in a different namespace get scope `EXTERNAL`
and become interfaces.

```turtle
@prefix :      <https://data.riepr.omgeving.vlaanderen.be/ns/riepr#> .
@prefix owl:   <http://www.w3.org/2002/07/owl#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix prov:  <http://www.w3.org/ns/prov#> .

:Exploitant a owl:Class ;
    rdfs:label "Exploitant"@nl ;
    rdfs:comment "Een exploitant is een entiteit die een milieuimpact heeft."@nl ;
    rdfs:subClassOf prov:Agent .
```

The class name is the local name of the URI (`Exploitant`), not the label. The label is used for
display, the comment for documentation.

An interface is only kept if more than one concrete class implements it, or if it is the range of
some property. Its properties are narrowed to those that every implementing class has. Superclasses
that are themselves concrete classes become an `extends` relation instead, and the inherited
properties are removed from the subclass.

## Properties

There are two ways a property lands on a class.

**As an OWL restriction on the class.** This is the main mechanism, and the only one that carries
cardinality:

```turtle
:Exploitant rdfs:subClassOf [ a owl:Restriction ;
    rdfs:comment "Een exploitant moet overeenkomen met één organisatie (VKBO)"@nl ;
    owl:onProperty prov:hadPrimarySource ;
    owl:someValuesFrom org:Organization ;
    owl:minCardinality "1"^^xsd:nonNegativeInteger ;
    owl:maxCardinality "1"^^xsd:nonNegativeInteger
] .
```

**As a standalone property declaration**, when the resource is typed `owl:ObjectProperty` or
`owl:DatatypeProperty`:

```turtle
:localId a owl:DatatypeProperty ;
    rdfs:label "Local Identifier"@en ;
    rdfs:comment "Een local identifier is een unieke identificatie binnen de context van RIE-IEPR."@en ;
    rdfs:range xsd:string .
```

Write several restrictions for the same `owl:onProperty` and they are merged into one property: the
strongest lower bound, the strongest upper bound, the union of all ranges, and the first non-null
name, comment and `owl:inverseOf` found.

Inverse properties are collapsed. When two properties are linked by `owl:inverseOf`, each side takes
its `from` cardinality from the other side, and one of the two is dropped unless it carries a
comment. If neither carries a comment, the toolkit writes a generated one (`Inverse property of …`)
on the side with the lower URI.

## Cardinality

Cardinality is read only from OWL restrictions:

- `owl:cardinality N` sets both min and max to `N`.
- `owl:minCardinality` / `owl:maxCardinality` set the bounds independently. These are ignored when
  the restriction is a *qualified* one (it has `owl:onClass` or `owl:onDataRange`).
- `owl:minQualifiedCardinality` and `owl:qualifiedCardinality` set the lower bound only. A qualified
  maximum does not constrain the property globally, so it is not applied.

A missing maximum means "many". The min/max pair on each side is turned into one of
`ONE_TO_ONE`, `ONE_TO_MANY`, `MANY_TO_ONE`, `MANY_TO_MANY`. An attribute is nullable when it is not
an identifier and its minimum is not 1.

## Datatypes and ranges

The range comes from `rdfs:range` on the property, or from `owl:someValuesFrom`, `owl:allValuesFrom`,
`owl:onClass` or `owl:onDataRange` in the restriction. If the range object is a blank node with
`owl:unionOf`, or a Turtle collection, every URI member becomes a range — that produces a union type.

If a range URI matches a class in the model, the property becomes a relation. Otherwise the range URI
is used as the attribute's datatype. When a property's range spans both a class and one of its own
subclasses, only the most general class is kept.

### Rewriting datatypes

`ontology.override-datatypes` rewrites one RDF datatype URI into another, everywhere it appears as a
range. It maps RDF datatype to RDF datatype — not to SQL or Java types.

```yaml
ontology:
  override-datatypes:
    - uri: "http://www.w3.org/2000/01/rdf-schema#Literal"
      override: "http://www.w3.org/2001/XMLSchema#string"
```

### Preserving a typed literal's datatype

If you give a property the range `rdfs:Datatype`, the toolkit stores the value as a string *and*
synthesises a second attribute, the attribute name suffixed with `_datatype` and also a string, to
hold the datatype IRI.
The example config uses this for `skos:notation`:

```yaml
ontology:
  override-properties:
    # By setting the range to rdfs:Datatype, we create a new attribute for the datatype
    - uri: "http://www.w3.org/2004/02/skos/core#notation"
      range: "http://www.w3.org/2000/01/rdf-schema#Datatype"
```

## Identifiers and URI templates

A property becomes an identifier (a primary key downstream) in exactly three ways: a `hydra:search`
URI template, `ontology.extra-properties` with `identifier: true`, or `ontology.override-properties`
with `identifier: true`.

### hydra:search

Attach a Hydra IRI template to a class to say how its instance URIs are built. The properties it maps
are its identity:

```turtle
@prefix hydra: <http://www.w3.org/ns/hydra/core#> .
@prefix dct:   <http://purl.org/dc/terms/> .

:Exploitatielocatie a owl:Class ;
    rdfs:label "Exploitatielocatie"@nl ;
    hydra:search [ a hydra:IriTemplate ;
        hydra:template "https://data.mjv.omgeving.vlaanderen.be/id/exploitatielocatie/{uuid}/{issued}/{created}"^^hydra:Rfc6570Template ;
        hydra:mapping [ hydra:variable "uuid" ;    hydra:property :localId ] ,
                      [ hydra:variable "issued" ;  hydra:property dct:issued ] ,
                      [ hydra:variable "created" ; hydra:property dct:created ]
    ] .
```

What the toolkit does with it:

- `hydra:template` is stored as the template string.
- Every `hydra:mapping` contributes one `hydra:variable` → `hydra:property` pair. Mappings without
  both, or whose `hydra:property` is not a resource, are skipped.
- Every mapped property is marked as an identifier. If the class does not already have that property,
  it is added with cardinality exactly 1.
- Primary keys are ordered by where their variable appears left-to-right in the template string.
  A mapped property whose variable is absent from the template is still an identifier, it just sorts
  after the ones that appear.

Those five terms — `hydra:search`, `hydra:template`, `hydra:mapping`, `hydra:variable`,
`hydra:property` — are the only Hydra vocabulary the toolkit reads. Anything else on the template
(including `hydra:required`, which the example ontology writes) is ignored.

### The `uri` fallback

If a class ends up with no identifier at all, the toolkit falls back to an attribute literally named
`uri` and logs a warning. This is why the example config injects a `uri` extra property onto every
class. If there is no `uri` attribute either, you get a warning and a table with no primary key.

Run with `ODD_LOG_LEVEL=DEBUG` to see these warnings in context.

### Surrogate keys

When a class has more than one identifier — for example a natural key combined with temporal
versioning properties — you can collapse them into a single generated key:

```yaml
ontology:
  surrogate-keys:
    enabled: true
    name: "id"
    datatype: "http://www.w3.org/2001/XMLSchema#string"
```

The original identifiers stay as regular, non-key attributes. Defaults: `enabled: false`,
`name: "id"`, `datatype: xsd:string`. Classes with one or zero identifiers are untouched.

## Enum classes

List the class URIs you want turned into enumerations under `ontology.enum-classes.classes`. Both
ontology and external classes are eligible, which is how ADMS and SOSA vocabulary classes qualify:

```yaml
ontology:
  enum-classes:
    classes:
      - "http://www.w3.org/ns/sosa/Procedure"
      - "http://www.w3.org/ns/adms#Status"
    trim-class-name-from-values: true
```

Note the shape: `enum-classes` is an object with a `classes` list, not a bare list of URIs.

Values are collected from two places:

1. **Subclasses** of the enum class — but only those with no domain-specific properties. A subclass
   is skipped if it has any property that is neither an identifier nor one of your
   `extra-properties`.
2. **Individuals**, meaning any resource whose `rdf:type` is the enum class itself.

The RIE-IEPR ontology hard-codes its code lists as individuals that are also `skos:Concept`s:

```turtle
@prefix adms: <http://www.w3.org/ns/adms#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .

:inGebruik a adms:Status, skos:Concept ;
    rdfs:label "In gebruik"@nl ;
    rdfs:comment "De entiteit is in gebruik."@nl .

:ontmanteld a adms:Status, skos:Concept ;
    rdfs:label "Ontmanteld"@nl ;
    rdfs:comment "De entiteit is ontmanteld."@nl .
```

Value names are the local name in `UPPER_SNAKE_CASE`, so these become `IN_GEBRUIK` and `ONTMANTELD`.

With `trim-class-name-from-values: true`, a redundant class-name token is stripped from the start or
end of the value name. For `sosa:Procedure` the class token is `PROCEDURE`, so `:verwerkingProcedure`
becomes `VERWERKING` instead of `VERWERKING_PROCEDURE`. Trimming is skipped if it would leave an
empty name.

Once a class is an enum, it and its harvested values are removed from the regular class and interface
lists.

## Extra properties

`ontology.extra-properties` injects a property onto **every** class that does not already have that
URI. Use it for columns your ontology does not model but your storage needs.

```yaml
ontology:
  extra-properties:
    # We want to keep the URI in the database for reference
    - name: "uri"
      uri: "http://example.org/vocab/uri"
      comment: "URI"
      range: "http://www.w3.org/2001/XMLSchema#string"
      cardinality:
        max: 1
        min: 1
```

Each entry is an object, not a bare URI. Fields:

| Field | Meaning |
|---|---|
| `uri` | Required. Entries without a URI are skipped |
| `name` | Attribute name; also used as the label |
| `comment` | Documentation string |
| `range` | Range URI. Omit for an unconstrained range |
| `identifier` | `true` marks it as a primary key. Default `false` |
| `cardinality` | `{min, max}`. Defaults to unbounded |

Extra properties sort directly after the primary keys, in the order you list them.

## Override properties

`ontology.override-properties` patches a property wherever it appears, matched on its URI. Use it to
correct a vocabulary you do not control.

```yaml
ontology:
  override-properties:
    - uri: "https://data.riepr.omgeving.vlaanderen.be/ns/riepr#localId"
      comment: "UUID"
      name: "uuid"
      identifier: true
      range: "http://www.w3.org/2001/XMLSchema#string"
      cardinality:
        max: 1
        min: 1
    - uri: "http://www.w3.org/2000/01/rdf-schema#label"
      cardinality:
        max: 1
      range: "http://www.w3.org/2001/XMLSchema#string"
    - uri: "http://qudt.org/schema/qudt/numericValue"
      cardinality:
        max: 1
      range: "http://www.w3.org/2001/XMLSchema#decimal"
```

This is a **list keyed on property URI**, not a map keyed on class URI. Fields:

| Field | Meaning |
|---|---|
| `uri` | Required. The property to patch. Entries without it are skipped |
| `name` | Replaces the attribute name |
| `comment` | Replaces the comment |
| `range` | Replaces the whole range with this single URI |
| `datatype` | Fallback used as the range when `range` is absent |
| `identifier` | `true` or `false`; overrides the identifier flag |
| `cardinality` | `{min, max}`; replaces the cardinality outright |

Blank and whitespace-only values are treated as absent, so a field you leave out never clears an
existing value. There is no way to override a label here — use the concepts file for that.

## Temporal properties

`ontology.temporal-properties` is a plain list of property URIs:

```yaml
ontology:
  temporal-properties:
    - "http://purl.org/dc/terms/created"
    - "http://purl.org/dc/terms/issued"
    - "http://purl.org/dc/terms/valid"
```

These properties are **not** removed from the generated schema. They do two things:

1. They sort into their own block, after the primary keys and the extra properties, in the order you
   list them.
2. When a class also appears in `ontology.metadata-classes.classes`, its temporal properties are
   copied onto the generated metadata class so each metadata row is versioned the same way as the
   entity it describes.

## Metadata classes

`ontology.metadata-classes` synthesises a key/value side table for the classes you name:

```yaml
ontology:
  metadata-classes:
    suffix: "Metadata"
    key: "http://example.org/vocab/metadataKey"
    value: "http://example.org/vocab/metadataValue"
    classes:
      - "https://data.riepr.omgeving.vlaanderen.be/ns/riepr#Exploitatie"
```

For each listed class that exists in the generated model, a new class is added named
the class name followed by `suffix` (default `Metadata`), containing:

- a `key` string attribute backed by the `key` property URI,
- a `value` string attribute backed by the `value` property URI,
- a copy of every temporal property the source class has, and
- a many-to-one reference attribute back to the source class, named after the source class in
  lowerCamelCase.

Both `key` and `value` are property URIs and have no defaults, so set them explicitly. Class URIs
that do not match a generated class are silently skipped.

## Naming from a concept scheme

The optional `ontology.concepts-file-path` file lets you rename generated classes and properties
without touching the ontology. The toolkit looks for `skos:Concept` resources and links them to
ontology terms through `owl:equivalentClass` and `owl:equivalentProperty`:

```turtle
@prefix :      <https://data.riepr.omgeving.vlaanderen.be/id/concept/> .
@prefix locn:  <http://www.w3.org/ns/locn#> .
@prefix owl:   <http://www.w3.org/2002/07/owl#> .
@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .

:Adres a skos:Concept ;
    skos:prefLabel "adres"@nl ;
    owl:equivalentClass locn:Address ;
    skos:inScheme <> .

:straat a skos:Concept ;
    skos:prefLabel "straat"@nl ;
    owl:equivalentProperty locn:thoroughfare ;
    skos:inScheme <> .
```

When a concept matches a class or property, the concept's **local name** replaces the generated name
(`locn:Address` is emitted as `Adres`, `locn:thoroughfare` as `straat`), and the concept's
`rdfs:label` replaces the label if it has one. `skos:prefLabel`, `skos:definition` and
`skos:inScheme` are not read — they document the scheme for human readers.

A concept whose equivalent class is not yet in the ontology is added as a new class, with the
properties that declare it as their `rdfs:domain`. Only properties that themselves have a matching
concept are kept on such a class.

## What is not read

To save you time when an annotation seems to have no effect:

- **Dublin Core.** `dct:title`, `dct:description`, `dct:creator`, `dct:issued`, `dct:modified` and
  friends are never read as metadata. `dct:` properties matter only when your restrictions reference
  them as properties (as `dct:created` and `dct:issued` do in the example).
- **SKOS labels.** `skos:prefLabel`, `skos:altLabel`, `skos:definition`, `skos:example`,
  `skos:broader`, `skos:narrower`. Labels come from `rdfs:label`, comments from `rdfs:comment`.
- **Hydra beyond templates.** Only `hydra:search`, `hydra:template`, `hydra:mapping`,
  `hydra:variable` and `hydra:property` are read. `hydra:required` on a mapping is ignored — express
  requiredness with an OWL cardinality restriction instead.
- **SHACL in the input.** The `shacl` generator *writes* SHACL shapes; it does not read `sh:` shapes
  from your ontology. See [Generators](./generators).

## Checking your work

Generate everything against a config and inspect the result:

```bash
java -jar target/oddtoolkit.jar --generator=all --config-file=config.yml
```

Point the toolkit at a different ontology without editing the config:

```bash
java -jar target/oddtoolkit.jar \
  --generator=shacl \
  --config-file=config.yml \
  --ontology-file=path/to/ontology.ttl
```

If a class or property is missing, raise the log level to see which adapter dropped it:

```bash
ODD_LOG_LEVEL=DEBUG java -jar target/oddtoolkit.jar --generator=class-diagram --config-file=config.yml
```

Common causes:

- The class is not typed `a owl:Class`.
- The property is on the class but has no `owl:Restriction` and is not typed `owl:ObjectProperty` or
  `owl:DatatypeProperty`.
- The property is inherited from a concrete superclass, so it was removed from the subclass.
- The property is one half of an `owl:inverseOf` pair with no `rdfs:comment`, so it was dropped in
  favour of the other half.
- The class is listed in `ontology.enum-classes.classes`, or is a value of a class that is.

## Related pages

- [Configuration](./configuration) — every `ontology:` key with its default
- [Adapters](./adapters) — the pipeline stages that perform this extraction
- [Generators](./generators) — what each generator does with the extracted model
- [Examples](./examples) — the full RIE-IEPR ontology and config
