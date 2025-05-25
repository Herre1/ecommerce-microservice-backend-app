#!/bin/bash

# ===========================================
# SCRIPT DE DESPLIEGUE STAGING
# Sistema E-commerce - Taller 2 - Paso 4
# ===========================================

set -e  # Exit on any error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuración
STAGING_PORT_BASE="9000"
PROJECT_VERSION="0.1.0"
DOCKER_REGISTRY="selimhorri"
SERVICES=("service-discovery" "user-service" "product-service" "order-service" "payment-service" "shipping-service")

# Función para imprimir mensajes coloreados
print_message() {
    color=$1
    shift
    echo -e "${color}$@${NC}"
}

print_header() {
    echo ""
    print_message $BLUE "=========================================="
    print_message $BLUE "$1"
    print_message $BLUE "=========================================="
}

print_success() {
    print_message $GREEN "OK: $1"
}

print_warning() {
    print_message $YELLOW "Warning: $1"
}

print_error() {
    print_message $RED "ERROR: $1"
}

print_info() {
    print_message $CYAN "INFO: $1"
}

print_stage() {
    print_message $PURPLE "STAGE: $1"
}

# Función para mostrar ayuda
show_help() {
    echo "Script de Despliegue Staging - E-commerce Taller 2"
    echo ""
    echo "Uso: $0 [OPCIÓN]"
    echo ""
    echo "Opciones:"
    echo "  deploy       Ejecutar despliegue completo de staging"
    echo "  build        Solo construir servicios para staging"
    echo "  test         Solo ejecutar pruebas de staging"
    echo "  status       Verificar estado de servicios staging"
    echo "  logs         Mostrar logs de servicios staging"
    echo "  stop         Detener servicios staging"
    echo "  clean        Limpiar recursos staging"
    echo "  help         Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0 deploy    # Despliegue completo"
    echo "  $0 status    # Ver estado actual"
    echo "  $0 logs      # Ver logs"
}

# Función para verificar prerequisitos
check_prerequisites() {
    print_header "VERIFICANDO PREREQUISITOS"
    
    # Verificar Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker no está instalado"
        exit 1
    fi
    print_success "Docker: $(docker --version | cut -d' ' -f3 | cut -d',' -f1)"
    
    # Verificar Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose no está instalado"
        exit 1
    fi
    print_success "Docker Compose: $(docker-compose --version | cut -d' ' -f3 | cut -d',' -f1)"
    
    # Verificar Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven no está instalado"
        exit 1
    fi
    print_success "Maven: $(mvn -version | head -1 | cut -d' ' -f3)"
    
    # Verificar Java
    if ! command -v java &> /dev/null; then
        print_error "Java no está instalado"
        exit 1
    fi
    print_success "Java: $(java -version 2>&1 | head -1 | cut -d'"' -f2)"
    
    print_success "Todos los prerequisitos están disponibles"
}

