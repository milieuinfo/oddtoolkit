# Quick Start Guide

This is a quick reference for the new extensibility features. For detailed information, see the full documentation.

## 5-Minute Quick Start

### 1. Build the Project
```bash
mvn clean package
```

### 2. Run with CLI Arguments
```bash
# Show available options
java -jar target/oddtoolkit-*.jar --help

# Run one generator
java -jar target/oddtoolkit-*.jar --generator=class-diagram

# Run all generators
java -jar target/oddtoolkit-*.jar --generator=all

# Run with custom config
java -jar target/oddtoolkit-*.jar --generator=sql --config-file=config.yml
```

### 3. Create a Custom Generator (5 steps)

**Step 1:** Create Java file
```java
// File: src/main/java/com/example/MyGenerator.java
package com.example.oddtoolkit.generator;

import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import org.springframework.stereotype.Component;

@Component
public class MyGenerator extends BaseGenerator {

  @Override
  public String getName() {
    return "my-generator";
  }

  @Override
  public String getDescription() {
    return "My custom generator";
  }

  @Override
  public void generate() throws Exception {
    validate();
    System.out.println("Generating...");
    // Your logic here
  }
}
```

**Step 2:** Build
```bash
mvn clean package
```

**Step 3:** Run
```bash
java -jar target/oddtoolkit-*.jar --generator=my-generator
```

That's it!

## Documentation Map

| Document | Purpose | When to Read |
|----------|---------|--------------|
| **[guide/usage](/guide/usage)** | End-to-end workflow from build to generation | Start here |
| **[guide/configuration](/guide/configuration)** | How configuration is structured and resolved | When preparing config files |
| **[guide/ontology-metadata](/guide/ontology-metadata)** | OWL, Hydra, and semantic metadata support | When designing ontologies |
| **[cli-guide](/cli-guide)** | Complete CLI reference and troubleshooting | Before scripting/automation |
| **[extension-guide](/extension-guide)** | Extension model for custom generators/adapters | When extending toolkit |

## Common Tasks

### Run All Available Generators
```bash
java -jar oddtoolkit.jar --generator=all
```

### Use Configuration File
```bash
# Create config file (config.yml)
ontology:
  ontology-file-path: "path/to/ontology.ttl"
  concepts-file-path: "path/to/concepts.ttl"

# Run with config
java -jar oddtoolkit.jar --config-file=config.yml --generator=sql
```

### Override via Environment Variable
```bash
export ODD_GENERATOR_NAME=class-diagram
export ODD_OUTPUT_PATH=/tmp/output
java -jar oddtoolkit.jar
```

### Custom Generator with Adapter
```java
@Configuration
public class MyConfig {

  @Bean
  public MyAdapter myAdapter() {
    return new MyAdapter();
  }

  @Bean
  public MyGenerator myGenerator(
      OntologyInfo ontologyInfo,
      ConceptSchemeInfo conceptSchemeInfo,
      MyAdapter adapter) {
    return new MyGenerator(
        ontologyInfo,
        conceptSchemeInfo,
        List.of(adapter),
        Map.of("option", "value")
    );
  }
}
```

## New Classes Overview

### Configuration
- `GeneratorRegistry` - Access generators by name
- `AdapterRegistry` - Access adapters by name
- `CliConfiguration` - Parse CLI arguments
- `ConfigurationSourceResolver` - Load YAML/JSON files

### Generation
- `BaseGenerator` - Extend this for custom generators
- `GeneratorRegistrationHelper` - Registers generators on startup
- `GeneratorCliRunner` - CLI entry point

## Configuration Precedence

When values are set in multiple places, this is the priority order:

1. **Command-line args** - Highest priority
   ```bash
   java -jar app.jar --generator=sql --output=/tmp
   ```

2. **Environment variables** - Second priority
   ```bash
   export ODD_GENERATOR_NAME=sql
   java -jar app.jar
   ```

3. **Configuration files** - Third priority
   ```yaml
   # config.yml
   generators:
     sql:
       output-path: /tmp/output
   ```

4. **Code defaults** - Lowest priority

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Generator not found | Check spelling with `--help` |
| File not found | Use absolute path instead of relative |
| Permission denied | `chmod +r filename` |
| Out of memory | `java -Xmx4G -jar app.jar` |
| See debug logs | `java -Dlogging.level.be.vlaanderen.omgeving=DEBUG -jar app.jar` |

## Useful Locations

- Main docs home: `/`
- VitePress config: `docs/.vitepress/config.js`
- Usage guide: `/guide/usage`
- Configuration guide: `/guide/configuration`
- CLI reference: `/cli-guide`
- Extension guide: `/extension-guide`

## Next Steps

1. **Read**: Start with `/guide/usage`
2. **Try**: Build and run `java -jar target/oddtoolkit-*.jar --help`
3. **Configure**: Use `/guide/configuration` as your template
4. **Extend**: Implement custom behavior via `/extension-guide`

## Getting Help

1. **Question about CLI?** -> See `/cli-guide`
2. **How to extend?** -> See `/extension-guide`
3. **Need a complete run-through?** -> See `/guide/usage`
4. **Need config examples?** -> See `/guide/configuration`

---

**For complete documentation, see:**
- [Documentation Home](/)
- [Usage Guide](/guide/usage)
- [Configuration Guide](/guide/configuration)
- [CLI Guide](/cli-guide)
- [Extension Guide](/extension-guide)
