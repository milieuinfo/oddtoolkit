#!/usr/bin/env bash
#
# Refresh the generated examples shown in the documentation.
#
# The generator tests write their output to target/test-cache. Run them first:
#
#   ./mvnw -Dtest='ClassDiagramGeneratorTest,ERDiagramGeneratorTest,SQLGeneratorTest,\
# ShaclGeneratorTest,JavaGeneratorTest,TypescriptGeneratorTest,DataFrameGeneratorTest,\
# BikeshedGeneratorTest,ODCSGeneratorTest' test
#
# then run this script. The ontology under docs/examples/riepr/ontology is the *input*
# to those tests (see src/test/resources/application.yml); it is never overwritten here.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

EXAMPLE_DIR="$ROOT_DIR/docs/examples/riepr"
OUTPUTS_DIR="$EXAMPLE_DIR/outputs"
IMAGES_DIR="$ROOT_DIR/docs/images/generated"
CACHE_DIR="$ROOT_DIR/target/test-cache"

mkdir -p "$OUTPUTS_DIR" "$IMAGES_DIR"

# source-in-cache -> destination-in-docs
FILES=(
  "class-diagram/class-diagram.mmd:$OUTPUTS_DIR/class-diagram.mmd"
  "er-diagram/er-diagram.mmd:$OUTPUTS_DIR/er-diagram.mmd"
  "sql/schema.sql:$OUTPUTS_DIR/schema.sql"
  "shacl/schema.ttl:$OUTPUTS_DIR/schema.ttl"
  "java/Exploitatie.java:$OUTPUTS_DIR/Exploitatie.java"
  "typescript/exploitatie.model.ts:$OUTPUTS_DIR/exploitatie.model.ts"
  "dataframe/frame.json:$OUTPUTS_DIR/frame.json"
  "odcs/contract.json:$OUTPUTS_DIR/contract.json"
  "bikeshed/ontology.bs:$OUTPUTS_DIR/ontology.bs"
)

# PNG export depends on generators.diagram-generator.export-png (default true), which
# needs a Playwright browser. Missing PNGs are reported but are not fatal.
OPTIONAL_FILES=(
  "class-diagram/class-diagram.png:$IMAGES_DIR/class-diagram-test.png"
  "er-diagram/er-diagram.png:$IMAGES_DIR/er-diagram-test.png"
)

# Preflight: report every missing file at once instead of dying on the first cp.
missing=()
for entry in "${FILES[@]}"; do
  source="$CACHE_DIR/${entry%%:*}"
  [[ -f "$source" ]] || missing+=("$source")
done

if (( ${#missing[@]} > 0 )); then
  echo "error: the following generator outputs are missing from $CACHE_DIR:" >&2
  printf '  %s\n' "${missing[@]}" >&2
  echo >&2
  echo "Run the generator tests first (see the header of this script)." >&2
  exit 1
fi

for entry in "${FILES[@]}"; do
  cp "$CACHE_DIR/${entry%%:*}" "${entry#*:}"
done

for entry in "${OPTIONAL_FILES[@]}"; do
  source="$CACHE_DIR/${entry%%:*}"
  if [[ -f "$source" ]]; then
    cp "$source" "${entry#*:}"
  else
    echo "warning: $source not found; keeping the committed PNG" >&2
  fi
done

# VitePress renders a diagram only from a ```mermaid fence, and its <<< snippet import
# cannot produce one. Emit include-partials that the docs pull in with @include, so the
# rendered diagrams stay in lockstep with the generator output.
for diagram in class-diagram er-diagram; do
  {
    echo '```mermaid'
    cat "$OUTPUTS_DIR/$diagram.mmd"
    echo '```'
  } > "$OUTPUTS_DIR/$diagram.mermaid.md"
done

echo "Documentation examples refreshed in docs/examples/riepr/outputs and docs/images/generated."
