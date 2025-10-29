# Jenkins Pipeline Configuration
# =============================

## 🛠️ Requisitos Previos en Jenkins

### 1. Plugins Necesarios:
- Pipeline
- Git
- Maven Integration
- JUnit
- JaCoCo
- HTML Publisher (opcional, para reportes HTML)
- Email Extension (opcional, para notificaciones)

### 2. Herramientas a Configurar en Jenkins:
- **Maven**: Nombre `Maven-3.9` (o cambiar en Jenkinsfile)
- **JDK**: Nombre `JDK-17` (o cambiar en Jenkinsfile)

### 3. Variables de Entorno (Opcionales):
```
NOTIFICATION_EMAIL=team@vallegrande.edu.pe
SLACK_CHANNEL=#development
```

## 🚀 Configuración del Pipeline

### 1. Crear un nuevo Pipeline Job:
1. New Item → Pipeline
2. Nombre: `VG-User-Microservice-Pipeline`
3. Pipeline definition: `Pipeline script from SCM`
4. Repository URL: `[tu-repo-git]`
5. Script Path: `Jenkinsfile`

### 2. Configurar Triggers:
- ✅ GitHub hook trigger for GITScm polling
- ✅ Poll SCM: `H/5 * * * *` (cada 5 minutos)

### 3. Parámetros del Build (Opcional):
- `RUN_PERFORMANCE_TESTS` (Boolean): false por defecto
- `NOTIFICATION_ENABLED` (Boolean): true por defecto

## 📊 Reportes Generados

### 1. Test Results:
- Ubicación: `target/surefire-reports/*.xml`
- Tipo: JUnit XML
- Cobertura: Solo pruebas unitarias específicas

### 2. Code Coverage:
- Ubicación: `target/site/jacoco/`
- Formato: HTML + XML
- Mínimo requerido: 70%

### 3. Artifacts:
- JAR file: `target/*.jar`
- Build report: `build-report.txt`
- Coverage reports: `target/site/jacoco/`

## 🎯 Stages del Pipeline

1. **🔍 Checkout**: Obtener código fuente
2. **🧹 Clean & Compile**: Limpiar y compilar
3. **🧪 Unit Tests**: Ejecutar las 3 pruebas unitarias
4. **📊 Code Coverage**: Generar reportes de cobertura
5. **🚀 Performance Tests**: Solo en ramas main/develop
6. **📦 Package**: Empaquetar aplicación
7. **📋 Quality Gate**: Verificar criterios de calidad

## 🛡️ Garantías de Seguridad

### ✅ Confirmaciones en cada build:
- NO se crean usuarios reales en Keycloak
- NO se envían emails reales por SMTP  
- Solo mocks y simulaciones controladas
- Perfil `test` activado para todas las pruebas

## 🔧 Comandos Útiles para Testing Local

### Ejecutar solo las pruebas unitarias:
```bash
mvn test -Dtest="UserManagementServiceSimpleTest,AuthServiceImplTest,EmailServiceImplTest"
```

### Generar reporte de coverage:
```bash
mvn jacoco:prepare-agent test jacoco:report
```

### Ejecutar todas las pruebas:
```bash
mvn clean test
```

### Ejecutar pruebas de rendimiento:
```bash
mvn test -Dtest="PerformanceTestSuite" -Dspring.profiles.active=performance
```

## 📧 Notificaciones (Configurar según necesidad)

### Email:
- Éxito: Solo en rama main
- Fallo: Siempre notificar
- Destinatarios: Equipo de desarrollo

### Slack (Opcional):
- Canal: #development
- Éxito: Emoji ✅
- Fallo: Emoji ❌

## 🚨 Troubleshooting

### Error: "Tool Maven-3.9 does not exist"
**Solución**: Configurar Maven en Jenkins Global Tools

### Error: "No test results found"
**Solución**: Verificar que las pruebas se ejecuten correctamente localmente

### Error: Coverage below 70%
**Solución**: Escribir más pruebas unitarias o ajustar el límite

### Error: Quality Gate Failed
**Solución**: Revisar fallos en pruebas y corregir