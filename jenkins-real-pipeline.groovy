pipeline {
    agent any
    
    tools {
        maven 'Maven-3.8'
        jdk 'JDK-11'
    }
    
    environment {
        SERVICE_NAME = 'service-discovery'
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo "Checking out code for ${SERVICE_NAME}..."
                sh 'pwd && ls -la'
            }
        }
        
        stage('Build') {
            steps {
                echo "Building ${SERVICE_NAME}..."
                dir('service-discovery') {
                    sh 'mvn clean compile -DskipTests -X'
                }
            }
        }
        
        stage('Test') {
            steps {
                echo "Running tests for ${SERVICE_NAME}..."
                dir('service-discovery') {
                    sh 'mvn test'
                }
            }
        }
        
        stage('Package') {
            steps {
                echo "Packaging ${SERVICE_NAME}..."
                dir('service-discovery') {
                    sh 'mvn package -DskipTests'
                }
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline completed successfully! ✅'
        }
        failure {
            echo 'Pipeline failed! ❌'
        }
    }
} 