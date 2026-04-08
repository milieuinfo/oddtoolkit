# ODDToolkit Documentation

This folder contains the VitePress documentation site.

## Main pages

- `index.md` - landing page
- `guide/usage.md` - end-to-end usage flow
- `guide/configuration.md` - configuration reference
- `guide/ontology-metadata.md` - ontology and metadata details
- `cli-guide.md` - CLI command reference
- `extension-guide.md` - extension points and wiring
- `QUICKSTART.md` - fast start commands
- `guide/generated-examples.md` - screenshots and snippets generated from tests

## Local docs build

```bash
cd docs
npm install
npm run docs:dev
```

Production build:

```bash
cd docs
npm run docs:build
```
