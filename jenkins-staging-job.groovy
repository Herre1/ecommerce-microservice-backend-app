/*
 * ===============================================
 * JENKINS JOB - STAGING DEPLOYMENT
 * Sistema E-commerce - Taller 2 - Paso 4
 * ===============================================
 * 
 * Este job ejecuta el pipeline de staging que:
 * 1. Toma artefactos de desarrollo
 * 2. Los despliega en ambiente staging
 * 3. Ejecuta pruebas específicas de staging
 * 4. Prepara para promoción a producción
 */

// Configuración del Job
pipelineJob('ecommerce-staging-deployment') {
    description('''
Pipeline de Despliegue Staging - E-commerce Microservices

Este pipeline:
- Despliega servicios en ambiente staging
- Ejecuta pruebas de smoke e integración
- Valida readiness para producción
- Provee gate de aprobación manual

Parte del Taller 2 - Paso 4 (Stage Pipelines)
    ''')
    
    // Configuración de parámetros
    parameters {
        choiceParam('DEPLOY_ENVIRONMENT', ['staging-auto', 'staging-manual', 'staging-rollback'], 'Tipo de despliegue a staging')
        stringParam('BUILD_NUMBERS', 'latest', 'Números de build específicos (service1:123,service2:124 o "latest")')
        booleanParam('RUN_SMOKE_TESTS', true, 'Ejecutar pruebas de smoke en staging')
        booleanParam('RUN_INTEGRATION_TESTS', true, 'Ejecutar pruebas de integración en staging')
        booleanParam('AUTO_PROMOTE_TO_PROD', false, 'Promoción automática a producción si todas las pruebas pasan')
    }
    
    // Configuración de triggers
    triggers {
    }
    
    // Configuración de propiedades
    properties {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
        disableConcurrentBuilds()
        
        // Configuración de timeout
        timeout {
            absolute(30) // 30 minutos
        }
    }
    
    // Definición del pipeline
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/Herre1/ecommerce-microservice-backend-app.git')
                        credentials('github-token')
                    }
                    branch('master')
                }
            }
            scriptPath('pipeline-scripts/ecommerce-staging-pipeline.groovy')
        }
    }
}

// Job adicional para mostrar estado de staging
pipelineJob('ecommerce-staging-status') {
    description('''
Estado del Ambiente Staging

Muestra el estado actual de todos los servicios en staging:
• Servicios ejecutándose
• Estado de salud
• Endpoints disponibles
• Logs recientes
    ''')
    
    definition {
        cps {
            script('''
pipeline {
    agent any
    
    stages {
        stage('Staging Status Check') {
            steps {
                script {
                    echo "Verificando estado de ambiente staging..."
                    
                    // Verificar contenedores
                    sh '''
                        echo "Contenedores staging ejecutándose:"
                        docker ps --filter "name=staging" --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}" || echo "No hay contenedores staging"
                    '''
                    
                    // Verificar endpoints
                    def endpoints = [
                        'Service Discovery': 'http://localhost:90061/actuator/health',
                        'User Service': 'http://localhost:90080/actuator/health',
                        'Product Service': 'http://localhost:90081/actuator/health',
                        'Order Service': 'http://localhost:90082/actuator/health',
                        'Payment Service': 'http://localhost:90083/actuator/health',
                        'Shipping Service': 'http://localhost:90084/actuator/health'
                    ]
                    
                    echo "Verificando endpoints staging:"
                    endpoints.each { serviceName, url ->
                        try {
                            sh "curl -f -s '${url}' > /dev/null"
                            echo "OK: ${serviceName}: UP"
                        } catch (Exception e) {
                            echo "ERROR: ${serviceName}: DOWN"
                        }
                    }
                }
            }
        }
        
        stage('Recent Logs') {
            steps {
                script {
                    sh '''
                        if [ -f "staging-deployment/docker-compose-staging.yml" ]; then
                            echo "Logs recientes de staging:"
                            docker-compose -f staging-deployment/docker-compose-staging.yml logs --tail=20
                        else
                            echo "No hay logs de staging disponibles"
                        fi
                    '''
                }
            }
        }
    }
    
    post {
        always {
            echo "Verificación de estado staging completada"
        }
    }
}
            ''')
        }
    }
}

// Configuración adicional para views
listView('Staging Pipeline') {
    description('Pipeline de Staging - Taller 2')
    jobs {
        name('ecommerce-staging-deployment')
        name('ecommerce-staging-status')
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

// Crear carpeta para organizar jobs de staging
folder('staging') {
    description('Jobs relacionados con el ambiente de staging')
}

// Job para limpiar ambiente staging
pipelineJob('staging/cleanup-staging-environment') {
    description('Limpieza de ambiente staging')
    
    definition {
        cps {
            script('''
pipeline {
    agent any
    
    parameters {
        booleanParam('CONFIRM_CLEANUP', false, 'Confirmar limpieza de ambiente staging')
        booleanParam('REMOVE_IMAGES', false, 'Eliminar también imágenes Docker staging')
    }
    
    stages {
        stage('Cleanup Confirmation') {
            when {
                not { params.CONFIRM_CLEANUP }
            }
            steps {
                error("Limpieza cancelada: parámetro CONFIRM_CLEANUP debe ser true")
            }
        }
        
        stage('Stop Staging Services') {
            steps {
                sh '''
                    echo "Deteniendo servicios staging..."
                    docker-compose -f staging-deployment/docker-compose-staging.yml down || true
                    echo "Servicios staging detenidos"
                '''
            }
        }
        
        stage('Remove Staging Images') {
            when {
                expression { params.REMOVE_IMAGES }
            }
            steps {
                sh '''
                    echo "Eliminando imágenes staging..."
                    docker images | grep "\\-staging" | awk '{print $3}' | xargs -r docker rmi -f || true
                    echo "Imágenes staging eliminadas"
                '''
            }
        }
        
        stage('Cleanup Files') {
            steps {
                sh '''
                    echo "Limpiando archivos temporales..."
                    rm -rf staging-deployment staging-logs staging-configs || true
                    echo "Archivos temporales eliminados"
                '''
            }
        }
        
        stage('System Cleanup') {
            steps {
                sh '''
                    echo "Limpieza del sistema Docker..."
                    docker system prune -f || true
                    echo "Sistema Docker limpio"
                '''
            }
        }
    }
    
    post {
        success {
            echo "Limpieza de staging completada exitosamente"
        }
        failure {
            echo "Error en limpieza de staging"
        }
    }
}
            ''')
        }
    }
} 