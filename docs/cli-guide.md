# CLI Usage Guide

## Build

```bash
./mvnw clean package
```

Use the generated JAR:

```bash
java -jar target/oddtoolkit.jar --help
```

## Core commands

Run one generator:

```bash
java -jar target/oddtoolkit.jar --generator=class-diagram
```

Run all generators:

```bash
java -jar target/oddtoolkit.jar --generator=all
```

Run with config file:

```bash
java -jar target/oddtoolkit.jar --generator=sql --config-file=config.yml
```

Override ontology file paths:

```bash
java -jar target/oddtoolkit.jar \
  --generator=java \
  --config-file=config.yml \
  --ontology-file=/path/to/ontology.ttl \
  --concepts-file=/path/to/concepts.ttl
```

## Available generators

| Name | Description |
|---|---|
| `all` | Run all registered generators |
| `class` | Build in-memory class model |
| `class-diagram` | Mermaid class diagram |
| `er-diagram` | Mermaid ER diagram |
| `sql` | SQL DDL output |
| `shacl` | SHACL shapes |
| `java` | Java model code |
| `typescript` | TypeScript model code |

## CLI options

| Option | Purpose |
|---|---|
| `--generator=NAME` | Select generator (`all` supported) |
| `--config-file=PATH` | Load YAML/JSON config |
| `--output=PATH` | Output directory override |
| `--ontology-file=PATH` | Ontology input override |
| `--concepts-file=PATH` | Concepts input override |
| `--help`, `-h` | Print help |

## Environment variables

Configuration values can be supplied through `ODD_*` variables.

| Variable | Example |
|---|---|
| `ODD_GENERATOR_NAME` | `sql` |
| `ODD_CONFIG_FILE` | `/path/to/config.yml` |
| `ODD_ONTOLOGY_FILE` | `/path/to/ontology.ttl` |
| `ODD_CONCEPTS_FILE` | `/path/to/concepts.ttl` |
| `ODD_OUTPUT_PATH` | `/tmp/out` |

## Configuration precedence

1. CLI arguments
2. Environment variables
3. Config file
4. Defaults

## Troubleshooting

Generator not found:

```bash
java -jar target/oddtoolkit.jar --help
```

Config file not found:

```bash
ls -la /path/to/config.yml
```

Insufficient memory on large ontologies:

```bash
java -Xmx4G -jar target/oddtoolkit.jar --generator=java --config-file=config.yml
```

Verbose application logging:

```bash
java -Dlogging.level.be.vlaanderen.omgeving=DEBUG -jar target/oddtoolkit.jar --generator=sql
```