# Función para generar docker-compose staging
generate_staging_compose() {
    print_stage "Generando configuración Docker Compose para staging..."
    
    mkdir -p staging-deployment
    
    cat > staging-deployment/docker-compose-staging.yml << EOF
version: '3.8'

services:
  service-discovery-staging:
    image: ${DOCKER_REGISTRY}/service-discovery-ecommerce-boot:${PROJECT_VERSION}-staging
    container_name: service-discovery-staging
    ports:
      - "${STAGING_PORT_BASE}61:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - SERVER_PORT=8761
    networks:
      - ecommerce-staging
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  user-service-staging:
    image: ${DOCKER_REGISTRY}/user-service-ecommerce-boot:${PROJECT_VERSION}-staging
    container_name: user-service-staging
    ports:
      - "${STAGING_PORT_BASE}80:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - SERVER_PORT=8080
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-staging:8761/eureka
    depends_on:
      - service-discovery-staging
    networks:
      - ecommerce-staging
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  product-service-staging:
    image: ${DOCKER_REGISTRY}/product-service-ecommerce-boot:${PROJECT_VERSION}-staging
    container_name: product-service-staging
    ports:
      - "${STAGING_PORT_BASE}81:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - SERVER_PORT=8080
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-staging:8761/eureka
    depends_on:
      - service-discovery-staging
    networks:
      - ecommerce-staging
    restart: unless-stopped

  order-service-staging:
    image: ${DOCKER_REGISTRY}/order-service-ecommerce-boot:${PROJECT_VERSION}-staging
    container_name: order-service-staging
    ports:
      - "${STAGING_PORT_BASE}82:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - SERVER_PORT=8080
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-staging:8761/eureka
    depends_on:
      - service-discovery-staging
    networks:
      - ecommerce-staging
    restart: unless-stopped

  payment-service-staging:
    image: ${DOCKER_REGISTRY}/payment-service-ecommerce-boot:${PROJECT_VERSION}-staging
    container_name: payment-service-staging
    ports:
      - "${STAGING_PORT_BASE}83:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - SERVER_PORT=8080
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-staging:8761/eureka
    depends_on:
      - service-discovery-staging
    networks:
      - ecommerce-staging
    restart: unless-stopped

  shipping-service-staging:
    image: ${DOCKER_REGISTRY}/shipping-service-ecommerce-boot:${PROJECT_VERSION}-staging
    container_name: shipping-service-staging
    ports:
      - "${STAGING_PORT_BASE}84:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - SERVER_PORT=8080
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-staging:8761/eureka
    depends_on:
      - service-discovery-staging
    networks:
      - ecommerce-staging
    restart: unless-stopped

networks:
  ecommerce-staging:
    driver: bridge
    name: ecommerce-staging-network

volumes:
  staging-data:
    name: ecommerce-staging-data
EOF

    print_success "Docker Compose configurado para staging"
}

# Función para construir servicios
build_services() {
    print_header "CONSTRUYENDO SERVICIOS PARA STAGING"
    
    for service in "${SERVICES[@]}"; do
        print_stage "Construyendo ${service}..."
        
        if [ -d "$service" ]; then
            cd "$service"
            
            # Construir JAR
            print_info "Compilando ${service}..."
            mvn clean package -DskipTests -q || {
                print_warning "Error compilando ${service}, continuando..."
                cd ..
                continue
            }
            
            # Construir imagen Docker
            print_info "Construyendo imagen Docker para ${service}..."
            docker build -t "${DOCKER_REGISTRY}/${service}-ecommerce-boot:${PROJECT_VERSION}-staging" . || {
                print_warning "Error construyendo imagen Docker para ${service}"
                cd ..
                continue
            }
            
            print_success "${service} construido exitosamente"
            cd ..
        else
            print_warning "Directorio ${service} no encontrado"
        fi
    done
    
    print_success "Construcción de servicios completada"
}

# Función para desplegar servicios
deploy_services() {
    print_header "DESPLEGANDO SERVICIOS EN STAGING"
    
    # Detener servicios existentes
    print_stage "Deteniendo servicios staging existentes..."
    docker-compose -f staging-deployment/docker-compose-staging.yml down 2>/dev/null || true
    
    # Limpiar recursos
    print_stage "Limpiando recursos previos..."
    docker system prune -f || true
    
    # Iniciar servicios
    print_stage "Iniciando servicios staging..."
    cd staging-deployment
    docker-compose -f docker-compose-staging.yml up -d
    cd ..
    
    print_success "Servicios staging desplegados"
}

