# ODDToolkit

ODDToolkit (Ontology Driven Design Toolkit) turns an OWL/RDF ontology into concrete artifacts:
Mermaid class and ER diagrams, a SQL schema, Java and TypeScript model classes, SHACL shapes,
Bikeshed documentation, data frames and Open Data Contract Standard contracts.

It is a plain Java 21 application built on Apache Jena, driven by a configurable adapter pipeline.

## Prerequisites

- JDK 21
- Maven is optional — the repository ships the wrapper (`./mvnw`, or `mvnw.cmd` on Windows)

## Build

```bash
./mvnw clean package
```

The executable JAR lands at `target/oddtoolkit.jar`.

```bash
java -jar target/oddtoolkit.jar --help
```

## Run

Every invocation selects one generator (or `all`) and a configuration file. Options are
`--key=value`; there are no subcommands and no positional arguments.

```bash
java -jar target/oddtoolkit.jar \
  --generator=class-diagram \
  --config-file=src/test/resources/application.yml
```

`src/test/resources/application.yml` is a working configuration for the bundled example ontology in
`docs/examples/riepr/`. Set `ODD_LOG_LEVEL=DEBUG` for verbose logging.

## Project layout

- `src/main/java` — application, generators and adapters
- `src/test/java` — unit tests
- `src/test/resources/application.yml` — example configuration
- `docs/examples/riepr/` — example ontology and generated output
- `docs/` — VitePress documentation site

## Documentation

Full documentation: <https://milieuinfo.github.io/oddtoolkit/>

To build the site locally:

```bash
cd docs
npm install
npm run docs:build
```

## License

ODDToolkit is distributed under the GNU General Public License v3.0.
See [LICENSE](./LICENSE) for the full text.
