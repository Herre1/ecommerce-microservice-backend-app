// Script para probar la configuración SCM en Jenkins
// INSTRUCCIONES:
// 1. Crea un nuevo Pipeline en Jenkins
// 2. Selecciona "Pipeline script from SCM"
// 3. SCM: Git
// 4. Repository URL: https://github.com/Herre1/ecommerce-microservice-backend-app.git
// 5. Branch: */master
// 6. Script Path: jenkins-scm-test.groovy

pipeline {
    agent any
    
    stages {
        stage('Test SCM Checkout') {
            steps {
                echo 'SCM Checkout funciona correctamente!'
                echo 'Repositorio: https://github.com/Herre1/ecommerce-microservice-backend-app.git'
                echo "Workspace: ${env.WORKSPACE}"
                sh 'pwd'
                sh 'ls -la'
            }
        }
        
        stage('Verify Jenkinsfiles') {
            steps {
                echo 'Verificando Jenkinsfiles...'
                sh 'find . -name "Jenkinsfile*" -type f'
                sh 'ls -la service-discovery/'
            }
        }
        
        stage('Test Environment') {
            steps {
                echo 'Verificando herramientas...'
                sh 'java -version'
                sh 'mvn -version'
                sh 'git --version'
            }
        }
    }
    
    post {
        success {
            echo '🎉 SCM está funcionando correctamente!'
            echo 'Ahora puedes usar "Pipeline script from SCM" para tus otros pipelines.'
        }
        failure {
            echo '❌ Hay un problema con la configuración de SCM.'
        }
    }
} 