# ODDToolkit

ODDToolkit (Ontology Driven Design Toolkit) generates diagrams and source artifacts from ontology-driven models.

## Key Features

- Mermaid class and ER diagram generation
- SQL schema generation
- Java and TypeScript model generation
- SHACL shape generation
- Configurable adapter pipeline

## Quickstart

```bash
./mvnw clean package
java -jar target/oddtoolkit.jar --help
```

Run one generator:

```bash
java -jar target/oddtoolkit.jar --generator=class-diagram --config-file=src/test/resources/application.yml
```

Run all generators:

```bash
java -jar target/oddtoolkit.jar --generator=all --config-file=src/test/resources/application.yml
```

## Project layout

- `src/main/java` - application and generator code
- `src/test/java` - unit tests
- `src/test/resources/application.yml` - example configuration
- `docs/` - VitePress documentation

## Documentation

The docs site source is in `docs/`.

```bash
cd docs
npm install
npm run docs:build
```

## License

ODDToolkit is distributed under the **GNU General Public License v3.0**. See [License](./license) for details and the full license text.

