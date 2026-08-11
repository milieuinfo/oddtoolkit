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

  options {
    disableConcurrentBuilds()
  }

  environment {
    GH_PAGES_BRANCH         = 'gh-pages'
    GITHUB_REPO             = 'milieuinfo/oddtoolkit'
  }

  stages {

    stage('Setup') {
      steps {
        script {
          if (env.BRANCH_IS_PRIMARY) {
            properties([versions.releaseParameters()])
            if (versions.isRelease()) {
              def currentVersion = maven.version()
              def version = versions.bump(currentVersion)
              git.validateTag(version)
              maven.validateVersion(version)
              env.VERSION = version
            }
          }
        }
      }
    }

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
                    withEnv(['DOCS_BASE=/oddtoolkit/']) {
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
            container('jnlp') {
              script {
                git.withGitAuth {
                  sh '''
                    set -e
                    REPO_URL=$(git config --get remote.origin.url)
                    rm -rf .gh-pages-deploy
                    git clone --depth 1 --branch "$GH_PAGES_BRANCH" "$REPO_URL" .gh-pages-deploy \
                        || git clone --depth 1 "$REPO_URL" .gh-pages-deploy

                    cd .gh-pages-deploy
                    git checkout -B "$GH_PAGES_BRANCH"
                    find . -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +
                    cp -R ../docs/.vitepress/dist/. .

                    git config user.email "$GIT_USER_EMAIL"
                    git config user.name "$GIT_USER_NAME"
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

    stage('Primary branch') {
      when {
        expression { env.BRANCH_IS_PRIMARY }
      }

      stages {

        stage('Maven prepare') {
          when {
            expression { versions.isRelease() }
          }
          steps {
            script {
              maven.goal([
                goal     : 'release:clean release:prepare',
                version  : env.VERSION,
                skipTests: true
              ])
            }
          }
        }

        stage('Maven deploy') {
          when {
            expression { !versions.isRelease() }
          }
          steps {
            script {
              maven.goal([goal: 'deploy', skipTests: true])
            }
          }
        }

        stage('Maven release') {
          when {
            expression { versions.isRelease() }
          }
          steps {
            script {
              maven.goal([
                goal     : 'release:perform',
                version  : env.VERSION,
                skipTests: true
              ])
            }
          }
        }

        stage('GitHub release') {
          when {
            expression { versions.isRelease() }
          }
          steps {
            container('jnlp') {
              script {
                git.withGitAuth {
                  sh '''
                    set -e
                    TAG="v${VERSION}"

                    JAR=""
                    if [ -f target/checkout/target/oddtoolkit.jar ]; then
                      JAR=target/checkout/target/oddtoolkit.jar
                    elif [ -f target/oddtoolkit.jar ]; then
                      JAR=target/oddtoolkit.jar
                    fi
                    if [ -z "$JAR" ]; then
                      echo "No release jar found to attach to the GitHub release"
                      exit 1
                    fi

                    TOKEN="${GITHUB_TOKEN:-${GH_TOKEN:-}}"
                    if [ -z "$TOKEN" ]; then
                      TOKEN=$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill 2>/dev/null | sed -n 's/^password=//p')
                    fi
                    if [ -z "$TOKEN" ]; then
                      echo "Unable to obtain a GitHub token (set GITHUB_TOKEN/GH_TOKEN or configure git credentials)"
                      exit 1
                    fi

                    AUTH="Authorization: token ${TOKEN}"
                    if curl -fsS -H "${AUTH}" "https://api.github.com/repos/${GITHUB_REPO}/releases/tags/${TAG}" >/dev/null 2>&1; then
                      echo "GitHub release ${TAG} already exists"
                    else
                      curl -fsS -X POST -H "${AUTH}" -H "Accept: application/vnd.github+json" \
                        -d "{\"tag_name\":\"${TAG}\",\"name\":\"${TAG}\",\"body\":\"ODDToolkit release ${VERSION}\"}" \
                        "https://api.github.com/repos/${GITHUB_REPO}/releases"
                      echo "GitHub release ${TAG} created"
                    fi

                    RELEASE_ID=$(curl -fsS -H "${AUTH}" "https://api.github.com/repos/${GITHUB_REPO}/releases/tags/${TAG}" \
                      | grep -o '"id": *[0-9][0-9]*' | head -n 1 | grep -o '[0-9][0-9]*')
                    if [ -z "$RELEASE_ID" ]; then
                      echo "Unable to resolve the release id for ${TAG}"
                      exit 1
                    fi

                    curl -fsS -X POST -H "${AUTH}" -H "Content-Type: application/java-archive" \
                      --data-binary "@${JAR}" \
                      "https://uploads.github.com/repos/${GITHUB_REPO}/releases/${RELEASE_ID}/assets?name=oddtoolkit.jar"
                    echo "Attached ${JAR} to GitHub release ${TAG}"
                  '''
                }
              }
            }
          }
        }
      }
    }
  }
}
