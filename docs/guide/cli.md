# CLI Reference

ODDToolkit ships as a single executable JAR. You run it once per invocation, you select a generator,
and the generator writes its output where your configuration file says.

```bash
java -jar target/oddtoolkit.jar --generator=NAME [OPTIONS]
```

See [Installation](./installation) if you have not built `target/oddtoolkit.jar` yet.

## Invocation rules

Three rules cover the whole command line:

- **There are no subcommands.** `--generator=NAME` selects what runs; there is no `oddtoolkit generate`
  or `oddtoolkit run`.
- **There are no positional arguments.** Anything that does not start with `--` is rejected with
  `Ignoring unexpected argument: …` on stderr.
- **Every option is `--key=value`.** The `=` is mandatory. `--generator sql` does not work —
  it is parsed as a malformed option followed by a stray argument. The only exceptions are the bare
  help flags `--help` and `-h`.

Unknown or malformed options print a warning to stderr and are then skipped. They do not stop the run.

## Options

| Option | Required | Meaning |
|---|---|---|
| `--generator=NAME` | Yes | Name of the generator to execute, or `all`. |
| `--config-file=PATH` | In practice yes | Configuration file: `.yml`, `.yaml` or `.json`. |
| `--ontology-file=PATH` | No | Overrides `ontology.ontology-file-path` from the configuration file. |
| `--concepts-file=PATH` | No | Overrides `ontology.concepts-file-path` from the configuration file. |
| `--help`, `-h` | No | Print the usage text and exit without running anything. |

That is the complete list. In particular there is **no** `--output`, `--output-format` or
`--output-file` option, and no way to set arbitrary configuration properties from the command line.
Output locations live in the configuration file only — see [Configuration](./configuration).

### `--generator=NAME`

Selects the generator. The name is case-sensitive; `--generator=SQL` fails, `--generator=sql` works.

```bash
java -jar target/oddtoolkit.jar \
  --generator=sql \
  --config-file=src/test/resources/application.yml
```

The registered names are:

| Name | Produces |
|---|---|
| `bikeshed` | A Bikeshed specification (`.bs`) |
| `class` | The in-memory class model only (no file output) |
| `class-diagram` | A Mermaid class diagram (`.mmd`, plus `.png` unless disabled) |
| `data-frame` | A data frame description (`.json`) |
| `er-diagram` | A Mermaid ER diagram (`.mmd`, plus `.png` unless disabled) |
| `java` | Java model classes in a directory |
| `odcs` | An Open Data Contract Standard contract (`.json`) |
| `shacl` | SHACL shapes in Turtle (`.ttl`) |
| `sql` | A SQL schema (`.sql`) |
| `typescript` | TypeScript model files in a directory |

`--help` prints the same list, read straight from the registry, so it is always current for your
build. [Generators](./generators) documents what each one does and how to configure it.

### The `all` keyword

`all` is not a generator, it is a keyword. It runs every registered generator, in alphabetical order,
in a single JVM:

```bash
java -jar target/oddtoolkit.jar \
  --generator=all \
  --config-file=src/test/resources/application.yml
```

The match is case-insensitive, so `--generator=ALL` also works. Each generator uses its own
configuration section, so a generator with no configured output path falls back to whatever that
generator does by default (usually stdout). If one generator throws, the run stops there and the
remaining generators do not execute.

### `--config-file=PATH`

Points at the YAML or JSON file holding the `ontology`, `generators` and `adapters` sections.

```bash
java -jar target/oddtoolkit.jar --generator=shacl --config-file=./my-config.yml
```

The format is chosen by file extension: `.json` is parsed as JSON, `.yml` and `.yaml` as YAML.
Any other extension is refused with a warning and the file is ignored. A missing file also produces
a warning and is ignored — the run then continues with code defaults, which almost always fails
because no ontology path is set.

### `--ontology-file=PATH` and `--concepts-file=PATH`

