# CLI Usage Guide

## Overview

The ODD Toolkit can be run as a command-line application to generate code from ontology files. This guide covers how to use the CLI to execute generators with custom configurations.

## Building the Application

```bash
# Build the JAR file with Maven
mvn clean package

# The JAR will be located at: target/oddtoolkit-0.0.1-SNAPSHOT.jar
```

## Running Generators

### Basic Usage

```bash
# Show help
java -jar oddtoolkit.jar --help

# Run with default configuration (from application.yml)
java -jar oddtoolkit.jar
```

### Specifying a Generator

```bash
# Generate class diagram
java -jar oddtoolkit.jar --generator=class-diagram

# Generate SQL
java -jar oddtoolkit.jar --generator=sql

# Generate Java classes
java -jar oddtoolkit.jar --generator=java

# Generate TypeScript types
java -jar oddtoolkit.jar --generator=typescript

# Generate all registered outputs
java -jar oddtoolkit.jar --generator=all
```

### Available Generators

The following generators are available:

| Generator | Name | Description |
|-----------|------|-------------|
| All generators | `all` | Executes all registered generators in one run |
| Class Diagram | `class-diagram` | Generates Mermaid class diagram from ontology |
| Entity-Relationship Diagram | `er-diagram` | Generates Mermaid ER diagram for database design |
| SQL | `sql` | Generates SQL DDL statements |
| Java | `java` | Generates Java POJOs/records from ontology classes |
| TypeScript | `typescript` | Generates TypeScript interfaces and types |
| SHACL | `shacl` | Generates SHACL shapes for validation |
| Class | `class` | Generates abstract class representations |

### Using Configuration Files

You can provide configuration via YAML or JSON files:

```bash
# Using a YAML configuration file
java -jar oddtoolkit.jar --config-file=/path/to/config.yml

# Using a JSON configuration file
java -jar oddtoolkit.jar --config-file=/path/to/config.json

# Combine with generator selection
java -jar oddtoolkit.jar --generator=java --config-file=custom-config.yml
```

#### Configuration File Format (YAML)

```yaml
ontology:
  ontology-file-path: "src/test/resources/examples/ns/riepr/riepr.ttl"
  concepts-file-path: "src/test/resources/examples/id/concept/riepr/riepr.ttl"
  enum-classes:
    - "http://www.w3.org/ns/sosa/Procedure"
    - "http://www.w3.org/ns/adms#Status"
  temporal-properties:
    - "http://purl.org/dc/terms/created"

generators:
  class-diagram:
    adapters:
      - "ontologyLoadAdapter"
      - "ontologyClassExtractAdapter"
```

#### Configuration File Format (JSON)

```json
{
  "ontology": {
    "ontology-file-path": "src/test/resources/examples/ns/riepr/riepr.ttl",
    "concepts-file-path": "src/test/resources/examples/id/concept/riepr/riepr.ttl",
    "enum-classes": [
      "http://www.w3.org/ns/sosa/Procedure"
    ]
  },
  "generators": {
    "class-diagram": {
      "adapters": ["ontologyLoadAdapter", "ontologyClassExtractAdapter"]
    }
  }
}
```

### Overriding Configuration via Command Line

You can override specific configuration values without creating a config file:

```bash
# Override ontology file path
java -jar oddtoolkit.jar --generator=class-diagram \
  --ontology-file=/path/to/custom.ttl

# Override concepts file path
java -jar oddtoolkit.jar --generator=sql \
  --ontology-file=/path/to/ontology.ttl \
  --concepts-file=/path/to/concepts.ttl

# Specify output directory
java -jar oddtoolkit.jar --generator=java --output=/tmp/generated-code
```

### Using Environment Variables

Configure the application using environment variables with the `ODD_` prefix:

