pipeline {
    agent any
    
    environment {
        SERVICE_NAME = 'user-service'
        IMAGE_NAME = "selimhorri/${SERVICE_NAME}-ecommerce-boot"
        IMAGE_TAG = '0.1.0'
        LOCUST_VERSION = '2.17.0'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code from GitHub...'
                git branch: 'master', url: 'https://github.com/Herre1/ecommerce-microservice-backend-app.git'
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
                echo 'Running enhanced unit tests...'
                dir('user-service') {
                    sh 'mvn test'
                    publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                }
            }
            post {
                always {
                    dir('user-service') {
                        publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                        archiveArtifacts artifacts: 'target/surefire-reports/*', allowEmptyArchive: true
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
        
        stage('Start Service for Testing') {
            steps {
                echo 'Starting user service for integration/stress testing...'
                dir('user-service') {
                    script {
                        // Start the service in background for testing
                        sh 'nohup java -jar target/*-v*.jar --spring.profiles.active=test > app.log 2>&1 &'
                        sh 'echo $! > app.pid'
                        
                        // Wait for service to start
                        timeout(time: 2, unit: 'MINUTES') {
                            waitUntil {
                                script {
                                    def result = sh(script: 'curl -s http://localhost:8080/actuator/health || true', returnStdout: true)
                                    return result.contains('"status":"UP"')
                                }
                            }
                        }
                        echo 'Service started successfully!'
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                echo 'Running integration tests...'
                script {
                    // Test service endpoints
                    sh '''
                        echo "Testing User Service endpoints..."
                        
                        # Test health endpoint
                        curl -f http://localhost:8080/actuator/health
                        
                        # Test get all users
                        echo "Testing GET /api/users"
                        curl -f -H "Content-Type: application/json" http://localhost:8080/api/users
                        
                        # Test get user by username (should exist from migration data)
                        echo "Testing GET /api/users/username/admin"
                        curl -f -H "Content-Type: application/json" http://localhost:8080/api/users/username/admin
                        
                        # Test credentials endpoint
                        echo "Testing GET /api/credentials"
                        curl -f -H "Content-Type: application/json" http://localhost:8080/api/credentials
                    '''
                }
            }
        }
        
        stage('Install Locust') {
            steps {
                echo 'Installing Locust for stress testing...'
                sh '''
                    if ! command -v locust &> /dev/null; then
                        echo "Installing Locust..."
                        pip3 install locust==${LOCUST_VERSION} || pip install locust==${LOCUST_VERSION}
                    else
                        echo "Locust already installed"
                    fi
                    locust --version
                '''
            }
        }
        
        stage('Stress Tests') {
            steps {
                echo 'Running stress tests with Locust...'
                script {
                    // Create results directory
                    sh 'mkdir -p stress-test-results'
                    
                    // Run stress tests
                    sh '''
                        echo "Running stress tests for 60 seconds with 20 users..."
                        cd locust-stress-tests
                        locust -f user_service_stress_test.py UserServiceStressTest \\
                            --host=http://localhost:8080 \\
                            --users=20 \\
                            --spawn-rate=5 \\
                            --run-time=60s \\
                            --headless \\
                            --html=../stress-test-results/stress-test-report.html \\
                            --csv=../stress-test-results/stress-test
                    '''
                    
                    // Run spike tests
                    sh '''
                        echo "Running spike tests for 30 seconds with 50 users..."
                        cd locust-stress-tests
                        locust -f user_service_stress_test.py UserServiceSpikeTest \\
                            --host=http://localhost:8080 \\
                            --users=50 \\
                            --spawn-rate=10 \\
                            --run-time=30s \\
                            --headless \\
                            --html=../stress-test-results/spike-test-report.html \\
                            --csv=../stress-test-results/spike-test
                    '''
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'stress-test-results/*', allowEmptyArchive: true
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'stress-test-results',
                        reportFiles: '*.html',
                        reportName: 'Stress Test Reports'
                    ])
                }
            }
        }
        
        stage('Performance Analysis') {
            steps {
                echo 'Analyzing performance results...'
                script {
                    // Read and analyze stress test results
                    def stressStats = sh(script: 'cat stress-test-results/stress-test_stats.csv | tail -1', returnStdout: true).trim()
                    def spikeStats = sh(script: 'cat stress-test-results/spike-test_stats.csv | tail -1', returnStdout: true).trim()
                    
                    echo "Stress Test Results: ${stressStats}"
                    echo "Spike Test Results: ${spikeStats}"
                    
                    // Create performance summary
                    sh '''
                        echo "=== PERFORMANCE TEST SUMMARY ===" > stress-test-results/summary.txt
                        echo "Test Date: $(date)" >> stress-test-results/summary.txt
                        echo "" >> stress-test-results/summary.txt
                        echo "Stress Test (20 users, 60s):" >> stress-test-results/summary.txt
                        cat stress-test-results/stress-test_stats.csv | tail -1 >> stress-test-results/summary.txt
                        echo "" >> stress-test-results/summary.txt
                        echo "Spike Test (50 users, 30s):" >> stress-test-results/summary.txt
                        cat stress-test-results/spike-test_stats.csv | tail -1 >> stress-test-results/summary.txt
                    '''
                }
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                echo 'Archiving artifacts...'
                dir('user-service') {
                    archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'app.log', allowEmptyArchive: true
                }
                archiveArtifacts artifacts: 'stress-test-results/*', allowEmptyArchive: true
            }
        }
    }
    
    post {
        always {
            echo 'Cleaning up...'
            script {
                // Stop the service
                sh '''
                    if [ -f user-service/app.pid ]; then
                        PID=$(cat user-service/app.pid)
                        if ps -p $PID > /dev/null; then
                            kill $PID
                            echo "Service stopped (PID: $PID)"
                        fi
                    fi
                '''
            }
            cleanWs()
        }
        success {
            echo "✅ ${SERVICE_NAME} pipeline with comprehensive testing succeeded!"
            emailext (
                subject: "✅ User Service Pipeline Success",
                body: """
                User Service pipeline completed successfully!
                
                ✅ Build: SUCCESS
                ✅ Unit Tests: PASSED  
                ✅ Integration Tests: PASSED
                ✅ Stress Tests: COMPLETED
                
                Check stress test reports for performance metrics.
                """,
                to: "developer@company.com"
            )
        }
        failure {
            echo "❌ ${SERVICE_NAME} pipeline failed!"
            emailext (
                subject: "❌ User Service Pipeline Failed",
                body: """
                User Service pipeline failed!
                
                Please check the build logs for details.
                """,
                to: "developer@company.com"
            )
        }
    }
} 