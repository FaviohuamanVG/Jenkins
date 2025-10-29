# Detalle de Microservicios - Sistema Escolar Multisucursal (Versión Total) Dividido por Roles y Instituciones

## Contexto del Sistema

El sistema escolar multisucursal gestiona una estructura jerárquica completa desde un grupo educativo principal que administra múltiples instituciones, cada una con sus propios sedes, niveles educativos y aulas. El sistema maneja datos de usuarios, estudiantes, asistencias, calificaciones, trámites y procesos administrativos con roles claramente definidos (ADMIN, DIRECTOR, TEACHER).

---

## 1️⃣ Microservicio User Management ✅ IMPLEMENTADO v5.0

> **Descripción:** Gestionar usuarios del sistema educativo con estructura jerárquica RESTful organizada por roles, utilizando headers HTTP para autenticación y autorización granular.

### Database Schema (PostgreSQL)

```sql
    CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        first_name TEXT NOT NULL,
        last_name TEXT NOT NULL,
        document_type TEXT NOT NULL CHECK (document_type IN ('DNI', 'CE', 'PASSPORT')),
        document_number TEXT NOT NULL,
        email TEXT UNIQUE NOT NULL,
        phone TEXT,
        password TEXT NOT NULL,
        roles TEXT NOT NULL, -- JSON array de roles: ["ADMIN"], ["DIRECTOR", "TEACHER"], etc.
        institution_id TEXT,
        status TEXT DEFAULT 'A' CHECK (status IN ('A', 'I')),
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
        UNIQUE(document_type, document_number)
    ) 
```
### 👥 Vistas por Roles

#### 🔴 ADMIN
- Puede gestionar todos los usuarios que sean DIRECTORES
- Admin puede crear admins y asignar institución a un director

#### 🟡 DIRECTOR  
- Gestiona usuarios dentro de su institución como (TEACHER, SECRETARY, AUXILIARY únicamente)
- Cuando crea un usuario, se le asigna automáticamente la institución del director
- No puede crear ADMINS ni otros DIRECTORES, ni cambiar roles o instituciones
- Solo puede eliminar usuarios TEACHER, AUXILIARY, SECRETARY (no puede eliminar otros DIRECTORES)

#### 🟢 TEACHER, AUXILIARY, SECRETARY, DIRECTOR
- Solo pueden ver y actualizar su propia información personal (excepto rol e institución)

---

### 🔴 Endpoints ADMIN ✅ IMPLEMENTADOS - Headers HTTP v5.0

| Método | Endpoint | Headers Requeridos | Descripción |
|--------|----------|-------------------|-------------|
| `POST` | `/users/admin/create` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` (null) | Crear usuarios ADMIN y DIRECTOR únicamente (puede asignar institución a directores) |
| `GET` | `/users/admin` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` (null) | Listar todos los usuarios ADMIN del sistema |
| `GET` | `/users/admin/directors` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` (null) | Listar todos los usuarios DIRECTORES de todas las instituciones |
| `GET` | `/users/admin/directors/{institution_id}` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` (null) | Listar usuarios DIRECTORES de una institución específica |
| `PUT` | `/users/admin/update/{user_id}` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` (null) | Actualizar usuarios ADMIN y DIRECTORES (puede cambiar roles e instituciones) |
| `DELETE` | `/users/admin/delete/{user_id}` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` (null) | Eliminar usuarios ADMIN y DIRECTORES únicamente |

### 🟡 Endpoints DIRECTOR ✅ IMPLEMENTADOS - Headers HTTP v5.0

| Método | Endpoint | Headers Requeridos | Descripción |
|--------|----------|-------------------|-------------|
| `POST` | `/users/director/create` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` | Crear usuarios TEACHER, AUXILIARY, SECRETARY únicamente (NO puede crear DIRECTOR, institución se asigna automáticamente) |
| `GET` | `/users/director/staff` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` | Listar todos los usuarios de su institución (TEACHER, AUXILIARY, SECRETARY) |
| `GET` | `/users/director/by-role/{role}` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` | Listar usuarios por rol específico de su institución (TEACHER, AUXILIARY, SECRETARY) |
| `PUT` | `/users/director/update/{user_id}` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` | Actualizar usuarios de su institución (NO puede cambiar roles o instituciones) |
| `DELETE` | `/users/director/delete/{user_id}` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` | Eliminar usuarios de su institución (TEACHER, AUXILIARY, SECRETARY únicamente, NO puede eliminar DIRECTOR) |

### 🟢 Endpoints TEACHER, AUXILIARY, SECRETARY ✅ IMPLEMENTADOS - Headers HTTP v5.0

| Método | Endpoint | Headers Requeridos | Descripción |
|--------|----------|-------------------|-------------|
| `GET` | `/users/personal/profile` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` | Obtener información personal (solo puede ver su propia información) |
| `PUT` | `/users/personal/update` | `X-User-Id`, `X-User-Roles`, `X-Institution-Id` | Actualizar información personal (solo puede modificar su propia información, excepto rol e institución) |


