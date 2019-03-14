if (env.BRANCH_NAME == 'master') {
    properties([
        disableConcurrentBuilds(),
    ])
}
pipeline {
    agent { label "devel8" }
    tools {
        maven "maven 3.5"
    }
    environment {
        MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
    triggers {
        pollSCM("H/3 * * * *")
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
    }
    stages {
        stage("build") {
            steps {
                // Fail Early..
                script {
                    if (! env.BRANCH_NAME) {
                        currentBuild.result = Result.ABORTED
                        throw new hudson.AbortException('Job Started from non MultiBranch Build')
                    } else {
                        println(" Building BRANCH_NAME == ${BRANCH_NAME}")
                    }

                }

                sh """
                    mvn -B clean
                    mvn -B install pmd:pmd javadoc:aggregate
                    rm -rf ~/.m2/repository/dk/dbc/solr-performance-test*

                """
            }
        }

        stage("upload") {
            steps {
                script {
                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                        sh """
                            mvn -Dmaven.repo.local=\$WORKSPACE/.repo jar:jar deploy:deploy
                        """
                    }
                }
            }
        }
    }
}
