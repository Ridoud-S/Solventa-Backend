# Solventa CRM — Documentación del Backend (MVP v1.0)

> Generado el 11 de junio de 2026. Cubre el backend tal como quedó al cierre del
> MVP (8 módulos completos). Si el código evoluciona, este documento debe
> actualizarse junto con los cambios relevantes.

## Índice de documentos

| Archivo | Contenido |
|---|---|
| `00-ARQUITECTURA-Y-SEGURIDAD.md` | Este archivo. Visión general, stack, estructura, multi-tenancy, auth, convenciones |
| `01-BASE-DE-DATOS.md` | Esquema completo de PostgreSQL, relaciones, migraciones y datos de prueba |
| `02-API-REFERENCE.md` | Referencia de todos los endpoints REST, por módulo |
| `03-REGLAS-DE-NEGOCIO-Y-PENDIENTES.md` | Reglas de negocio por módulo, configuración de entornos, pendientes y roadmap |

---

## 1. ¿Qué es Solventa?

Solventa es un **CRM multi-tenant para SMBs mexicanas** (ferreterías,
constructoras, despachos, talleres, etc.). El MVP cubre el flujo comercial
básico:

```
Lead (prospecto) → Customer (cliente) → Quote (cotización) → Seguimiento
```

Cada empresa que se registra es un **tenant** independiente. Todos los datos
(leads, clientes, cotizaciones, etc.) están aislados por tenant — ninguna
empresa puede ver datos de otra.

---

## 2. Stack tecnológico

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 (target en `pom.xml`), corre sobre JDK 23 en dev |
| Framework | Spring Boot | 3.5.0 |
| Build | Maven | — |
| Base de datos | PostgreSQL | 13+ (probado en dev con 13.18) |
| Migraciones | Flyway | 11.7.2 |
| ORM | Hibernate / Spring Data JPA | (incluido en Spring Boot 3.5.0) |
| Autenticación | JWT (jjwt) | 0.12.6 |
| Hashing de contraseñas | BCrypt (Spring Security) | factor 12 |
| Documentación API | springdoc-openapi | 2.8.8 (ver nota en pendientes) |
| Boilerplate | Lombok | — |
| Email (preparado, no usado en prod) | spring-boot-starter-mail | — |
| Pool de conexiones | HikariCP | — |

**Paquete raíz:** `com.solventa.solventa_backend`

---

## 3. Estructura del proyecto

El backend es un **monolito modular**: cada dominio de negocio es un paquete
independiente con sus propias capas (`model`, `dto`, `repository`, `service`,
`controller`). Esto facilita que en el futuro se pueda extraer un módulo a un
microservicio si fuera necesario, sin reescribir todo.

```
com.solventa.solventa_backend
│
├── SolventaBackendApplication.java   (@SpringBootApplication, @EnableScheduling)
│
├── auth/                              Autenticación y JWT
│   ├── controller/  AuthController
│   ├── dto/          RegisterRequest, LoginRequest, RefreshTokenRequest,
│   │                  ForgotPasswordRequest, ResetPasswordRequest, AuthResponse
│   ├── service/      AuthService
│   └── util/         JwtUtil, JwtAuthFilter
│
├── tenant/                            La "empresa" (raíz del multi-tenant)
│   ├── model/        Company
│   └── repository/   CompanyRepository
│
├── users/                             Usuarios del sistema
│   ├── model/        User, Role (enum)
│   └── repository/   UserRepository
│   (sin service/controller propio — gestionado vía AuthService.
│    Ver pendientes: falta módulo de gestión de equipo)
│
├── leads/                             Prospectos comerciales
│   ├── model/        Lead (+ enums LeadSource, LeadStatus, LeadPriority)
│   ├── dto/          LeadRequest, LeadResponse, LeadStatusRequest,
│   │                  ConvertLeadResponse
│   ├── repository/   LeadRepository
│   ├── service/      LeadService
│   └── controller/   LeadController
│
├── customers/                         Clientes
│   ├── model/        Customer
│   ├── dto/          CustomerRequest, CustomerResponse
│   ├── repository/   CustomerRepository
│   ├── service/      CustomerService
│   └── controller/   CustomerController
│
├── followups/                         Seguimientos y recordatorios (polimórfico)
│   ├── model/        FollowUp (+ enums EntityType, FollowUpType), Reminder
│   ├── dto/          FollowUpRequest/Response, ReminderRequest/Response
│   ├── repository/   FollowUpRepository, ReminderRepository
│   ├── service/      FollowUpService, ReminderService
│   └── controller/   FollowUpController, ReminderController
│
├── quotes/                             Cotizaciones
│   ├── model/        Quote (+ enum QuoteStatus), QuoteLine, QuoteStatusHistory
│   ├── dto/          QuoteLineRequest/Response, QuoteRequest/Response,
│   │                  QuoteStatusRequest, QuoteStatusHistoryResponse
│   ├── repository/   QuoteRepository, QuoteStatusHistoryRepository,
│   │                  QuoteStatusAggregation (proyección)
│   ├── service/      QuoteService, QuoteExpirationJob (@Scheduled)
│   └── controller/   QuoteController
│
├── dashboard/                          Métricas agregadas
│   ├── dto/          DashboardStatsResponse
│   ├── service/      DashboardService
│   └── controller/   DashboardController
│
└── shared/                             Código transversal
    ├── config/       SecurityConfig, OpenApiConfig, WebMvcConfig
    ├── context/      TenantContext (ThreadLocal)
    ├── dto/          ApiResponse<T>
    ├── exception/    BusinessException, GlobalExceptionHandler
    └── util/         SecurityUtils
```

