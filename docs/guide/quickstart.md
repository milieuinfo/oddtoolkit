# Quick Start

Build the toolkit, run one generator against the bundled example ontology, and look at what came out.
About five minutes, most of it the first Maven build.

## 1. Build

From the repository root:

```bash
./mvnw clean package
```

This produces `target/oddtoolkit.jar`. See [Installation](./installation) for prerequisites and
Windows commands.

Check that it runs:

```bash
java -jar target/oddtoolkit.jar --help
```

## 2. Know your inputs

The repository ships a complete working example, the RIE-IEPR ontology:

| File | Role |
|---|---|
| `docs/examples/riepr/ontology/ns/riepr/riepr.ttl` | The ontology |
| `docs/examples/riepr/ontology/id/concept/riepr/riepr.ttl` | The SKOS concept scheme |
| `src/test/resources/application.yml` | A ready-made configuration that points at both |

You always need a configuration file. The ontology and concept-scheme paths have no defaults, so a
run without `--config-file` — and without both `--ontology-file` and `--concepts-file` — fails while
loading the ontology. Start from `src/test/resources/application.yml` and adapt it later.

## 3. Run a generator

Generate the Mermaid class diagram:

```bash
java -jar target/oddtoolkit.jar \
  --generator=class-diagram \
  --config-file=src/test/resources/application.yml
```

The first run reaches out to the network: the `ontology-extract-external` adapter fetches the
vocabularies the ontology imports (results are cached under `target/test-cache/external`), and PNG
export downloads a Playwright browser. Both are optional — see below if you are offline.

## 4. Look at the output

The configuration writes everything under `target/test-cache/`:

```bash
ls target/test-cache/class-diagram/
```

You get `class-diagram.mmd`, and `class-diagram.png` if PNG export succeeded. The `.mmd` file is
plain text — open it, or paste it into any Mermaid renderer.

## 5. Try the others

Same command, different generator name:

```bash
# SQL schema -> target/test-cache/sql/schema.sql
java -jar target/oddtoolkit.jar --generator=sql --config-file=src/test/resources/application.yml

# Java model classes -> target/test-cache/java/
java -jar target/oddtoolkit.jar --generator=java --config-file=src/test/resources/application.yml

# Everything at once
java -jar target/oddtoolkit.jar --generator=all --config-file=src/test/resources/application.yml
```

`--generator=all` runs all ten generators in one JVM. The full list is in the
[CLI reference](./cli), and [Generators](./generators) explains each one.

## If something goes wrong

Turn up the logging — `ODD_LOG_LEVEL` is the only environment variable the toolkit reads:

```bash
ODD_LOG_LEVEL=DEBUG java -jar target/oddtoolkit.jar \
  --generator=class-diagram \
  --config-file=src/test/resources/application.yml
```

Working offline, or want to skip the browser download? Copy the configuration and disable the two
network-dependent parts:

```yaml
generators:
  diagram-generator:
    export-png: false

adapters:
  ontology-extract-external:
    enabled: false
```

Disabling `ontology-extract-external` means imported external vocabularies are not resolved, so the
generated output will be thinner. See [Adapters](./adapters) for what each adapter contributes.

## Next steps

- [CLI Reference](./cli) — every option, with its failure modes
- [Configuration Reference](./configuration) — the `ontology`, `generators` and `adapters` sections
- [Generators](./generators) — what each generator emits and how to configure it
- [Generated Examples](./examples) — the output this ontology produces
