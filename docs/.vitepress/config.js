import { defineConfig } from 'vitepress'

function normalizeBasePath(value) {
  if (!value || value.trim() === '') {
    return '/'
  }
  const withLeadingSlash = value.startsWith('/') ? value : `/${value}`
  return withLeadingSlash.endsWith('/') ? withLeadingSlash : `${withLeadingSlash}/`
}

function detectBasePath() {
  if (process.env.DOCS_BASE) {
    return normalizeBasePath(process.env.DOCS_BASE)
  }

  // For GitHub Pages: milieuinfo.github.io/oddtoolkit/
  if (process.env.GITHUB_PAGES === 'true') {
    return '/oddtoolkit/'
  }

  if (process.env.GITHUB_ACTIONS === 'true' && process.env.GITHUB_REPOSITORY) {
    const repositoryName = process.env.GITHUB_REPOSITORY.split('/')[1]
    if (repositoryName) {
      return normalizeBasePath(repositoryName)
    }
  }

  // Default to /oddtoolkit/ for GitHub Pages deployment
  return '/oddtoolkit/'
}

const base = detectBasePath()

export default defineConfig({
  title: 'ODDToolkit',
  description: 'Documentation for the Ontology Driven Design Toolkit',
  base,
  cleanUrls: true,
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/usage' },
      { text: 'Examples', link: '/guide/generated-examples' },
      { text: 'CLI', link: '/cli-guide' },
      { text: 'Extension', link: '/extension-guide' },
      { text: 'Quickstart', link: '/QUICKSTART' }
    ],
    sidebar: [
      {
        text: 'Get Started',
        items: [
          { text: 'Overview', link: '/' },
          { text: 'Usage', link: '/guide/usage' },
          { text: 'Configuration', link: '/guide/configuration' },
          { text: 'Generated Examples', link: '/guide/generated-examples' },
          { text: 'Ontology & Metadata', link: '/guide/ontology-metadata' }
        ]
      },
      {
        text: 'Reference',
        items: [
          { text: 'CLI Guide', link: '/cli-guide' },
          { text: 'Extension Guide', link: '/extension-guide' },
          { text: 'Quickstart', link: '/QUICKSTART' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/maximvdw/oddtoolkit' }
    ],
    search: {
      provider: 'local'
    }
  }
})
