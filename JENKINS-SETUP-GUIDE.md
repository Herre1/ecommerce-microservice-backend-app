# Guía de Configuración de Jenkins - Taller 2

## Configuración Automática (Recomendada)

### Opción 1: Script Automático

1. **Abrir Jenkins**: http://localhost:8090
2. **Ir a "Manage Jenkins" > "Script Console"**
3. **Copiar el contenido completo del archivo `setup-jenkins-jobs.groovy`**
4. **Pegar en la consola y hacer clic en "Run"**
5. **Verificar que aparezcan los 5 jobs creados**

---

## Configuración Manual (Alternativa)

### Paso 1: Crear Job de Testing Comprensivo

1. **Crear nuevo job**:
   - Nombre: `ecommerce-comprehensive-testing`
   - Tipo: "Pipeline"
   - Clic en "OK"

2. **Configurar el pipeline**:
   - Description: "Pipeline comprensivo de testing para el User Service"
   - Pipeline Definition: "Pipeline script"
   - Script: Copiar contenido de `pipeline-scripts/comprehensive-testing-pipeline.groovy`
   - Guardar

### Paso 2: Crear Job de User Service

1. **Crear nuevo job**:
   - Nombre: `ecommerce-user-service-pipeline`
   - Tipo: "Pipeline"
   - Clic en "OK"

2. **Configurar el pipeline**:
   - Description: "Pipeline básico para User Service con testing"
   - Pipeline Definition: "Pipeline script"
   - Script: Copiar contenido de `pipeline-scripts/user-service-with-tests.groovy`
   - Guardar

### Paso 3: Crear Job de Staging

1. **Crear nuevo job**:
   - Nombre: `ecommerce-staging-deployment`
   - Tipo: "Pipeline"
   - Clic en "OK"

2. **Configurar el pipeline**:
   - Description: "Pipeline de despliegue en ambiente staging"
   - Pipeline Definition: "Pipeline script"
   - Script: Copiar contenido de `pipeline-scripts/ecommerce-staging-pipeline-hybrid-fixed.groovy`
   - Guardar

### Paso 4: Crear Job de Kubernetes Staging

1. **Crear nuevo job**:
   - Nombre: `ecommerce-kubernetes-staging`
   - Tipo: "Pipeline"
   - Clic en "OK"

2. **Configurar el pipeline**:
   - Description: "Pipeline de staging con Kubernetes"
   - Pipeline Definition: "Pipeline script"
   - Script: Copiar contenido de `pipeline-scripts/ecommerce-kubernetes-staging-pipeline.groovy`
   - Guardar

### Paso 5: Crear Job de Producción

1. **Crear nuevo job**:
   - Nombre: `ecommerce-kubernetes-master`
   - Tipo: "Pipeline"
   - Clic en "OK"

2. **Configurar el pipeline**:
   - Description: "Pipeline de producción con Kubernetes"
   - Pipeline Definition: "Pipeline script"
   - Script: Copiar contenido de `pipeline-scripts/ecommerce-kubernetes-master-pipeline.groovy`
   - Guardar

---

## Verificación de la Configuración

### Comprobar que Jenkins está funcionando:

```bash
# Verificar que Jenkins está ejecutándose
curl -I http://localhost:8090

# Debería devolver: HTTP/1.1 200 OK
```

### Lista de Jobs Esperados:

Después de la configuración, deberías ver estos jobs en Jenkins:

1. **ecommerce-comprehensive-testing** - Testing completo
2. **ecommerce-user-service-pipeline** - Pipeline básico
3. **ecommerce-staging-deployment** - Despliegue staging
4. **ecommerce-kubernetes-staging** - Staging con K8s
5. **ecommerce-kubernetes-master** - Producción con K8s

---

## Orden de Ejecución Recomendado

### Para el Taller 2, ejecutar en este orden:

1. **Paso 3 - Testing**: `ecommerce-comprehensive-testing`
   - Valida todas las pruebas (unitarias, integración, E2E, estrés)
   - Genera reportes de testing

2. **Paso 4 - Staging**: `ecommerce-staging-deployment`
   - Despliega en ambiente staging
   - Ejecuta validaciones de staging
   - Gate de aprobación manual

3. **Paso 5 - Producción**: `ecommerce-kubernetes-master`
   - Despliegue en Kubernetes
   - Validaciones de producción
   - Monitoreo completo

---

## Configuraciones Adicionales

### Variables de Entorno Globales en Jenkins:

1. **Ir a "Manage Jenkins" > "Configure System"**
2. **Sección "Global Properties" > "Environment variables"**
3. **Agregar estas variables**:

| Variable | Valor |
|----------|-------|
| `DOCKER_REGISTRY` | `selimhorri` |
| `PROJECT_VERSION` | `0.1.0` |
| `KUBERNETES_NAMESPACE` | `ecommerce` |
| `LOCUST_VERSION` | `2.17.0` |

### Plugins Necesarios:

Verificar que estos plugins estén instalados:

- **Pipeline Plugin**
- **Docker Pipeline Plugin**
- **Kubernetes Plugin**
- **HTML Publisher Plugin**
- **JUnit Plugin**
- **Git Plugin**

---

## Solución de Problemas

### Problema: "Pipeline script not found"

**Solución**:
1. Verificar que los archivos `.groovy` existen en `pipeline-scripts/`
2. Usar "Pipeline script" en lugar de "Pipeline script from SCM"
3. Copiar el contenido completo del archivo en el campo Script

### Problema: "Workspace not found"

**Solución**:
1. Verificar la ruta del workspace en el script
2. Cambiar `workspacePath` en `setup-jenkins-jobs.groovy` si es necesario
3. Usar rutas absolutas

### Problema: "Permission denied"

**Solución**:
1. Ejecutar Jenkins como administrador
2. Verificar permisos de Docker
3. Comprobar acceso a directorios del proyecto

---

## Comandos Útiles

### Verificar Jobs desde Línea de Comandos:

```bash
# Listar todos los jobs
curl -s "http://localhost:8090/api/json" | jq '.jobs[].name'

# Ver estado de un job específico
curl -s "http://localhost:8090/job/ecommerce-comprehensive-testing/api/json" | jq '.lastBuild.result'

# Ejecutar un job desde CLI (requiere autenticación)
curl -X POST "http://localhost:8090/job/ecommerce-comprehensive-testing/build"
```

### Backup de Configuración:

```bash
# Hacer backup de la configuración de Jenkins
cp -r jenkins-data/jobs/ jenkins-backup-$(date +%Y%m%d)/
```

---

## Siguiente Paso

Una vez configurados los jobs:

1. **Ejecutar `ecommerce-comprehensive-testing`** para validar el testing
2. **Revisar reportes generados**
3. **Continuar con staging y producción**

¡La configuración de Jenkins estará completa y lista para el Taller 2!

---

*Última actualización: Configuración para ambiente Windows con Docker* 