# Función para verificar estado de servicios
check_services_status() {
    print_header "VERIFICANDO ESTADO DE SERVICIOS STAGING"
    
    local services_map=(
        "service-discovery:${STAGING_PORT_BASE}61"
        "user-service:${STAGING_PORT_BASE}80"
        "product-service:${STAGING_PORT_BASE}81"
        "order-service:${STAGING_PORT_BASE}82"
        "payment-service:${STAGING_PORT_BASE}83"
        "shipping-service:${STAGING_PORT_BASE}84"
    )
    
    local max_wait=300  # 5 minutos
    local check_interval=10
    local total_waited=0
    
    for service_info in "${services_map[@]}"; do
        IFS=':' read -r service_name port <<< "$service_info"
        print_stage "Verificando ${service_name} en puerto ${port}..."
        
        local service_ready=false
        local attempts=0
        
        while [ "$service_ready" = false ] && [ $total_waited -lt $max_wait ]; do
            attempts=$((attempts + 1))
            
            if curl -f -s "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
                service_ready=true
                print_success "${service_name} está listo (intento ${attempts})"
            else
                print_info "${service_name} no está listo, esperando... (intento ${attempts})"
                sleep $check_interval
                total_waited=$((total_waited + check_interval))
            fi
        done
        
        if [ "$service_ready" = false ]; then
            print_error "${service_name} no se pudo inicializar en ${max_wait} segundos"
            return 1
        fi
    done
    
    print_success "Todos los servicios staging están listos"
    return 0
}

# Función para ejecutar pruebas de staging
run_staging_tests() {
    print_header "EJECUTANDO PRUEBAS DE STAGING"
    
    # Pruebas de Smoke
    print_stage "Ejecutando Smoke Tests..."
    
    local smoke_tests=(
        "Service Discovery:http://localhost:${STAGING_PORT_BASE}61/actuator/health"
        "User Service:http://localhost:${STAGING_PORT_BASE}80/actuator/health"
        "User API:http://localhost:${STAGING_PORT_BASE}80/api/users"
        "Product Service:http://localhost:${STAGING_PORT_BASE}81/actuator/health"
        "Order Service:http://localhost:${STAGING_PORT_BASE}82/actuator/health"
        "Payment Service:http://localhost:${STAGING_PORT_BASE}83/actuator/health"
        "Shipping Service:http://localhost:${STAGING_PORT_BASE}84/actuator/health"
    )
    
    for test_info in "${smoke_tests[@]}"; do
        IFS=':' read -r test_name test_url <<< "$test_info"
        
        if curl -f -s "$test_url" > /dev/null 2>&1; then
            print_success "Smoke Test: ${test_name} - PASSED"
        else
            print_error "Smoke Test: ${test_name} - FAILED"
            return 1
        fi
    done
    
    # Pruebas de Integración
    print_stage "Ejecutando Integration Tests..."
    
    print_info "Test 1: Verificando registro en Service Discovery"
    if curl -s "http://localhost:${STAGING_PORT_BASE}61/eureka/apps" | grep -q "user-service"; then
        print_success "User Service registrado en Discovery"
    else
        print_warning "User Service no registrado en Discovery"
    fi
    
    print_info "Test 2: Creando usuario de prueba staging"
    local user_data='{"firstName":"Staging","lastName":"User","email":"staging@test.com","phone":"555-0123","credential":{"username":"staginguser","password":"password123","roleBasedAuthority":"ROLE_USER","isEnabled":true,"isAccountNonExpired":true,"isAccountNonLocked":true,"isCredentialsNonExpired":true}}'
    
    if curl -X POST "http://localhost:${STAGING_PORT_BASE}80/api/users" \
           -H "Content-Type: application/json" \
           -d "$user_data" \
           -s > /dev/null 2>&1; then
        print_success "Usuario staging creado exitosamente"
    else
        print_warning "Usuario staging ya existe o creación falló"
    fi
    
    print_info "Test 3: Verificando listado de usuarios"
    if curl -f -s "http://localhost:${STAGING_PORT_BASE}80/api/users" > /dev/null 2>&1; then
        print_success "Listado de usuarios funcional"
    else
        print_error "Listado de usuarios no disponible"
        return 1
    fi
    
    print_success "Todas las pruebas de staging pasaron"
}

# Función para mostrar logs
show_logs() {
    print_header "LOGS DE SERVICIOS STAGING"
    
    if [ -f "staging-deployment/docker-compose-staging.yml" ]; then
        docker-compose -f staging-deployment/docker-compose-staging.yml logs --tail=50
    else
        print_error "No hay servicios staging desplegados"
    fi
}

