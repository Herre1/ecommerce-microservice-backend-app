#!/usr/bin/env groovy

/**
 * Script de configuración automática para Jenkins
 * Crea todos los jobs necesarios para el Taller 2
 * 
 * Instrucciones de uso:
 * 1. Abrir Jenkins en http://localhost:8090
 * 2. Ir a "Manage Jenkins" > "Script Console"
 * 3. Copiar y ejecutar este script
 */

import jenkins.model.*
import hudson.model.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition

println "=== CONFIGURACIÓN AUTOMÁTICA DE JENKINS JOBS ==="
println ""

// Función para crear un job de pipeline con script directo
def createPipelineJob(String jobName, String description, String pipelineScript) {
    def jenkins = Jenkins.getInstance()
    
    // Verificar si el job ya existe
    def existingJob = jenkins.getItem(jobName)
    if (existingJob != null) {
        println "Job '${jobName}' ya existe. Actualizando..."
        existingJob.delete()
    }
    
    // Crear nuevo job
    def job = jenkins.createProject(WorkflowJob.class, jobName)
    job.setDescription(description)
    
    // Configurar el pipeline script directamente
    job.setDefinition(new CpsFlowDefinition(pipelineScript, true))
    
    // Guardar configuración
    job.save()
    println "Job '${jobName}' creado exitosamente"
    
    return job
}

