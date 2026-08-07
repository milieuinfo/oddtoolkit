# Installation

ODDToolkit is a plain Java application. You build it from source and run the resulting JAR.
There is no published binary and no package-manager install.

## Prerequisites

| Requirement | Notes |
|---|---|
| JDK 21 | The build compiles with `--release 21`. A newer JDK works only if it still accepts release 21. |
| Maven | Optional. The repository ships the Maven wrapper (`./mvnw`, `mvnw.cmd` on Windows), so you do not need Maven installed. |
| Git | To clone the repository. |

Check your JDK first:

```bash
java -version
```

## Clone

```bash
git clone https://github.com/milieuinfo/oddtoolkit.git
cd oddtoolkit
```

## Build

From the repository root:

```bash
./mvnw clean package
```

On Windows:

```bat
mvnw.cmd clean package
```

The build runs the unit tests. To skip them:

```bash
./mvnw clean package -DskipTests
```

## What the build produces

| Path | What it is |
|---|---|
| `target/oddtoolkit.jar` | The shaded, executable JAR. This is the one you run. |
| `target/original-oddtoolkit.jar` | The pre-shaded artifact. Not runnable on its own. |
| `target/lib/` | The runtime dependencies, copied out for the non-shaded JAR. |

## Verify

```bash
java -jar target/oddtoolkit.jar --help
```

You should see the usage text, including the list of registered generator names.
If you get `no main manifest attribute`, you are running `original-oddtoolkit.jar` — use
`target/oddtoolkit.jar`.

Next, run your first generator in [Quick Start](./quickstart), or read the full option list in the
[CLI reference](./cli).

## PNG export and Playwright

The diagram generators (`class-diagram`, `er-diagram`) write a Mermaid `.mmd` file and, by default,
also render it to a `.png` next to it. Rendering uses Playwright, which drives a headless browser.

The browser is not part of the JAR — Playwright fetches it on first use, which needs network access
and takes a while. If the Playwright runtime is unavailable, the toolkit logs a warning, skips the
PNG and keeps the `.mmd` file; the run does not fail.

To skip PNG export entirely, add this to your configuration file:

```yaml
generators:
  diagram-generator:
    export-png: false
```

See [Generators](./generators) for the rest of the diagram options.
