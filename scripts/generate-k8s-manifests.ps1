# Script para generar manifests de Kubernetes para todos los microservicios
# Microservicios a generar
$services = @(
    @{Name="user-service"; Port=8080; StagingPort=9080; MasterPort=8080},
    @{Name="product-service"; Port=8081; StagingPort=9081; MasterPort=8081},
    @{Name="order-service"; Port=8082; StagingPort=9082; MasterPort=8082},
    @{Name="payment-service"; Port=8083; StagingPort=9083; MasterPort=8083},
    @{Name="shipping-service"; Port=8084; StagingPort=9084; MasterPort=8084}
)

$environments = @("staging", "master")

function Generate-Manifest {
    param($ServiceName, $Port, $Environment, $Namespace)
    
    $manifest = @"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $ServiceName
  namespace: $Namespace
  labels:
    app: $ServiceName
    environment: $Environment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: $ServiceName
  template:
    metadata:
      labels:
        app: $ServiceName
        environment: $Environment
    spec:
      containers:
      - name: $ServiceName
        image: selimhorri/${ServiceName}-ecommerce-boot:0.1.0
        ports:
        - containerPort: $Port
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "$Environment"
        - name: EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE
          value: "http://service-discovery-service:8761/eureka"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: $Port
          initialDelaySeconds: 90
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: $Port
          initialDelaySeconds: 60
          periodSeconds: 15
---
apiVersion: v1
kind: Service
metadata:
  name: ${ServiceName}-service
  namespace: $Namespace
  labels:
    app: $ServiceName
    environment: $Environment
spec:
  type: ClusterIP
  ports:
  - port: $Port
    targetPort: $Port
    protocol: TCP
  selector:
    app: $ServiceName
"@
    return $manifest
}

Write-Host "Generando manifests de Kubernetes..."

foreach ($env in $environments) {
    $namespace = "ecommerce-$env"
    $envDir = "kubernetes/$env"
    
    Write-Host "Generando manifests para ambiente: $env"
    
    foreach ($service in $services) {
        $serviceName = $service.Name
        $port = $service.Port
        
        Write-Host "  Generando $serviceName..."
        
        $manifest = Generate-Manifest -ServiceName $serviceName -Port $port -Environment $env -Namespace $namespace
        $filePath = "$envDir/${serviceName}-deployment.yaml"
        
        $manifest | Out-File -FilePath $filePath -Encoding UTF8
    }
}

Write-Host "Manifests generados exitosamente!"
Write-Host "Para desplegar:"
Write-Host "  kubectl apply -f kubernetes/namespaces.yaml"
Write-Host "  kubectl apply -f kubernetes/staging/"
Write-Host "  kubectl apply -f kubernetes/master/" 