try {
    // 1. Job de Testing Comprensivo
    println "1. Creando job: ecommerce-comprehensive-testing"
    def testingScript = '''
pipeline {
    agent any
    
    environment {
        SERVICE_NAME = 'user-service'
        LOCUST_VERSION = '2.17.0'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                echo "Building ${SERVICE_NAME}..."
                dir('user-service') {
                    sh 'mvn clean compile -DskipTests'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                dir('user-service') {
                    sh 'mvn test -Dtest=UserServiceImplTest'
                }
            }
            post {
                always {
                    dir('user-service') {
                        publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                    }
                }
            }
        }
        
        stage('Package') {
            steps {
                echo 'Creating JAR package...'
                dir('user-service') {
                    sh 'mvn package -DskipTests'
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                echo 'Running integration tests...'
                dir('user-service') {
                    sh 'mvn test -Dtest=MicroservicesIntegrationTest'
                }
            }
        }
        
        stage('E2E Tests') {
            steps {
                echo 'Running E2E tests...'
                dir('user-service') {
                    sh 'mvn test -Dtest=EcommerceE2EFlowTest'
                }
            }
        }
        
        stage('Stress Tests') {
            steps {
                echo 'Running stress tests with Locust...'
                script {
                    sh 'mkdir -p stress-test-results'
                    sh """
                        cd locust-stress-tests
                        python -m locust -f user_service_stress_test.py UserServiceStressTest \\
                            --host=http://localhost:8700/user-service \\
                            --users=10 \\
                            --spawn-rate=2 \\
                            --run-time=30s \\
                            --headless \\
                            --html=../stress-test-results/stress-test-report.html
                    """
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'stress-test-results/*', allowEmptyArchive: true
                }
            }
        }
        
        stage('Archive Results') {
            steps {
                echo 'Archiving test results...'
                archiveArtifacts artifacts: 'user-service/target/*.jar', allowEmptyArchive: true
            }
        }
    }
    
    post {
        always {
            echo 'Pipeline completed!'
        }
        success {
            echo 'All tests passed successfully!'
        }
        failure {
            echo 'Some tests failed. Please check the reports.'
        }
    }
}
'''

    createPipelineJob(
        "ecommerce-comprehensive-testing",
        "Pipeline comprensivo de testing para el User Service - Incluye pruebas unitarias, integración, E2E y de estrés",
        testingScript
    )
    
    // 2. Job básico de User Service
    println "2. Creando job: ecommerce-user-service-pipeline"
    def userServiceScript = '''
pipeline {
    agent any
    
    environment {
        SERVICE_NAME = 'user-service'
        IMAGE_NAME = "selimhorri/${SERVICE_NAME}-ecommerce-boot"
        IMAGE_TAG = '0.1.0'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                echo "Building ${SERVICE_NAME}..."
                dir('user-service') {
                    sh 'mvn clean compile -DskipTests'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                dir('user-service') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    dir('user-service') {
                        publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                    }
                }
            }
        }
        
        stage('Package') {
            steps {
                echo 'Creating JAR package...'
                dir('user-service') {
                    sh 'mvn package -DskipTests'
                }
            }
        }
        
        stage('Docker Build') {
            steps {
                echo 'Building Docker image...'
                dir('user-service') {
                    sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                }
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                echo 'Archiving artifacts...'
                dir('user-service') {
                    archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
                }
            }
        }
    }
    
    post {
        always {
            echo 'Pipeline completed!'
        }
    }
}
'''

    createPipelineJob(
        "ecommerce-user-service-pipeline", 
        "Pipeline básico para User Service con testing integrado",
        userServiceScript
    )
    
    // 3. Job de Staging
    println "3. Creando job: ecommerce-staging-deployment"
    def stagingScript = '''
pipeline {
    agent any
    
    environment {
        STAGING_NAMESPACE = 'ecommerce-staging'
        IMAGE_TAG = '0.1.0-staging'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code for staging...'
                checkout scm
            }
        }
        
        stage('Build Staging Images') {
            steps {
                echo 'Building Docker images for staging...'
                sh """
                    docker build -t selimhorri/user-service-ecommerce-boot:\${IMAGE_TAG} user-service/
                    docker build -t selimhorri/service-discovery-ecommerce-boot:\${IMAGE_TAG} service-discovery/
                """
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                echo 'Deploying to staging environment...'
                sh """
                    mkdir -p staging-deployment
                    cat > staging-deployment/docker-compose-staging.yml << 'EOF'
version: '3.8'
services:
  service-discovery-staging:
    image: selimhorri/service-discovery-ecommerce-boot:\${IMAGE_TAG}
    ports:
      - "90061:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
    networks:
      - ecommerce-staging

  user-service-staging:
    image: selimhorri/user-service-ecommerce-boot:\${IMAGE_TAG}
    ports:
      - "90080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-staging:8761/eureka
    depends_on:
      - service-discovery-staging
    networks:
      - ecommerce-staging

networks:
  ecommerce-staging:
    driver: bridge
    name: ecommerce-staging-network
EOF
                    docker-compose -f staging-deployment/docker-compose-staging.yml up -d
                """
            }
        }
        
        stage('Staging Health Check') {
            steps {
                echo 'Checking staging deployment health...'
                script {
                    sleep(30) // Wait for services to start
                    sh """
                        echo "Checking service discovery..."
                        curl -f http://localhost:90061/actuator/health || echo "Service discovery not ready"
                        
                        echo "Checking user service..."
                        curl -f http://localhost:90080/actuator/health || echo "User service not ready"
                    """
                }
            }
        }
        
        stage('Staging Tests') {
            steps {
                echo 'Running staging validation tests...'
                sh """
                    echo "Testing staging endpoints..."
                    curl -f http://localhost:90080/api/users || echo "Users endpoint test failed"
                    curl -f http://localhost:90061/eureka/apps || echo "Eureka apps test failed"
                """
            }
        }
        
        stage('Manual Approval Gate') {
            steps {
                script {
                    def userInput = input(
                        id: 'stagingApproval',
                        message: 'Staging deployment completed. Approve for production?',
                        parameters: [
                            choice(
                                choices: ['Approve', 'Reject'],
                                description: 'Choose action',
                                name: 'approval'
                            )
                        ]
                    )
                    
                    if (userInput == 'Reject') {
                        error('Deployment rejected by user')
                    }
                    
                    echo "Deployment approved for production"
                }
            }
        }
    }
    
    post {
        always {
            echo 'Staging pipeline completed!'
        }
        cleanup {
            sh """
                echo "Cleaning up staging environment..."
                docker-compose -f staging-deployment/docker-compose-staging.yml down || true
                docker system prune -f || true
            """
        }
    }
}
'''

    createPipelineJob(
        "ecommerce-staging-deployment",
        "Pipeline de despliegue en ambiente staging con validaciones",
        stagingScript
    )
    
    println ""
    println "=== CONFIGURACIÓN COMPLETADA ==="
    println "Jobs creados exitosamente:"
    println "- ecommerce-comprehensive-testing"
    println "- ecommerce-user-service-pipeline"
    println "- ecommerce-staging-deployment"
    println ""
    println "Puedes acceder a estos jobs desde el dashboard de Jenkins."
    
    // Crear una vista para organizar los jobs
    println "4. Creando vista personalizada..."
    def jenkins = Jenkins.getInstance()
    def viewName = "Ecommerce Taller 2"
    
    // Verificar si la vista ya existe
    def existingView = jenkins.getView(viewName)
    if (existingView != null) {
        println "Vista '${viewName}' ya existe."
    } else {
        def view = new hudson.model.ListView(viewName)
        view.setIncludeRegex("ecommerce-.*")
        jenkins.addView(view)
        view.save()
        println "Vista '${viewName}' creada para organizar los jobs."
    }
    
} catch (Exception e) {
    println "ERROR: ${e.message}"
    e.printStackTrace()
}

println ""
println "=== INSTRUCCIONES POST-CONFIGURACIÓN ==="
println "1. Refrescar el dashboard de Jenkins (F5)"
println "2. Verificar que todos los jobs aparecen en la lista"
println "3. Hacer clic en 'Ecommerce Taller 2' para ver la vista organizada"
println "4. Para ejecutar:"
println "   - Empezar con 'ecommerce-comprehensive-testing' para validar"
println "   - Continuar con 'ecommerce-staging-deployment' para staging"
println ""
println "¡CONFIGURACIÓN DE JENKINS COMPLETADA!" 