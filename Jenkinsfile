pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'tu-registro-docker'
        KUBERNETES_NAMESPACE = 'ecommerce'
    }
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    def serviceName = "${env.JOB_NAME}"
                    docker.build("${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER}")
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                sh 'mvn verify -DskipUnitTests'
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def serviceName = "${env.JOB_NAME}"
                    sh """
                        kubectl set image deployment/${serviceName} ${serviceName}=${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER} -n ${KUBERNETES_NAMESPACE}
                    """
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline ejecutado exitosamente!'
        }
        failure {
            echo 'Pipeline falló!'
        }
    }
} 