Override the two ontology inputs without editing the configuration file. Useful for pointing the same
configuration at a different ontology version.

```bash
java -jar target/oddtoolkit.jar \
  --generator=class-diagram \
  --config-file=src/test/resources/application.yml \
  --ontology-file=docs/examples/riepr/ontology/ns/riepr/riepr.ttl \
  --concepts-file=docs/examples/riepr/ontology/id/concept/riepr/riepr.ttl
```

Blank values are ignored, so passing `--ontology-file=` leaves the configured value alone.

### `--help`, `-h`

Both spellings work. Help wins over everything else: if `--help` appears anywhere on the command
line, the usage text is printed and no generator runs.

```bash
java -jar target/oddtoolkit.jar --help
```

## Configuration precedence

Exactly two tiers plus defaults:

1. Command-line arguments — but only `--ontology-file` and `--concepts-file`
2. The configuration file passed with `--config-file`
3. Code defaults

Nothing else feeds configuration. There are no `ODD_*` configuration variables, no `-D` property
overrides and no implicit config file that gets picked up from the working directory.

## Where output goes

Each generator writes to the path under its own configuration key — `generators.<name>.output-file`
for the single-file generators, `generators.<name>.output-directory` for `java` and `typescript`.
Parent directories are created for you.

Generators that produce a single document (`class-diagram`, `er-diagram`, `sql`, `shacl`, `bikeshed`,
`data-frame`, `odcs`) print to stdout when no output path is configured, which makes them easy to pipe:

```bash
java -jar target/oddtoolkit.jar --generator=sql --config-file=minimal.yml > schema.sql
```

`java` and `typescript` have no stdout fallback: they require `output-directory` and fail without it.
`class` never writes files — it only builds the in-memory model, which is useful for checking that
your ontology loads.

## Logging

`ODD_LOG_LEVEL` is the **only** environment variable the toolkit reads. It sets the log level of the
`be.vlaanderen.omgeving` loggers (default `INFO`); everything else logs at `WARN`.

```bash
ODD_LOG_LEVEL=DEBUG java -jar target/oddtoolkit.jar \
  --generator=sql \
  --config-file=src/test/resources/application.yml
```

Accepted values are the usual Logback levels: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`.
Use `DEBUG` to see each adapter as it runs. Note that `-Dlogging.level.*` does nothing here — that is
Spring syntax and this project does not use Spring.

## JVM options

Anything before `-jar` goes to the JVM, not to the toolkit. Large ontologies mostly need heap:

```bash
java -Xmx4G -jar target/oddtoolkit.jar \
  --generator=java \
  --config-file=src/test/resources/application.yml
```

## Troubleshooting

**`Ignoring unexpected argument: sql`**
You wrote `--generator sql` with a space, or passed a positional argument. Use `--generator=sql`.

**`Unknown or malformed option: --generator (options use --key=value; run --help)`**
The option had no `=`. Same fix as above.

**`Unknown option: --output (run --help)`**
That option does not exist. Check the table above; output paths are configured in the configuration
file, not on the command line.

**`No generator specified. Use --generator=NAME or --generator=all.`**
`--generator` is missing or empty. The help text is printed after the message and nothing runs.

**`Generator 'sqlgenerator' is not available. Available: bikeshed, class, …`**
The name is not in the registry. Names are case-sensitive and match the table above — use `sql`, not
`sqlgenerator` or `SQL`. The message lists the registered names.

**`Configuration file not found: …` followed by a failure while loading the ontology**
The path was wrong, so the toolkit fell back to defaults and ended up with no ontology file to read.
Check the path, then re-run.

**`Unsupported configuration file format: … Supported: .json, .yml, .yaml`**
Rename the file so its extension matches its content.

**`PNG export skipped: Playwright runtime is unavailable …`**
Only the PNG is missing; the `.mmd` file was written. Set
`generators.diagram-generator.export-png: false` to silence it — see [Installation](./installation).
