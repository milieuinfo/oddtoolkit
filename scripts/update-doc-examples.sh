#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

EXAMPLE_DIR="$ROOT_DIR/docs/examples/riepr"
ONTOLOGY_DIR="$EXAMPLE_DIR/ontology"
OUTPUTS_DIR="$EXAMPLE_DIR/outputs"
LEGACY_EXAMPLES_DIR="$ROOT_DIR/docs/examples/generated"
IMAGES_DIR="$ROOT_DIR/docs/images/generated"
CACHE_DIR="$ROOT_DIR/target/test-cache"
TEST_EXAMPLES_DIR="$ROOT_DIR/src/test/resources/examples"

mkdir -p \
  "$ONTOLOGY_DIR/ns/riepr" \
  "$ONTOLOGY_DIR/id/concept/riepr" \
  "$OUTPUTS_DIR" \
  "$LEGACY_EXAMPLES_DIR" \
  "$IMAGES_DIR"

if [[ -f "$TEST_EXAMPLES_DIR/ns/riepr/riepr.ttl" ]]; then
  cp "$TEST_EXAMPLES_DIR/ns/riepr/riepr.ttl" "$ONTOLOGY_DIR/ns/riepr/riepr.ttl"
fi
if [[ -f "$TEST_EXAMPLES_DIR/id/concept/riepr/riepr.ttl" ]]; then
  cp "$TEST_EXAMPLES_DIR/id/concept/riepr/riepr.ttl" "$ONTOLOGY_DIR/id/concept/riepr/riepr.ttl"
fi

cp "$CACHE_DIR/class-diagram/class-diagram.mmd" "$OUTPUTS_DIR/class-diagram.mmd"
cp "$CACHE_DIR/er-diagram/er-diagram.mmd" "$OUTPUTS_DIR/er-diagram.mmd"
cp "$CACHE_DIR/sql/schema.sql" "$OUTPUTS_DIR/schema.sql"
cp "$CACHE_DIR/shacl/schema.ttl" "$OUTPUTS_DIR/schema.ttl"
cp "$CACHE_DIR/java/Exploitatie.java" "$OUTPUTS_DIR/Exploitatie.java"
cp "$CACHE_DIR/typescript/exploitatie.model.ts" "$OUTPUTS_DIR/exploitatie.model.ts"
cp "$CACHE_DIR/dataframe/frame.json" "$OUTPUTS_DIR/frame.json"
cp "$CACHE_DIR/odcs/contract.json" "$OUTPUTS_DIR/contract.json"
cp "$CACHE_DIR/bikeshed/ontology.bs" "$OUTPUTS_DIR/ontology.bs"
cp "$CACHE_DIR/bikeshed/ontology.html" "$OUTPUTS_DIR/ontology.html"

# Keep the legacy location in sync to avoid breaking existing references.
cp "$OUTPUTS_DIR/class-diagram.mmd" "$LEGACY_EXAMPLES_DIR/class-diagram.mmd"
cp "$OUTPUTS_DIR/er-diagram.mmd" "$LEGACY_EXAMPLES_DIR/er-diagram.mmd"
cp "$OUTPUTS_DIR/schema.sql" "$LEGACY_EXAMPLES_DIR/schema.sql"
cp "$OUTPUTS_DIR/schema.ttl" "$LEGACY_EXAMPLES_DIR/schema.ttl"
cp "$OUTPUTS_DIR/Exploitatie.java" "$LEGACY_EXAMPLES_DIR/Exploitatie.java"
cp "$OUTPUTS_DIR/exploitatie.model.ts" "$LEGACY_EXAMPLES_DIR/exploitatie.model.ts"
cp "$OUTPUTS_DIR/frame.json" "$LEGACY_EXAMPLES_DIR/frame.json"
cp "$OUTPUTS_DIR/contract.json" "$LEGACY_EXAMPLES_DIR/contract.json"
cp "$OUTPUTS_DIR/ontology.bs" "$LEGACY_EXAMPLES_DIR/ontology.bs"
cp "$OUTPUTS_DIR/ontology.html" "$LEGACY_EXAMPLES_DIR/ontology.html"

cp "$CACHE_DIR/class-diagram/class-diagram.png" "$IMAGES_DIR/class-diagram-test.png"
cp "$CACHE_DIR/er-diagram/er-diagram.png" "$IMAGES_DIR/er-diagram-test.png"

echo "Documentation examples refreshed in docs/examples/riepr, docs/examples/generated and docs/images/generated."
