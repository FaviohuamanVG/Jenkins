# 🏛️ VG Users Microservice

## 📋 Tabla de Contenidos
- [Contexto](#contexto)
- [Arquitectura](#arquitectura)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Tecnologías](#tecnologías)
- [Funcionalidades](#funcionalidades)
- [Endpoints API](#endpoints-api)
- [Instalación y Configuración](#instalación-y-configuración)
- [Ejemplos de Uso](#ejemplos-de-uso)

---

## 🎯 Contexto

El **VG Users Microservice** es un sistema de gestión integral de usuarios diseñado para instituciones educativas del grupo Valle Grande. Este microservicio maneja la autenticación, autorización y gestión de usuarios, así como sus asignaciones a diferentes instituciones educativas.

### Problema que Resuelve
- **Gestión centralizada** de usuarios en múltiples sedes
- **Asignación flexible** de usuarios a diferentes instituciones con roles específicos
- **Trazabilidad completa** de cambios y movimientos de usuarios
- **Integración** con Keycloak para autenticación y autorización
- **Eliminación lógica y física** de registros

---

## 🏗️ Arquitectura

### Arquitectura Hexagonal (Clean Architecture)
```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │      REST       │  │   REPOSITORY    │  │     CONFIG      │ │
│  │   Controllers   │  │   (MongoDB)     │  │   (CORS, etc)   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                        │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │    SERVICES     │  │   INTERFACES    │                   │
│  │ (Business Logic)│  │  (Contracts)    │                   │
│  └─────────────────┘  └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │     MODELS      │  │      ENUMS      │  │      DTOs       │ │
│  │   (Entities)    │  │  (UserStatus,   │  │  (Requests,     │ │
│  │                 │  │   Roles, etc)   │  │   Responses)    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Tecnologías Utilizadas
- **Framework:** Spring Boot 3.x
- **Reactive Programming:** Spring WebFlux (Reactor)
- **Base de Datos:** MongoDB (Reactive)
- **Autenticación:** Keycloak Integration
- **Documentación:** Swagger/OpenAPI
- **Build Tool:** Maven
- **Java Version:** 17+

---

## 📁 Estructura del Proyecto

```
src/
├── main/
│   ├── java/pe/edu/vallegrande/vgmsuser/
│   │   ├── VgMsUserApplication.java                 # Main Application
│   │   ├── application/                             # APPLICATION LAYER
│   │   │   ├── impl/                               # Service Implementations
│   │   │   │   ├── AdminUserServiceImpl.java
│   │   │   │   ├── AuthServiceImpl.java
│   │   │   │   ├── EmailServiceImpl.java
│   │   │   │   ├── KeycloakServiceImpl.java
│   │   │   │   └── UserManagementServiceImpl.java
│   │   │   └── service/                            # Service Interfaces
│   │   │       ├── IAdminUserService.java
│   │   │       ├── IAuthService.java
│   │   │       ├── IEmailService.java
│   │   │       ├── IKeycloakService.java
│   │   │       └── IUserManagementService.java
│   │   ├── domain/                                  # DOMAIN LAYER
│   │   │   └── model/                              # Domain Models
│   │   │       ├── User.java                       # User Entity
│   │   │       ├── UserProfile.java                # User Profile Entity
│   │   │       ├── dto/                            # Data Transfer Objects
│   │   │       └── enums/                          # Domain Enums
│   │   │           ├── UserStatus.java
│   │   │           ├── DocumentType.java
│   │   │           └── PasswordStatus.java
│   │   └── infrastructure/                         # INFRASTRUCTURE LAYER
│   │       ├── config/                            # Configuration
│   │       │   ├── CorsConfig.java
│   │       │   └── MongoConfig.java
│   │       ├── repository/                        # Data Repositories
│   │       │   └── UserProfileRepository.java
│   │       ├── rest/                              # REST Controllers
│   │       │   └── UserManagementRest.java
│   │       └── util/                              # Utilities
│   └── resources/
│       ├── application.yml                        # Configuration
│       └── templates/                            # Email Templates
│           ├── reset-password-form.html
│           └── email/
│               ├── password-change-confirmation.html
│               ├── password-reset.html
│               └── temporary-credentials.html
└── test/                                         # Test Classes
    └── java/pe/edu/vallegrande/vgmsuser/
        └── VgMsUserApplicationTests.java
```

---

## ⚡ Funcionalidades

### 👥 Gestión de Usuarios
- ✅ **Crear usuarios completos** (Keycloak + MongoDB)
- ✅ **Autenticación y autorización** integrada con Keycloak
- ✅ **Gestión de perfiles** de usuario
- ✅ **Cambio de estados** (ACTIVE, INACTIVE, SUSPENDED)
- ✅ **Reset de contraseñas** con tokens
- ✅ **Envío de emails** automáticos
- ✅ **Eliminación lógica y física**

### 📊 Auditoria y Seguimiento
- ✅ **Registro de movimientos** con timestamps
- ✅ **Descripción detallada** de cada acción
- ✅ **Estados anteriores y nuevos** en cada cambio
- ✅ **Logs estructurados** para debugging

---

## 🔗 Endpoints API

### 👤 User Management (`/api/v1/user-director`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/users` | Crear usuario completo |
| `GET` | `/users` | Obtener todos los usuarios |
| `GET` | `/users/keycloak/{keycloakId}` | Obtener usuario por Keycloak ID |
| `GET` | `/users/username/{username}` | Obtener usuario por username |
| `PUT` | `/users/{keycloakId}` | Actualizar usuario |
| `DELETE` | `/users/{keycloakId}` | Eliminar usuario |
| `PATCH` | `/users/{keycloakId}/status` | Cambiar estado |
| `PATCH` | `/users/{keycloakId}/activate` | Activar usuario |
| `PATCH` | `/users/{keycloakId}/deactivate` | Desactivar usuario |
| `GET` | `/users/status/{status}` | Usuarios por estado |

---

## 📝 Ejemplos de Uso

### 🆕 Crear Usuario Completo

**Request:**
```http
POST /api/v1/user-director/users
Content-Type: application/json

{
  "username": "jperez",
  "email": "jperez@vallegrande.edu.pe",
  "firstName": "Juan",
  "lastName": "Pérez",
  "documentType": "DNI",
  "documentNumber": "12345678",
  "password": "TempPassword123!"
}
```

**Response (201 Created):**
```json
{
  "id": "68b8eb9c3bd340fcaa9ca81fb",
  "keycloakId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": "jperez",
  "email": "jperez@vallegrande.edu.pe",
  "firstName": "Juan",
  "lastName": "Pérez",
  "documentType": "DNI",
  "documentNumber": "12345678",
  "status": "ACTIVE",
  "createdAt": "2025-10-17T10:30:00.000",
  "updatedAt": "2025-10-17T10:30:00.000"
}
```

---

## 🎯 Roles y Estados

### 📊 Estados de Usuario (`UserStatus`)
- `ACTIVE` - Usuario activo
- `INACTIVE` - Usuario inactivo
- `SUSPENDED` - Usuario suspendido
- `TERMINATED` - Usuario terminado

---

## 🚀 Instalación y Configuración

### Prerrequisitos
- Java 17+
- Maven 3.8+
- MongoDB 4.4+
- Keycloak Server

### Configuración

1. **Clonar el repositorio:**
```bash
git clone <repository-url>
cd vg-users-microservice
```

2. **Configurar MongoDB y Keycloak en `application.yml`:**
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/vg_users_db
      
keycloak:
  server-url: http://localhost:8080
  realm: vg-realm
  client-id: vg-users-client
```

3. **Ejecutar la aplicación:**
```bash
mvn spring-boot:run
```

4. **La aplicación estará disponible en:**
```
http://localhost:8100
```

---

## 📚 Documentación Adicional

### Swagger/OpenAPI
- **URL:** `http://localhost:8100/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8100/v3/api-docs`

### Logs
- Los logs incluyen información detallada de cada operación
- Nivel configurable en `application.yml`
- Trazabilidad completa de movimientos de usuarios

---

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Ver `LICENSE` para más detalles.

---

## 👥 Autores

- **Valle Grande Team** - *Desarrollo inicial* - [Valle Grande](https://vallegrande.edu.pe)

---

**🎓 Valle Grande - Formando Líderes del Futuro**