```bash
# Set generator via environment variable
export ODD_GENERATOR_NAME=class-diagram
java -jar oddtoolkit.jar

# Override ontology file path
export ODD_ONTOLOGY_FILE=/path/to/custom.ttl
java -jar oddtoolkit.jar --generator=sql

# Multiple variables
export ODD_GENERATOR_NAME=java
export ODD_OUTPUT_PATH=/home/user/output
java -jar oddtoolkit.jar
```

#### Available Environment Variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `ODD_GENERATOR_NAME` | Select generator | `sql` |
| `ODD_CONFIG_FILE` | Configuration file path | `/path/to/config.yml` |
| `ODD_ONTOLOGY_FILE` | Ontology file path | `/path/to/ontology.ttl` |
| `ODD_CONCEPTS_FILE` | Concepts file path | `/path/to/concepts.ttl` |
| `ODD_OUTPUT_PATH` | Output directory | `/tmp/output` |

## Configuration Precedence

Configuration values are resolved in this order (highest to lowest priority):

1. **Command-line arguments** (`--generator=...`, `--ontology-file=...`)
2. **Environment variables** (`ODD_GENERATOR_NAME=...`)
3. **Configuration file** (`application.yml` or `--config-file=...`)
4. **Hardcoded defaults**

This means CLI arguments always override environment variables, which override configuration files.

## Common Use Cases

### Case 1: Generate Class Diagram with Custom Ontology

```bash
java -jar oddtoolkit.jar \
  --generator=class-diagram \
  --ontology-file=/home/user/my-ontology.ttl \
  --concepts-file=/home/user/my-concepts.ttl
```

### Case 2: Generate SQL for Database Schema

```bash
java -jar oddtoolkit.jar \
  --generator=sql \
  --config-file=db-config.yml \
  --output=/tmp/schema
```

### Case 3: Generate Java POJOs from Ontology

```bash
java -jar oddtoolkit.jar \
  --generator=java \
  --output=/home/user/src/generated
```

### Case 4: Batch Processing with Shell Script

```bash
#!/bin/bash

ONTOLOGY_DIR="./ontologies"
OUTPUT_DIR="./generated"
JAR="oddtoolkit.jar"

for ontology in $ONTOLOGY_DIR/*.ttl; do
  echo "Processing: $ontology"
  java -jar $JAR \
    --generator=class-diagram \
    --ontology-file="$ontology" \
    --output="$OUTPUT_DIR/$(basename $ontology .ttl)"
done
```

## Debugging and Logging

The application uses SLF4J for logging. You can control log levels:

```bash
# Enable debug logging
java -Dlogging.level.be.vlaanderen.omgeving=DEBUG \
  -jar oddtoolkit.jar --generator=sql

# Trace Spring Boot configuration
java -Dlogging.level.org.springframework.boot=DEBUG \
  -jar oddtoolkit.jar --generator=java

# Combine multiple log levels
java -Dlogging.level.be.vlaanderen.omgeving=DEBUG \
  -Dlogging.level.org.springframework=INFO \
  -jar oddtoolkit.jar --generator=class-diagram
```

## Troubleshooting

### Generator Not Found

**Error:** `Generator 'xyz' is not available`

**Solution:** Check spelling and use one of the available generators listed with `--help`:
```bash
java -jar oddtoolkit.jar --help
```

### File Not Found

**Error:** `Configuration file not found: /path/to/config.yml`

**Solution:** Verify the file path exists and is readable:
```bash
ls -la /path/to/config.yml
```

### Permission Denied

**Error:** `Cannot read file: /path/to/ontology.ttl (Permission denied)`

**Solution:** Check file permissions:
```bash
chmod +r /path/to/ontology.ttl
```

### Out of Memory

**Error:** `java.lang.OutOfMemoryError: Java heap space`

**Solution:** Increase JVM heap size:
```bash
java -Xmx4G -jar oddtoolkit.jar --generator=java
```

## Next Steps

- See [Extension Guide](/extension-guide) to create custom generators and adapters
- Review `src/test/resources/application.yml` for example configurations

