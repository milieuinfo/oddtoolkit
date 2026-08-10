<script setup>
import DefaultTheme from 'vitepress/theme'
import { withBase } from 'vitepress'
import { computed, onMounted, ref } from 'vue'

const { Layout } = DefaultTheme

const GITHUB_REPO = 'https://github.com/milieuinfo/oddtoolkit'

// VitePress does not expose the query string on `useRoute()`, so we read it from
// the URL once the page has mounted (client-only, avoids SSR/hydration mismatch).
const isMounted = ref(false)
const hasSemantics2026 = ref(false)

onMounted(() => {
  hasSemantics2026.value = new URLSearchParams(window.location.search).has('semantics2026')
  isMounted.value = true
})

const showLanding = computed(() => isMounted.value && hasSemantics2026.value)
</script>

<template>
  <div v-if="showLanding" class="semantics-landing">
    <div class="landing-card">
      <p class="badge">SEMANTiCS 2026 &middot; Ghent</p>
      <h1>ODDToolkit</h1>
      <p class="tagline">Ontology Driven Design Toolkit</p>
      <p class="subtitle">
        An open-source toolkit that bridges the developer gap in Ontology Driven Design:
        it turns an OWL ontology into SQL schemas, code models, SHACL shapes, diagrams and
        documentation &mdash; so implementation artefacts cannot drift from the ontology.
      </p>

      <div class="actions">
        <a class="btn primary" :href="GITHUB_REPO" target="_blank" rel="noopener">
          View source on GitHub
        </a>
        <a class="btn" :href="withBase('/POSTER_MEDIA_20.pdf')" target="_blank" rel="noopener">
          Poster (PDF)
        </a>
        <a class="btn" :href="withBase('/')">Full documentation</a>
      </div>

      <div class="authors">
        <h2>Authors</h2>
        <ul>
          <li>
            <strong>Dr. Maxim Van de Wynckel</strong>
            <span class="affil">Department of Environment and Spatial Development, Flemish Government, Belgium</span>
            <a class="orcid" :href="'https://orcid.org/0000-0003-0314-7107'" target="_blank" rel="noopener">
              ORCID: 0000-0003-0314-7107
            </a>
          </li>
          <li>
            <strong>Geert Van Haute</strong>
            <span class="affil">Department of Environment and Spatial Development, Flemish Government, Belgium</span>
            <a class="orcid" :href="'https://orcid.org/0009-0002-9836-8139'" target="_blank" rel="noopener">
              ORCID: 0009-0002-9836-8139
            </a>
          </li>
        </ul>
      </div>
    </div>
  </div>

  <Layout v-else />
</template>

<style scoped>
.semantics-landing {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: radial-gradient(1200px 500px at 50% -10%, rgba(99, 102, 241, 0.15), transparent),
    linear-gradient(180deg, #fafafa, #f1f3f5);
}

.landing-card {
  max-width: 720px;
  width: 100%;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(17, 24, 39, 0.12);
  padding: 48px;
  text-align: center;
}

.badge {
  display: inline-block;
  margin: 0 0 12px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #4f46e5;
  background: #eef2ff;
}

h1 {
  margin: 0;
  font-size: 40px;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.tagline {
  margin: 8px 0 0;
  font-size: 18px;
  font-weight: 600;
  color: #4b5563;
}

.subtitle {
  margin: 16px auto 0;
  max-width: 560px;
  font-size: 15px;
  line-height: 1.6;
  color: #6b7280;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-top: 28px;
}

.btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 18px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #ffffff;
  color: #111827;
  font-weight: 500;
  text-decoration: none;
  transition: border-color 0.15s, background-color 0.15s;
}

.btn:hover {
  border-color: #9ca3af;
}

.btn.primary {
  border-color: #4f46e5;
  background: #4f46e5;
  color: #ffffff;
}

.btn.primary:hover {
  background: #4338ca;
}

.authors {
  margin-top: 40px;
  padding-top: 28px;
  border-top: 1px solid #e5e7eb;
  text-align: left;
}

.authors h2 {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
}

.authors ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 20px;
}

.authors li {
  display: grid;
  gap: 4px;
}

.authors strong {
  font-size: 16px;
}

.affil {
  font-size: 13px;
  color: #6b7280;
}

.orcid {
  font-size: 13px;
  color: #4f46e5;
  text-decoration: none;
}

.orcid:hover {
  text-decoration: underline;
}
</style>
