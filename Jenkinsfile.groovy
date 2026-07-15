@Library('Cumulus@1.2-stable') _

def nodePodSpec = '''
spec:
  containers:
    - name: node
      image: acd-docker.repository.milieuinfo.be/library/node:20-alpine
      command:
        - cat
      tty: true
      resources:
        requests:
          memory: "512Mi"
          cpu: "250m"
        limits:
          memory: "2Gi"
'''

pipeline {

  agent {
    kubernetes {
      inheritFrom 'jenkins-jenkins-agent'
      yaml podBuilder.from([maven.podSpec(25), nodePodSpec])
    }
  }

  environment {
    GH_PAGES_CREDENTIALS_ID = 'github-pages-token'
    GH_PAGES_BRANCH         = 'gh-pages'
  }

  stages {

    stage('CI') {
      when {
        expression { git.notSkipCi() }
      }

      stages {

        stage('Build') {
          parallel {
            stage('Docs (VitePress)') {
              steps {
                container('node') {
                  dir('docs') {
                    sh '''
                      if [ -f package-lock.json ]; then
                        npm ci
                      else
                        npm install
                      fi
                      npm run docs:build
                      touch .vitepress/dist/.nojekyll
                    '''
                  }
                }
              }
              post {
                always {
                  archiveArtifacts artifacts: 'docs/.vitepress/dist/**', allowEmptyArchive: true, fingerprint: true
                }
              }
            }

            stage('Javadocs') {
              steps {
                script {
                  maven.goal([
                    goal     : 'javadoc:javadoc',
                    extraArgs: '-DskipTests'
                  ])
                }
              }
              post {
                always {
                  archiveArtifacts artifacts: 'target/reports/apidocs/**', allowEmptyArchive: true, fingerprint: true
                }
              }
            }
          }
        }

        stage('Deploy docs to GitHub Pages') {
          when {
            branch 'main'
          }
          steps {
            container('node') {
              withCredentials([usernamePassword(
                credentialsId: env.GH_PAGES_CREDENTIALS_ID,
                usernameVariable: 'GIT_USER',
                passwordVariable: 'GIT_TOKEN'
              )]) {
                sh '''
                  set -e
                  REPO_URL=$(git config --get remote.origin.url | sed -E "s#https://##" | sed -E "s#.*@##")
                  rm -rf .gh-pages-deploy
                  git clone --depth 1 --branch "$GH_PAGES_BRANCH" \
                      "https://${GIT_USER}:${GIT_TOKEN}@${REPO_URL}" .gh-pages-deploy \
                      || git clone --depth 1 "https://${GIT_USER}:${GIT_TOKEN}@${REPO_URL}" .gh-pages-deploy

                  cd .gh-pages-deploy
                  git checkout -B "$GH_PAGES_BRANCH"
                  find . -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +
                  cp -R ../docs/.vitepress/dist/. .

                  git config user.email "ci@jenkins.local"
                  git config user.name "Jenkins CI"
                  git add -A
                  if ! git diff --cached --quiet; then
                    git commit -m "docs: deploy from ${BUILD_TAG}"
                    git push origin "$GH_PAGES_BRANCH"
                  else
                    echo "No changes to deploy"
                  fi
                '''
              }
            }
          }
        }
      }
    }
  }
}
