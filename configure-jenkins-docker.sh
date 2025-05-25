#!/bin/bash

echo "=== CONFIGURACIÓN DE JENKINS PARA DOCKER ==="
echo "Este script configura Jenkins para tener acceso a Docker"

# 1. Verificar si Jenkins está corriendo
echo "1. Verificando estado de Jenkins..."
docker ps | grep jenkins || echo "Jenkins no está corriendo en Docker"

# 2. Encontrar el container de Jenkins
JENKINS_CONTAINER=$(docker ps --format "{{.Names}}" | grep jenkins | head -1)

if [ -z "$JENKINS_CONTAINER" ]; then
    echo "ERROR: No se encontró container de Jenkins"
    echo "Iniciando Jenkins con acceso a Docker..."
    
    # Crear directorio para datos de Jenkins si no existe
    mkdir -p jenkins-data
    
    # Ejecutar Jenkins con acceso a Docker
    docker run -d \
        --name jenkins-with-docker \
        --restart=on-failure \
        -p 8080:8080 \
        -p 50000:50000 \
        -v jenkins-data:/var/jenkins_home \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v $(which docker):/usr/bin/docker \
        --group-add $(stat -c %g /var/run/docker.sock) \
        jenkins/jenkins:lts
    
    echo "Jenkins iniciado con acceso a Docker"
    echo "Esperando a que Jenkins esté listo..."
    sleep 30
    
    JENKINS_CONTAINER="jenkins-with-docker"
else
    echo "Container de Jenkins encontrado: $JENKINS_CONTAINER"
fi

# 3. Verificar acceso a Docker desde Jenkins
echo "2. Verificando acceso a Docker desde Jenkins..."
docker exec $JENKINS_CONTAINER docker --version || {
    echo "ERROR: Jenkins no tiene acceso a Docker"
    echo "Intentando instalar Docker en Jenkins..."
    
    # Instalar Docker CLI en Jenkins
    docker exec -u root $JENKINS_CONTAINER bash -c "
        apt-get update && \
        apt-get install -y \
            apt-transport-https \
            ca-certificates \
            curl \
            gnupg \
            lsb-release && \
        curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
        echo 'deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian bullseye stable' | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
        apt-get update && \
        apt-get install -y docker-ce-cli
    "
}

# 4. Verificar Docker Compose
echo "3. Verificando Docker Compose en Jenkins..."
docker exec $JENKINS_CONTAINER docker-compose --version || {
    echo "Instalando Docker Compose en Jenkins..."
    docker exec -u root $JENKINS_CONTAINER bash -c "
        curl -L 'https://github.com/docker/compose/releases/download/v2.32.4/docker-compose-linux-x86_64' -o /usr/local/bin/docker-compose && \
        chmod +x /usr/local/bin/docker-compose && \
        ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    "
}

# 5. Verificar permisos
echo "4. Configurando permisos..."
docker exec -u root $JENKINS_CONTAINER bash -c "
    # Agregar usuario jenkins al grupo docker
    usermod -aG docker jenkins || echo 'Grupo docker no existe en container'
    
    # Dar permisos al socket de Docker
    chmod 666 /var/run/docker.sock || echo 'Socket no montado'
    
    # Verificar permisos
    ls -la /var/run/docker.sock || echo 'Socket no encontrado'
"

# 6. Reiniciar Jenkins para aplicar cambios
echo "5. Reiniciando Jenkins para aplicar cambios..."
docker restart $JENKINS_CONTAINER

echo "Esperando a que Jenkins reinicie..."
sleep 45

# 7. Verificación final
echo "6. Verificación final..."
docker exec $JENKINS_CONTAINER docker --version && echo "✓ Docker CLI disponible"
docker exec $JENKINS_CONTAINER docker-compose --version && echo "✓ Docker Compose disponible"

# 8. Crear script de test
echo "7. Creando script de test..."
cat > test-jenkins-docker.sh << 'EOF'
#!/bin/bash
echo "=== TEST DE JENKINS CON DOCKER ==="

JENKINS_CONTAINER=$(docker ps --format "{{.Names}}" | grep jenkins | head -1)

if [ -z "$JENKINS_CONTAINER" ]; then
    echo "ERROR: Jenkins no está corriendo"
    exit 1
fi

echo "Testing Docker access..."
docker exec $JENKINS_CONTAINER docker ps
echo ""

echo "Testing Docker Compose..."
docker exec $JENKINS_CONTAINER docker-compose --version
echo ""

echo "Testing Docker permissions..."
docker exec $JENKINS_CONTAINER docker images
echo ""

echo "✓ Jenkins con Docker configurado correctamente"
EOF

chmod +x test-jenkins-docker.sh

echo ""
echo "=== CONFIGURACIÓN COMPLETADA ==="
echo ""
echo "Jenkins debería tener acceso a Docker ahora."
echo ""
echo "Para verificar la configuración, ejecuta:"
echo "  ./test-jenkins-docker.sh"
echo ""
echo "Jenkins URL: http://localhost:8080"
echo ""
echo "Para obtener la contraseña inicial de Jenkins:"
echo "  docker exec $JENKINS_CONTAINER cat /var/jenkins_home/secrets/initialAdminPassword"
echo "" 