Write-Host "=== DESPLIEGUE STAGING DESDE HOST (WINDOWS) ===" -ForegroundColor Green

# Navegar al directorio del proyecto (ya estamos ahí)
Set-Location "C:\Users\ALEJANDRO\Desktop\ecommerce-microservice-backend-app-Manuel"

# Detener servicios existentes
Write-Host "Deteniendo servicios de staging existentes..." -ForegroundColor Yellow
docker-compose -f staging-deployment/docker-compose-staging.yml down

# Lista de servicios a construir
$services = @("service-discovery", "user-service", "product-service", "order-service", "payment-service", "shipping-service")

foreach ($service in $services) {
    Write-Host "Construyendo $service..." -ForegroundColor Cyan
    
    if (Test-Path $service) {
        Set-Location $service
        
        # Construir JAR
        Write-Host "Building JAR for $service..." -ForegroundColor Yellow
        $buildResult = $null
        
        # Intentar con Maven wrapper primero
        if (Test-Path "mvnw.cmd") {
            $buildResult = & cmd /c "mvnw.cmd clean package -DskipTests" 2>&1
        } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
            $buildResult = mvn clean package -DskipTests 2>&1
        } else {
            Write-Host "Maven no encontrado para $service" -ForegroundColor Red
        }
        
        # Construir imagen Docker
        if (Test-Path "Dockerfile") {
            Write-Host "Building Docker image for $service..." -ForegroundColor Yellow
            docker build -t "selimhorri/${service}-ecommerce-boot:0.1.0" .
            if ($LASTEXITCODE -eq 0) {
                Write-Host "Imagen Docker construida exitosamente para $service" -ForegroundColor Green
            } else {
                Write-Host "Error construyendo imagen Docker para $service" -ForegroundColor Red
            }
        } else {
            Write-Host "Dockerfile no encontrado para $service" -ForegroundColor Yellow
        }
        
        Set-Location ..
    } else {
        Write-Host "Directorio $service no encontrado, usando imagen del registry" -ForegroundColor Yellow
    }
}

# Iniciar servicios
Write-Host "Iniciando servicios en staging..." -ForegroundColor Green
docker-compose -f staging-deployment/docker-compose-staging.yml up -d

# Verificar estado
Write-Host "Servicios staging iniciados" -ForegroundColor Green
docker-compose -f staging-deployment/docker-compose-staging.yml ps

Write-Host "=== DESPLIEGUE COMPLETADO ===" -ForegroundColor Green

# Mostrar información de acceso
Write-Host ""
Write-Host "SERVICIOS STAGING DISPONIBLES:" -ForegroundColor Cyan
Write-Host "- Service Discovery: http://localhost:90061" -ForegroundColor White
Write-Host "- User Service: http://localhost:90080" -ForegroundColor White
Write-Host "- Product Service: http://localhost:90081" -ForegroundColor White
Write-Host "- Order Service: http://localhost:90082" -ForegroundColor White
Write-Host "- Payment Service: http://localhost:90083" -ForegroundColor White
Write-Host "- Shipping Service: http://localhost:90084" -ForegroundColor White
Write-Host ""
Write-Host "Ahora ve a Jenkins y confirma que el despliegue se completó exitosamente." -ForegroundColor Yellow 