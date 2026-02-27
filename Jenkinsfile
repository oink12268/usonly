pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'oink12268/usonly'
        DOCKER_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Clone App') {
            steps {
                git branch: 'main',
                    credentialsId: 'github',
                    url: 'https://github.com/oink12268/usonly.git'
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh """
                        docker login -u ${USER} -p ${PASS}
                        DOCKER_BUILDKIT=1 docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                }
            }
        }

        stage('Update Helm') {
            steps {
                dir('helm-repo') {
                    git branch: 'main',
                        credentialsId: 'github',
                        url: 'https://github.com/oink12268/usonly-helm.git'

                    withCredentials([usernamePassword(credentialsId: 'github', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        sh """
                            sed -i 's/tag: .*/tag: "${DOCKER_TAG}"/' charts/usonly/values.yaml

                            git config user.email "jenkins@usonly.com"
                            git config user.name "jenkins"
                            git add .
                            git commit -m "Update image tag to ${DOCKER_TAG}" || echo "No changes to commit"
                            git push https://${GIT_USER}:${GIT_PASS}@github.com/oink12268/usonly-helm.git main
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "배포 성공 - image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
        }
        failure {
            echo "배포 실패 - Build #${BUILD_NUMBER}"
        }
    }
}
