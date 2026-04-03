# Quick Start

Get from clone to generated output in a few commands.

## 1) Build

```bash
./mvnw clean package
```

## 2) Inspect CLI options

```bash
java -jar target/oddtoolkit.jar --help
```

## 3) Generate a class diagram

```bash
java -jar target/oddtoolkit.jar \
  --generator=class-diagram \
  --config-file=src/test/resources/application.yml
```

## 4) Generate everything

```bash
java -jar target/oddtoolkit.jar \
  --generator=all \
  --config-file=src/test/resources/application.yml
```

## 5) Useful next commands

Generate SQL only:

```bash
java -jar target/oddtoolkit.jar --generator=sql --config-file=src/test/resources/application.yml
```

Generate Java model only:

```bash
java -jar target/oddtoolkit.jar --generator=java --config-file=src/test/resources/application.yml
```

## Where outputs go

Output paths come from your config file (`generators.*`).
A default example is available at `src/test/resources/application.yml`.

## Next reads

- [Usage Guide](/guide/usage)
- [Configuration Guide](/guide/configuration)
- [CLI Guide](/cli-guide)
- [Extension Guide](/extension-guide)
