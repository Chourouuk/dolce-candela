// =====================================================================
//  Dolce Candela - Pipeline CI (jalon J1)
//  Checkout → Build → Tests unitaires → Tests d'intégration → Package
//  Toute étape en échec arrête le pipeline (les étapes suivantes ne s'exécutent pas).
//  Les étapes J2 → J4 (SonarQube, Nexus, Docker, Kubernetes, Trivy...) seront ajoutées à la suite.
// =====================================================================

// Fonctionne avec un agent Jenkins Linux (sh) ou Windows (bat)
def mvn(String args) {
    if (isUnix()) {
        sh "chmod +x mvnw && ./mvnw -B -ntp ${args}"
    } else {
        bat "mvnw.cmd -B -ntp ${args}"
    }
}

pipeline {
    agent any

    // Si Jenkins n'a pas Java 21 par défaut : déclarer un JDK 21 nommé "jdk21"
    // dans Administrer Jenkins → Tools, puis décommenter ce bloc.
    // tools {
    //     jdk 'jdk21'
    // }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    // Job "Pipeline" simple : déclenchement par le webhook GitHub (plugin GitHub).
    // Pour un job "Multibranch Pipeline", ce bloc est inutile : le webhook est géré par la source de branches.
    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def shortCommit = env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : 'local'
                    currentBuild.displayName = "#${env.BUILD_NUMBER} - ${env.BRANCH_NAME ?: 'main'} - ${shortCommit}"
                }
            }
        }

        stage('Build') {
            steps {
                mvn 'clean compile'
            }
        }

        stage('Tests unitaires') {
            steps {
                mvn 'test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: false
                }
            }
        }

        stage("Tests d'intégration") {
            steps {
                // -DskipUTs=true : les tests unitaires ont déjà tourné au stage précédent
                mvn 'verify -DskipUTs=true'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/TEST-*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Package') {
            steps {
                // Le .jar a été produit par "verify" : on l'archive avec une empreinte pour la traçabilité
                archiveArtifacts artifacts: 'target/dolce-candela-backend-*.jar', fingerprint: true
                archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
            }
        }
    }

    post {
        success {
            echo 'Pipeline réussi : build + tests unitaires + tests d\'intégration OK.'
        }
        failure {
            echo 'Pipeline en échec : voir le stage en rouge et les rapports de tests.'
        }
    }
}
