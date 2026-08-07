import { execSync } from 'node:child_process'
import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'

const ORG = 'milieuinfo'
const REPO = 'oddtoolkit'

// VitePress resolves the "last updated" timestamp by running `git log` per page.
// The Jenkins node container (node:20-alpine) has no git, so only enable the
// feature when git is actually available (local dev / GitHub Actions).
function gitAvailable() {
  try {
    execSync('git --version', { stdio: 'ignore' })
    return true
  } catch {
    return false
  }
}

function normalizeBasePath(value) {
  if (!value || value.trim() === '') {
    return '/'
  }
  const withLeadingSlash = value.startsWith('/') ? value : `/${value}`
  return withLeadingSlash.endsWith('/') ? withLeadingSlash : `${withLeadingSlash}/`
}

// Single source of truth for the deployed base path. CI sets DOCS_BASE explicitly;
// GitHub Actions falls back to the repository name; local builds use the repo name
// so `docs:preview` matches what GitHub Pages serves.
function detectBasePath() {
  if (process.env.DOCS_BASE) {
    return normalizeBasePath(process.env.DOCS_BASE)
  }

  if (process.env.GITHUB_ACTIONS === 'true' && process.env.GITHUB_REPOSITORY) {
    const repositoryName = process.env.GITHUB_REPOSITORY.split('/')[1]
    if (repositoryName) {
      return normalizeBasePath(repositoryName)
    }
  }

  return `/${REPO}/`
}

export default withMermaid(defineConfig({
  title: 'ODDToolkit',
  description: 'Documentation for the Ontology Driven Design Toolkit',
  base: detectBasePath(),
  cleanUrls: true,
  lastUpdated: gitAvailable(),

  // paper/ and poster/ hold publication sources, examples/ holds generator fixtures
  // and mermaid include-partials. None of them are pages.
  srcExclude: ['**/README.md', 'paper/**', 'poster/**', 'examples/**/*.md'],

  markdown: {
    languageAlias: {
      mmd: 'mermaid',
      bs: 'markdown',
      bikeshed: 'markdown',
      ttl: 'turtle'
    }
  },

  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/introduction' },
      { text: 'CLI', link: '/guide/cli' },
      { text: 'Configuration', link: '/guide/configuration' },
      { text: 'Examples', link: '/guide/examples' }
    ],
    sidebar: [
      {
        text: 'Getting Started',
        items: [
          { text: 'Introduction', link: '/guide/introduction' },
          { text: 'Installation', link: '/guide/installation' },
          { text: 'Quick Start', link: '/guide/quickstart' },
          { text: 'License', link: '/guide/license' }
        ]
      },
      {
        text: 'Using the toolkit',
        items: [
          { text: 'CLI Reference', link: '/guide/cli' },
          { text: 'Configuration Reference', link: '/guide/configuration' },
          { text: 'Generators', link: '/guide/generators' },
          { text: 'Adapters', link: '/guide/adapters' },
          { text: 'Ontology & Metadata', link: '/guide/ontology-metadata' }
        ]
      },
      {
        text: 'Reference',
        items: [
          { text: 'Generated Examples', link: '/guide/examples' },
          { text: 'Architecture', link: '/guide/architecture' },
          { text: 'Extending', link: '/guide/extending' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: `https://github.com/${ORG}/${REPO}` }
    ],
    search: {
      provider: 'local'
    },
    editLink: {
      pattern: `https://github.com/${ORG}/${REPO}/edit/main/docs/:path`,
      text: 'Edit this page on GitHub'
    },
    footer: {
      message: 'Released under the GNU General Public License v3.0.'
    }
  }
}))