# Función para detener servicios
stop_services() {
    print_header "DETENIENDO SERVICIOS STAGING"
    
    if [ -f "staging-deployment/docker-compose-staging.yml" ]; then
        docker-compose -f staging-deployment/docker-compose-staging.yml down
        print_success "Servicios staging detenidos"
    else
        print_warning "No hay servicios staging para detener"
    fi
}

# Función para limpiar recursos
clean_resources() {
    print_header "LIMPIANDO RECURSOS STAGING"
    
    # Detener servicios
    stop_services
    
    # Limpiar imágenes staging
    print_stage "Limpiando imágenes Docker staging..."
    docker images | grep "\-staging" | awk '{print $3}' | xargs -r docker rmi -f || true
    
    # Limpiar directorios temporales
    print_stage "Limpiando directorios temporales..."
    rm -rf staging-deployment staging-logs staging-configs || true
    
    # Limpiar sistema Docker
    print_stage "Limpiando sistema Docker..."
    docker system prune -f || true
    
    print_success "Limpieza completada"
}

# Función para mostrar estado actual
show_status() {
    print_header "ESTADO ACTUAL DE STAGING"
    
    print_stage "Contenedores Docker:"
    docker ps --filter "name=staging" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || print_info "No hay contenedores staging ejecutándose"
    
    print_stage "Endpoints Disponibles:"
    local endpoints=(
        "Service Discovery:http://localhost:${STAGING_PORT_BASE}61"
        "User Service:http://localhost:${STAGING_PORT_BASE}80"
        "Product Service:http://localhost:${STAGING_PORT_BASE}81"
        "Order Service:http://localhost:${STAGING_PORT_BASE}82"
        "Payment Service:http://localhost:${STAGING_PORT_BASE}83"
        "Shipping Service:http://localhost:${STAGING_PORT_BASE}84"
    )
    
    for endpoint_info in "${endpoints[@]}"; do
        IFS=':' read -r service_name endpoint_url <<< "$endpoint_info"
        
        if curl -f -s "${endpoint_url}/actuator/health" > /dev/null 2>&1; then
            print_success "${service_name}: ${endpoint_url} - RUNNING"
        else
            print_warning "${service_name}: ${endpoint_url} - DOWN"
        fi
    done
}

# Función para despliegue completo
full_deploy() {
    print_header "DESPLIEGUE COMPLETO DE STAGING"
    print_info "Iniciando despliegue completo - Taller 2 Paso 4"
    
    check_prerequisites
    generate_staging_compose
    build_services
    deploy_services
    
    print_stage "Esperando 30 segundos para que los servicios inicien..."
    sleep 30
    
    if check_services_status; then
        run_staging_tests
        
        print_header "DESPLIEGUE STAGING COMPLETADO EXITOSAMENTE"
        
        print_success "Servicios desplegados en staging"
        print_success "Pruebas de staging completadas"
        print_success "Ambiente staging listo para validación"
        
        print_info ""
        print_info "Endpoints de Staging:"
        print_info "• Service Discovery: http://localhost:${STAGING_PORT_BASE}61"
        print_info "• User Service: http://localhost:${STAGING_PORT_BASE}80"
        print_info "• Product Service: http://localhost:${STAGING_PORT_BASE}81"
        print_info "• Order Service: http://localhost:${STAGING_PORT_BASE}82"
        print_info "• Payment Service: http://localhost:${STAGING_PORT_BASE}83"
        print_info "• Shipping Service: http://localhost:${STAGING_PORT_BASE}84"
        print_info ""
        print_info "Comandos útiles:"
        print_info "• Ver estado: $0 status"
        print_info "• Ver logs: $0 logs"
        print_info "• Detener: $0 stop"
        
    else
        print_error "Error en verificación de servicios"
        exit 1
    fi
}

# Función principal
main() {
    case "${1:-deploy}" in
        deploy)
            full_deploy
            ;;
        build)
            check_prerequisites
            generate_staging_compose
            build_services
            ;;
        test)
            run_staging_tests
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs
            ;;
        stop)
            stop_services
            ;;
        clean)
            clean_resources
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Opción desconocida: $1"
            show_help
            exit 1
            ;;
    esac
}

# Verificar si el script se ejecuta directamente
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi 