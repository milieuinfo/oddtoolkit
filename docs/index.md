---
layout: home

hero:
  name: "ODDToolkit"
  text: "Ontology Driven Design Toolkit"
  tagline: Keep your SQL schema, code models, SHACL shapes, diagrams and data contracts generated from one OWL ontology — so they cannot drift.
  actions:
    - theme: brand
      text: Introduction
      link: /guide/introduction
    - theme: alt
      text: Quick Start
      link: /guide/quickstart
    - theme: alt
      text: Generators
      link: /guide/generators
    - theme: alt
      text: Generated Examples
      link: /guide/examples

features:
  - title: One model, ten artefacts
    details: SQL schema, Java and TypeScript models, SHACL shapes, Mermaid class and ER diagrams, a Bikeshed specification, an ODCS data contract and a JSON data frame — all from the same ontology.
    link: /guide/generators
    linkText: See the generators
  - title: Configurable adapter pipeline
    details: Thirteen adapters load the ontology and concept scheme, run a reasoner, resolve external imports, and apply your property, cardinality and datatype overrides before anything is rendered.
    link: /guide/adapters
    linkText: See the adapters
  - title: Consistency by construction
    details: The SQL, Java, ER and ODCS generators share one relational model, so table names, primary keys and join tables always agree across the outputs.
    link: /guide/architecture
    linkText: How it works
  - title: One command, one config file
    details: A single shaded jar and a YAML or JSON file. Run one generator or all of them, and override the ontology path from the command line.
    link: /guide/cli
    linkText: CLI reference
---