---

## 4. Multi-tenancy — cómo funciona el aislamiento de datos

Este es el mecanismo de seguridad más importante del sistema. Se basa en
**tres piezas que trabajan juntas en cada request**:

### 4.1. La columna `tenant_id`

Todas las tablas de negocio (`leads`, `customers`, `quotes`, `follow_ups`,
`reminders`, `users`) tienen una columna `tenant_id` que apunta a `companies.id`.

### 4.2. `TenantContext` — ThreadLocal

`shared/context/TenantContext.java` es un `ThreadLocal<UUID>` muy simple:

```java
TenantContext.setTenantId(uuid);
TenantContext.getTenantId();
TenantContext.clear();
```

Como cada request HTTP en Spring Boot se procesa en su propio hilo, esto
permite que cualquier `Service` obtenga "el tenant actual" sin tener que
pasarlo como parámetro por todas las capas.

### 4.3. `JwtAuthFilter` — quién llena el `TenantContext`

`auth/util/JwtAuthFilter.java` se ejecuta en **cada request** (antes del
`UsernamePasswordAuthenticationFilter`). Su trabajo:

1. Lee el header `Authorization: Bearer <token>`
2. Valida el JWT con `JwtUtil`
3. Extrae `userId`, `tenantId` y `role` de los claims del token
4. Llama a `TenantContext.setTenantId(tenantId)`
5. Llena el `SecurityContextHolder` con `principal = userId.toString()` y
   `authority = "ROLE_" + role`
6. **En el `finally`**, limpia ambos contextos — esto es crítico porque los
   hilos se reutilizan (thread pool de Tomcat); si no se limpia, un request
   podría heredar el tenant de un request anterior.

### 4.4. Cómo se usa en los repositorios

**Cada query de cada repositorio recibe `tenantId` explícitamente como
parámetro** — no hay un filtro automático de Hibernate (`@TenantId` /
`@Filter`). Ejemplo típico:

```java
@Query("SELECT l FROM Lead l WHERE l.tenant.id = :tenantId AND l.deletedAt IS NULL ...")
Page<Lead> findAllFiltered(@Param("tenantId") UUID tenantId, ...);
```

Y en el servicio:

```java
UUID tenantId = TenantContext.getTenantId();
return leadRepository.findAllFiltered(tenantId, ...);
```

**Por qué esta decisión**: es más verboso, pero es explícito y fácil de
auditar — cualquier query que toque una tabla de negocio debe recibir
`tenantId` o el revisor de código lo nota inmediatamente. Los filtros
automáticos de Hibernate son más "mágicos" y más fáciles de olvidar activar.

### 4.5. Validación realizada

Se probó registrando una segunda empresa ("Empresa Test B") y haciendo
`GET /api/leads` con su token — el resultado fue `totalElements: 0`,
confirmando que el aislamiento funciona correctamente.

⚠️ **Ver sección de pendientes** — hay un caso no cubierto: al asignar
`assignedToId` en Leads/Customers, no se valida que ese usuario pertenezca al
mismo tenant.

---

## 5. Autenticación y seguridad

### 5.1. Flujo de JWT

```
POST /api/auth/register  →  crea Company + User(ADMIN)  →  retorna accessToken + refreshToken
POST /api/auth/login      →  valida email+password        →  retorna accessToken + refreshToken
POST /api/auth/refresh    →  valida refreshToken           →  retorna nuevo accessToken + refreshToken
```

- **Algoritmo**: HMAC-SHA (HS512 vía `Keys.hmacShaKeyFor`)
- **Claims del token**: `sub` (userId), `tenantId`, `role`, `iat`, `exp`
- **Duración accessToken**: 8 horas (`app.jwt.expiration-ms = 28800000`)
- **Duración refreshToken**: 30 días (`app.jwt.refresh-expiration-ms = 2592000000`)
- **Secret**: en `dev` está hardcodeado en `application-dev.yml` (≥64
  caracteres, requerido por HS512). En `prod` viene de la variable de entorno
  `JWT_SECRET`.

`auth/util/JwtUtil.java` centraliza generación y validación:

