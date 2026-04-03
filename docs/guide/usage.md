# Usage

This guide walks through the normal workflow: build the toolkit, run a generator, and inspect the generated output.

## Prerequisites

- Java 21+
- Maven 3.9+ (or use `./mvnw`)

## Build

From the repository root:

```bash
./mvnw clean package
```

The build produces:

- `target/oddtoolkit.jar` (main executable JAR)
- `target/original-oddtoolkit.jar` (pre-shaded artifact)

## Run the CLI

Show available commands:

```bash
java -jar target/oddtoolkit.jar --help
```

Run a single generator:

```bash
java -jar target/oddtoolkit.jar --generator=class-diagram
```

Run all generators in one invocation:

```bash
java -jar target/oddtoolkit.jar --generator=all
```

## Use a configuration file

The CLI accepts YAML and JSON files through `--config-file`.

```bash
java -jar target/oddtoolkit.jar \
  --generator=sql \
  --config-file=src/test/resources/application.yml
```

## Override input files from the CLI

You can override ontology input paths without editing the config file:

```bash
java -jar target/oddtoolkit.jar \
  --generator=class \
  --config-file=src/test/resources/application.yml \
  --ontology-file=src/test/resources/examples/ns/riepr/riepr.ttl \
  --concepts-file=src/test/resources/examples/id/concept/riepr/riepr.ttl
```

## Configuration precedence

When the same value appears in multiple sources, the toolkit resolves it in this order:

1. CLI arguments (`--generator=...`, `--ontology-file=...`)
2. Environment variables (`ODD_*`)
3. Config file (`--config-file=...`)
4. Defaults in code

## Typical local workflow

1. Build with `./mvnw clean package`
2. Start with `--generator=class-diagram` to validate ontology extraction
3. Generate target artifacts (`sql`, `java`, `typescript`, `shacl`)
4. Re-run with `--generator=all` before committing
