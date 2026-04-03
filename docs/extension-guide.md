# Extension Guide

This guide shows how to add custom generators and adapters in the current bootstrap-based architecture.

## Architecture overview

At runtime the flow is:

1. `OddtoolkitApplication` starts the process
2. `OddtoolkitBootstrap` loads and binds configuration
3. Adapters are created and filtered (enabled/disabled)
4. Generators are created with selected adapters
5. `GeneratorCliRunner` executes the chosen generator(s)

There is no Spring container in this project; registration is done in code.

## Add a custom generator

### 1) Create a generator class

Create a class that extends `BaseGenerator`.

```java
package be.vlaanderen.omgeving.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.adapter.AbstractAdapter;
import be.vlaanderen.omgeving.oddtoolkit.model.ConceptSchemeInfo;
import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;
import java.util.List;

public class MarkdownSummaryGenerator extends BaseGenerator {

  public MarkdownSummaryGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      List<AbstractAdapter<?>> adapters) {
    super(ontologyInfo, conceptSchemeInfo, adapters);
  }

  @Override
  public String getName() {
    return "markdown-summary";
  }

  @Override
  public void run() {
    super.run();
    String content = "# Classes\n\nTotal: " + getOntologyClasses().size();
    saveToFile("target/summary.md", content);
  }
}
```

### 2) Wire it in `GeneratorConfiguration`

Add a factory method in `GeneratorConfiguration` similar to existing generators.

### 3) Register it in `OddtoolkitBootstrap`

Instantiate the generator and register it in `DefaultGeneratorRegistry`:

```java
registry.register(markdownSummaryGenerator.getName(), markdownSummaryGenerator);
```

### 4) Run it

```bash
java -jar target/oddtoolkit.jar --generator=markdown-summary --config-file=config.yml
```

## Add a custom adapter

### 1) Create adapter class

Adapters extend `AbstractAdapter<T>` where `T` is an `AbstractInfo` subtype.

```java
package be.vlaanderen.omgeving.oddtoolkit.adapter;

import be.vlaanderen.omgeving.oddtoolkit.model.OntologyInfo;

public class OntologyAuditAdapter extends AbstractAdapter<OntologyInfo> {

  public OntologyAuditAdapter() {
    super(OntologyInfo.class);
  }

  @Override
  public OntologyInfo adapt(OntologyInfo info) {
    // Add enrichment/validation logic here.
    return info;
  }
}
```

### 2) Add it to adapter creation

Register it in `OddtoolkitBootstrap#createAdapterBeans` using a stable key:

```java
allAdapters.put("ontology-audit", new OntologyAuditAdapter());
```

### 3) Make it optional via config (optional)

If needed, annotate with `@ConditionalOnConfigProperty` or rely on:

```yaml
adapters:
  ontology-audit:
    enabled: true
```

### 4) Select it per generator

```yaml
generators:
  class-diagram:
    adapters:
      - "ontology-load"
      - "ontology-audit"
      - "ontology-class-extract"
```

## Best practices

- Keep adapters focused on data extraction/transformation.
- Keep generators focused on rendering/exporting output.
- Use deterministic ordering in outputs where possible.
- Add tests for new adapter behavior and generator output.
- Prefer config toggles over hardcoded behavior switches.

## Testing extensions

Run targeted tests while implementing:

```bash
./mvnw -Dtest='*GeneratorTest,*AdapterTest' test
```

Run full suite before merging:

```bash
./mvnw test
```