### 🔐 Sistema de Headers HTTP v5.0

**Headers Obligatorios:**
- `X-User-Id`: ID único del usuario que realiza la petición (obtenido del token JWT)
- `X-User-Roles`: Roles del usuario separados por comas (ej: "ADMIN,DIRECTOR")  
- `X-Institution-Id`: ID de la institución del usuario (**obligatorio para DIRECTOR/TEACHER/AUXILIARY/SECRETARY**, **null para ADMIN**)

**Validación Granular:**
- ✅ **ADMIN**: Acceso completo a cualquier institución (`X-Institution-Id` = `null` ya que no pertenece a institución específica)
- ✅ **DIRECTOR**: Solo acceso a su propia institución (`X-Institution-Id` debe coincidir con su institución)
- ✅ **TEACHER/AUXILIARY/SECRETARY**: Solo acceso a su información personal y de su institución (`X-Institution-Id` obligatorio)


### 🔗 Endpoints de Otros Microservicios ✅ IMPLEMENTADO

| Método | Endpoint | Descripción | Return |
|--------|----------|-------------|--------|
| `GET` | `/user-role/{user_email}` | **MEJORADO v3.1**: Obtener información completa del usuario por email | `{ "id": "349d9570-837a-4b0e-93f1-6cb2d698de81", "email": "admin@sistema.edu", "roles": [ "ADMIN" ], "institution_id": null, "status": "A", "has_access": true }` |
| `GET` | `/user-role-by-id/{user_id}` | **NUEVO v3.1**: Obtener información completa del usuario por ID | `{{ "id": "349d9570-837a-4b0e-93f1-6cb2d698de81", "email": "admin@sistema.edu", "roles": [ "ADMIN" ], "institution_id": null, "status": "A", "has_access": true }` |

### 📌 Respuesta de los EndPoints

#### GET, POST, PUT : Un Objecto
```json
{
    "message": "User created successfully",
    "user": {
        "id": "3d08ba5c-480a-4b82-86b7-dcab4b13ad66",
        "first_name": "Carlfos",
        "last_name": "Directfor Pérez",
        "document_type": "DNI",
        "document_number": "99887726",
        "email": "carlos.director2@sistema.edu",
        "phone": "555666727",
        "roles": [
            "DIRECTOR"
        ],
        "institution_id": "96960392-1e5f-4e66-afc9-4b5bcd771d9f",
        "status": "A",
        "created_at": "2025-10-04 19:38:07"
    }
}
```

### GET : Un Array de Objectos
```json
{
    "message": "Users retrieved successfully",
    "total_users": 1,
    "users": [
        {
            "id": "349d9570-837a-4b0e-93f1-6cb2d698de81",
            "first_name": "Sistema",
            "last_name": "Administrador",
            "document_type": "DNI",
            "document_number": "00000000",
            "email": "admin@sistema.edu",
            "phone": "999999999",
            "roles": [
                "ADMIN"
            ],
            "institution_id": null,
            "status": "A",
            "created_at": "2025-10-04 00:21:43"
        },
          {
            "id": "349d9570-837a-4b0e-93f1-6cb2d698de81",
            "first_name": "Sistema",
            "last_name": "Administrador",
            "document_type": "DNI",
            "document_number": "00000000",
            "email": "admin@sistema.edu",
            "phone": "999999999",
            "roles": [
                "ADMIN"
            ],
            "institution_id": null,
            "status": "A",
            "created_at": "2025-10-04 00:21:43"
        }
    ]
}
```

#### DELETE : Un Mensaje
```json
{
    "message": "User desactive successfully"
}
```

#### Error Handling (400, 401, 403, 404, 500)
```json
{
    "error": "User not found"
}
```

### 🏗️ Nota

- El DNI debe quitar que no sea único
- Por ahora no vas a hacer la verificación de si existe el institution Usar de ejemplo : if(X-Institution-Id == "HJSHJU789289298289")

---