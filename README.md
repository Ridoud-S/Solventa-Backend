# Solventa CRM — Backend

> Mini CRM SaaS multi-tenant para PYMEs mexicanas.
> Gestión de leads, clientes, cotizaciones y seguimientos comerciales.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13+-blue?logo=postgresql)
![Flyway](https://img.shields.io/badge/Flyway-11.7.2-red)
![JWT](https://img.shields.io/badge/Auth-JWT%20%2B%20BCrypt-yellow)
![MVP](https://img.shields.io/badge/MVP-v1.0-blueviolet)

---

## ¿Qué es Solventa?

Solventa es una plataforma B2B diseñada para el flujo comercial de pequeñas y medianas empresas mexicanas: ferreterías, constructoras, talleres, despachos, distribuidores.

El flujo central del MVP es:

```
Lead (prospecto) → Cliente → Cotización → Seguimiento
```

Cada empresa que se registra es un **tenant completamente aislado** — ningún dato se comparte entre empresas.

---

## Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.0 |
| Base de datos | PostgreSQL 13+ |
| Migraciones | Flyway 11.7.2 |
| ORM | Spring Data JPA + Hibernate |
| Autenticación | JWT (jjwt 0.12.6) + BCrypt |
| Documentación | springdoc-openapi 2.8.8 |
| Boilerplate | Lombok |
| Build | Maven |

---

## Módulos del MVP

| Módulo | Descripción |
|---|---|
| **Auth** | Register, login, refresh token, forgot/reset password. Multi-tenant JWT. |
| **Leads** | CRUD de prospectos, cambio de estado, conversión a cliente. |
| **Customers** | CRUD de clientes con origen de lead opcional. |
| **FollowUps** | Historial de interacciones (llamadas, email, reuniones, WhatsApp) — polimórfico. |
| **Reminders** | Recordatorios por lead o cliente, con estado completado. |
| **Quotes** | Cotizaciones con líneas de detalle, cálculo automático (descuento + IVA), máquina de estados y job de vencimiento. |
| **Dashboard** | Métricas agregadas: leads, clientes, pipeline de cotizaciones, recordatorios del día. |

**Total: 32 endpoints REST** — ver [`docs/02-API-REFERENCE.md`](docs/02-API-REFERENCE.md)

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/com/solventa/solventa_backend/
│   │   ├── auth/           # JWT, login, registro
│   │   ├── tenant/         # Entidad Company (raíz multi-tenant)
│   │   ├── users/          # Entidad User, Role
│   │   ├── leads/          # Módulo de prospectos
│   │   ├── customers/      # Módulo de clientes
│   │   ├── followups/      # Seguimientos y recordatorios
│   │   ├── quotes/         # Cotizaciones + job de vencimiento
│   │   ├── dashboard/      # Agregaciones para métricas
│   │   └── shared/         # ApiResponse, TenantContext, excepciones, seguridad
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/migration/
│           ├── V1__init.sql       # Esquema completo (10 tablas)
│           └── V2__seed_data.sql  # Datos de prueba
└── test/
```

---

## Requisitos previos

- Java 21+
- Maven 3.8+
- PostgreSQL 13+

---

## Instalación y ejecución local

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/solventa-backend.git
cd solventa-backend
```

### 2. Crear la base de datos

```sql
CREATE DATABASE solventa_dev;
```

### 3. Configurar credenciales

Edita `src/main/resources/application-dev.yml` si tus credenciales de
PostgreSQL son distintas a las default:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/solventa_dev
    username: postgres
    password: postgres
```

### 4. Correr el proyecto

```bash
mvn spring-boot:run
```

Al iniciar, Flyway corre automáticamente `V1__init.sql` (crea las tablas)
y `V2__seed_data.sql` (inserta datos de prueba).

El servidor queda en `http://localhost:8080`.

---

## Datos de prueba (seed)

El seed incluye una empresa completa lista para probar:

| Usuario | Email | Contraseña | Rol |
|---|---|---|---|
| Roberto López | `roberto@ferreterialopez.mx` | `Admin123` | ADMIN |
| Ana Martínez | `ana@ferreterialopez.mx` | `Admin123` | SELLER |
| Carlos Herrera | `carlos@ferreterialopez.mx` | `Admin123` | SELLER |

También incluye 5 leads, 3 clientes, 5 cotizaciones con líneas de detalle,
6 seguimientos y 5 recordatorios distribuidos entre los usuarios.

---

## Primeras pruebas con IntelliJ HTTP Client o Postman

### 1. Login

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "roberto@ferreterialopez.mx",
  "password": "Admin123"
}
```

### 2. Listar leads (con el token recibido)

```http
GET http://localhost:8080/api/leads
Authorization: Bearer <accessToken>
```

### 3. Dashboard

```http
GET http://localhost:8080/api/dashboard/stats
Authorization: Bearer <accessToken>
```

---

## Multi-tenancy

El aislamiento de datos funciona así:

```
Request HTTP
     │
     ▼
JwtAuthFilter ──► extrae tenantId del JWT ──► TenantContext.setTenantId()
     │
     ▼
Controller ──► Service ──► Repository (siempre filtra por tenantId)
     │
     ▼
finally { TenantContext.clear() }   ← limpia el ThreadLocal siempre
```

Cada query de cada repositorio recibe `tenantId` explícitamente —
no hay filtros mágicos de Hibernate. Esto hace el aislamiento auditable
y explícito.

---

## Formato de respuesta

Todas las respuestas usan el wrapper `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": { }
}
```

En errores:

```json
{
  "success": false,
  "message": "Lead no encontrado"
}
```

Los códigos HTTP corresponden al tipo de error: `400`, `401`, `403`, `404`,
`409`, `500`.

---

## Variables de entorno en producción

```bash
DB_URL=jdbc:postgresql://host:5432/solventa_prod
DB_USERNAME=solventa_user
DB_PASSWORD=...
JWT_SECRET=...                    # openssl rand -base64 64
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=...
CORS_ALLOWED_ORIGINS=https://solventaio.com
PORT=8080
```

Activar el perfil de producción:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar solventa-backend.jar
```

---

## Documentación completa

La carpeta `docs/` contiene 4 archivos de referencia:

| Archivo | Contenido |
|---|---|
| [`00-ARQUITECTURA-Y-SEGURIDAD.md`](docs/00-ARQUITECTURA-Y-SEGURIDAD.md) | Stack, estructura de paquetes, multi-tenancy, JWT, convenciones de código |
| [`01-BASE-DE-DATOS.md`](docs/01-BASE-DE-DATOS.md) | Esquema completo, relaciones, triggers, datos del seed |
| [`02-API-REFERENCE.md`](docs/02-API-REFERENCE.md) | Los 32 endpoints con payloads de request/response |
| [`03-REGLAS-DE-NEGOCIO-Y-PENDIENTES.md`](docs/03-REGLAS-DE-NEGOCIO-Y-PENDIENTES.md) | Reglas de cada módulo, máquina de estados, configuración de entornos, pendientes y roadmap |

---

## Pendientes conocidos del MVP

- Sin autorización por rol (`@PreAuthorize`) — cualquier usuario autenticado
  puede hacer cualquier operación dentro de su tenant
- Sin módulo de gestión de equipo (invitar usuarios, listar, activar/desactivar)
- Sin envío real de emails (forgot-password solo imprime el token en el log)
- Sin tests automatizados (JUnit)
- Swagger UI configurado pero sin funcionar — usar `.http` / Postman
- `QuoteExpirationJob` no registra en `quote_status_history` al vencer automáticamente

Ver detalles y fixes sugeridos en
[`docs/03-REGLAS-DE-NEGOCIO-Y-PENDIENTES.md`](docs/03-REGLAS-DE-NEGOCIO-Y-PENDIENTES.md).

---

## Flujo de la máquina de estados de cotizaciones

```
DRAFT ──► SENT ──► WON
                ├──► LOST
                └──► EXPIRED  (también automático vía job cada hora)
```

- Solo `DRAFT` es editable y eliminable
- `WON`, `LOST`, `EXPIRED` son estados terminales
- Cada cambio queda registrado en `quote_status_history`

---

## Licencia

Privado — uso interno del equipo Solventa.