```java
generateAccessToken(userId, tenantId, role)
generateRefreshToken(userId, tenantId, role)
isValid(token)
extractUserId(token)
extractTenantId(token)
extractRole(token)
```

### 5.2. `SecurityConfig`

`shared/config/SecurityConfig.java`:

- **Stateless** (`SessionCreationPolicy.STATELESS`) — no hay sesiones de
  servidor, todo viaja en el JWT.
- **CSRF deshabilitado** (no aplica a APIs stateless con JWT).
- **CORS**: configurado vía `app.cors.allowed-origins` (en dev:
  `http://localhost:5173`).
- **Rutas públicas** (`permitAll`):
  - `/api/auth/**`
  - `/actuator/health`
  - Rutas de Swagger/webjars (aunque la UI nunca terminó de cargar — ver
    pendientes)
- **Todo lo demás requiere `authenticated()`**.
- `JwtAuthFilter` se registra con
  `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`.

### 5.3. Roles

Dos roles definidos en `users/model/Role.java`:

| Rol | Descripción |
|---|---|
| `ADMIN` | Se asigna automáticamente al usuario que registra la empresa |
| `SELLER` | Vendedor — se crea actualmente solo vía seed SQL (no hay endpoint de invitación todavía) |

⚠️ **Importante**: actualmente los roles se incluyen en el JWT
(`ROLE_ADMIN` / `ROLE_SELLER`) pero **ningún endpoint usa `@PreAuthorize` o
restricciones por rol**. Cualquier usuario autenticado de un tenant puede
hacer cualquier operación dentro de ese tenant. Ver pendientes.

### 5.4. Passwords

- BCrypt con `strength = 12` (`new BCryptPasswordEncoder(12)`)
- Mínimo 8 caracteres (validación `@Size(min = 8)` en los DTOs)

---

## 6. Convenciones de código

Estas convenciones se repiten en los 8 módulos — conocerlas acelera mucho
la lectura del código.

### 6.1. `ApiResponse<T>` — wrapper de respuesta

Todas las respuestas (éxito y error) usan `shared/dto/ApiResponse.java`:

```json
{
  "success": true,
  "message": "Mensaje opcional",
  "data": { }
}
```

Factories:
```java
ApiResponse.ok(data)
ApiResponse.ok("mensaje", data)
ApiResponse.error("mensaje")
```

### 6.2. `BusinessException` — errores de negocio

`shared/exception/BusinessException.java` extiende `RuntimeException` y
lleva un `HttpStatus`. Factories:

```java
BusinessException.notFound("Lead")      → 404
BusinessException.conflict("...")       → 409
BusinessException.badRequest("...")     → 400
BusinessException.forbidden("...")      → 403
```

Todas son capturadas por `GlobalExceptionHandler` y convertidas a
`ApiResponse.error(...)` con el status correspondiente.

### 6.3. Patrón DTO `.from(entity)`

Cada `*Response` DTO tiene un método estático `from(Entity)` que hace el
mapeo manual (no se usa MapStruct ni ModelMapper):

```java
public static LeadResponse from(Lead lead) {
    return LeadResponse.builder()
            .id(lead.getId())
            .name(lead.getName())
            // ...
            .build();
}
```

### 6.4. Soft delete

`leads`, `customers` y `quotes` usan **soft delete** vía columna
`deleted_at` (timestamp nullable). Los repositorios siempre filtran
`deletedAt IS NULL`. `follow_ups`, `reminders` y `quote_lines` usan
**hard delete** (no tienen esta columna).

### 6.5. `@EntityGraph` para evitar `LazyInitializationException`

Como `spring.jpa.open-in-view = false`, cualquier relación `@ManyToOne(LAZY)`
que se necesite leer fuera de la query original debe traerse con
`@EntityGraph(attributePaths = {...})` en el método del repositorio. Ejemplo:

```java
@EntityGraph(attributePaths = {"assignedTo", "tenant"})
Optional<Lead> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
```

### 6.6. Búsquedas de texto con `CAST(... AS string)`

PostgreSQL no puede aplicar `LOWER()` a un parámetro `NULL` sin tipo (lanza
`function lower(bytea) does not exist`). La solución usada en todos los
repositorios con búsqueda (`Lead`, `Customer`, `Quote`):

```java
LOWER(CAST(l.name AS string)) LIKE LOWER(CAST(CONCAT('%', :q, '%') AS string))
```

### 6.7. Obtener el usuario autenticado

`shared/util/SecurityUtils.getCurrentUserId()` extrae el `userId` (UUID)
desde el `SecurityContextHolder` (lo puso `JwtAuthFilter`). Se usa en
`FollowUpService`, `ReminderService` y `QuoteService` para registrar
"quién hizo qué".

### 6.8. Logging

Cada `Service` usa `@Slf4j` (Lombok) y registra eventos importantes con
`log.info(...)` — creaciones, conversiones, cambios de estado, eliminaciones.
Útil para debugging en dev ya que Swagger UI no está disponible.
