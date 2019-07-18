properties([
        [$class: 'CopyArtifactPermissionProperty', projectNames: '*']
])

def registry = 'docker-bamboo-repo.ned.ru'
def imageName = '/load/mock-boot'
def branch = env.BRANCH_NAME.replace('bugfix/', '').replace('feature/', '').replace('\\', '.').replace('/', '.')

def version
def tag

pipeline {
    agent {
        label 'Slave'
    }

    options {
        buildDiscarder(logRotator(
            daysToKeepStr: '10',
            numToKeepStr: '5',
            artifactDaysToKeepStr: '10',
            artifactNumToKeepStr: '5'))
    }

    stages {
        stage('Build') {
            steps {
                withEnv(['LANG=ru_RU.UTF-8']) {
                    script {
                        version = sh(
                                script: './gradlew getVersion --q',
                                returnStdout: true
                        ).trim() + "." + env.BUILD_NUMBER
                        tag = "$branch.$version"

                        manager.addShortText("${tag}", 'white', 'LimeGreen', "4px", 'LimeGreen')
                        sh "./gradlew clean build"
                        sh "cp build/libs/mock-boot-0.0.1.jar docker/"
                   }
                }
            }
        }

        stage('Docker build and publish') {
            steps {
                withDockerRegistry(credentialsId: 'eva-registry', url: 'https://docker-bamboo-repo.ned.ru') {
                    script {
                        dir("docker") {
                            sh "docker build --rm -t ${registry}${imageName}:latest ."
                            sh "docker push ${registry}${imageName}:latest"
                        }
                    }
                }
            }
        }
    }

    post {
         always {
             echo 'Finished and cleanup'
             deleteDir() // clean up workspace
         }
         success {
             script {
                 currentBuild.result = 'SUCCESS'
             }
         }
         failure {
             script {
                 currentBuild.result = 'FAILED'
             }
         }
    }
}