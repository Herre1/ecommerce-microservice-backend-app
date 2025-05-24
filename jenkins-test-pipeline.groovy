pipeline {
    agent any
    
    environment {
        SERVICE_NAME = 'service-discovery'
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo "Checking out code for ${SERVICE_NAME}..."
                // Simulamos checkout exitoso
                sh 'echo "Code checked out successfully"'
            }
        }
        
        stage('Build') {
            steps {
                echo "Building ${SERVICE_NAME}..."
                // Simulamos build exitoso
                sh 'echo "Build completed successfully"'
            }
        }
        
        stage('Test') {
            steps {
                echo "Running tests for ${SERVICE_NAME}..."
                // Simulamos tests exitosos
                sh 'echo "All tests passed"'
            }
        }
        
        stage('Package') {
            steps {
                echo "Packaging ${SERVICE_NAME}..."
                // Simulamos packaging exitoso
                sh 'echo "Package created successfully"'
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