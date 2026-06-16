# Solventa CRM — Reglas de Negocio, Configuración y Pendientes (MVP v1.0)

---

## 1. Reglas de negocio por módulo

### 1.1. Auth

- El **registro crea una empresa nueva cada vez** — no hay forma de que un
  segundo usuario se una a una empresa existente (eso requeriría el flujo de
  invitación, que no está implementado — ver sección 4).
- La verificación de email duplicado en `register` es **global**
  (`userRepository.findByEmail(email)`), no por tenant. Es decir: si
  `carlos@x.com` ya registró una empresa, no puede usar ese mismo email para
  registrar una segunda empresa — recibirá `409 Conflict`. Esto es una
  decisión de diseño (un email = una cuenta = una empresa en el MVP), pero
  vale la pena confirmarlo con el equipo de producto si en el futuro un
  mismo usuario debe poder pertenecer a varias empresas.
- `forgot-password` **siempre responde 200** (incluso si el email no existe)
  para no filtrar qué emails están registrados. El token se genera con
  `UUID.randomUUID()`, expira en 30 minutos, y por ahora **solo se imprime en
  el log** (`log.info("Reset token generado para: {} | Token: {}", ...)`).
  No hay envío real de correo.

---

### 1.2. Leads

**Estados**: `NEW → CONTACTED → QUALIFIED → CONVERTED | DISCARDED`

- El orden de los estados **no se valida estrictamente** (se puede pasar de
  `NEW` a `QUALIFIED` directo, o de `QUALIFIED` a `CONTACTED`). La única
  regla dura es:
  > **Un lead en estado `CONVERTED` no puede cambiar de estado nunca más**
  > (`PATCH /leads/{id}/status` retorna `400`).
- **Conversión a cliente** (`POST /leads/{id}/convert`):
  1. Valida que el lead no esté ya `CONVERTED` (`409` si lo está).
  2. Crea un `Customer` copiando `name`, `company`, `email`, `phone`,
     `notes`, `assignedTo` del lead, y enlaza `customer.lead_id = lead.id`.
  3. Marca `lead.status = CONVERTED`.
  4. Retorna `{ customerId, message }`.
- **Búsqueda** (`q`): busca coincidencia parcial (case-insensitive) en
  `name`, `company` o `email`.
- **No hay validación de email duplicado** entre leads — a propósito (ver
  conversación original: un mismo prospecto puede llegar por varios canales
  y generar varios leads).
- **Soft delete**: `DELETE` pone `deleted_at = NOW()`. El registro sigue en
  la BD para auditoría, pero no aparece en ningún listado ni se puede
  obtener por ID (`404`).

---

### 1.3. Customers

- **Email único por tenant** — al crear o actualizar, si el email ya
  pertenece a otro cliente activo del mismo tenant, retorna `409 Conflict`.
- `leadId` es de **solo lectura desde la API** — se establece únicamente al
  convertir un lead (no hay forma de "vincular" manualmente un cliente
  existente a un lead después de creado).
- Mismo patrón de soft delete que Leads.

---

### 1.4. FollowUps & Reminders

Ambos módulos comparten el mismo diseño **polimórfico**:
`entity_type` (`LEAD` | `CUSTOMER`) + `entity_id` (UUID).

- **Validación de integridad**: antes de crear cualquier `FollowUp` o
  `Reminder`, el servicio verifica que `entity_id` exista, no esté eliminado
  (`deleted_at IS NULL`) y pertenezca al tenant actual — usando
  `LeadRepository` o `CustomerRepository` según `entity_type`. Si no existe,
  `404`.
  > Esto es la única defensa contra que alguien cree un seguimiento/
  > recordatorio apuntando a un `entity_id` de otro tenant — es importante
  > no quitarla.
- El campo `user` se llena automáticamente con
  `SecurityUtils.getCurrentUserId()` — **no viene en el request body**.
- **FollowUp**: hard delete, sin estado, ordenado por `interactionDate DESC`
  al listar.
- **Reminder**: tiene `isDone` (default `false`) y endpoint
  `PATCH /{id}/complete`. El endpoint `GET /reminders/today` filtra por
  `user_id = usuario en sesión` — **cada usuario solo ve sus propios
  recordatorios de hoy**, no los de todo el equipo.

---

### 1.5. Quotes — el módulo más complejo

#### Cálculo de totales

`Quote.recalculateTotals()` (se llama en `create` y `update`):

```
subtotal = SUM(line.quantity * line.unitPrice)  redondeado a 2 decimales
total    = subtotal × (1 - discountPct/100) × (1 + taxPct/100)  redondeado a 2 decimales
```

Ejemplo verificado:
```
Línea 1: 3 × 850   = 2550
Línea 2: 5 × 320   = 1600
subtotal           = 4150.00
con 5% descuento   = 4150 × 0.95 = 3942.50
con 16% IVA        = 3942.50 × 1.16 = 4573.30  ← total
```

`taxPct` default = `16.00` (IVA estándar México). `discountPct` default = `0`.

#### Máquina de estados

```
        ┌────────┐   PATCH status=SENT    ┌──────┐
        │ DRAFT  │ ─────────────────────► │ SENT │
        └────────┘                        └──────┘
            │                                 │
            │ (editable / eliminable)         │ PATCH status=WON|LOST|EXPIRED
            │                                 ▼
            │                          ┌─────────────────────┐
            └─────────────────────────►│ WON | LOST | EXPIRED │  ← terminales
                                        └─────────────────────┘
```

Reglas implementadas en `QuoteService.validateTransition()`:

| Desde | Hacia permitido |
|---|---|
| `DRAFT` | `SENT` únicamente |
| `SENT` | `WON`, `LOST`, `EXPIRED` |
| `WON` / `LOST` / `EXPIRED` | ninguno (terminal) |

Cualquier otra combinación (incluyendo "cambiar al mismo estado actual")
retorna `400 Bad Request` con un mensaje descriptivo.

#### Edición y eliminación

- `PUT /quotes/{id}` y `DELETE /quotes/{id}` **solo funcionan si
  `status = DRAFT`**. Una vez `SENT`, la cotización es inmutable (excepto su
  `status`).
- Al actualizar, **todas las líneas se reemplazan**: `quote.getLines().clear()`
  + se vuelven a construir desde el request. Gracias a
  `orphanRemoval = true` en la relación `@OneToMany`, Hibernate borra las
  líneas viejas automáticamente.

#### Historial de estados

Cada llamada exitosa a `PATCH /quotes/{id}/status` inserta un registro en
`quote_status_history` con `oldStatus`, `newStatus`, `changedBy` (usuario del
JWT) y `changedAt = now()`. Es **append-only** — nunca se actualiza ni borra.

#### Job de vencimiento automático

`quotes/service/QuoteExpirationJob.java`:

```java
@Scheduled(cron = "0 0 * * * *") // cada hora en punto
```

- Busca **todas** las cotizaciones (de **todos los tenants** — el job corre
  fuera de un request HTTP, no hay `TenantContext`) con
  `status = SENT AND expiresAt < CURRENT_DATE AND deletedAt IS NULL`.
- Las marca como `EXPIRED`.

> ⚠️ **Pendiente**: el job NO inserta un registro en
> `quote_status_history` al expirar automáticamente — el historial de esa
> cotización quedará "incompleto" (su último registro será `→ SENT`, pero el
> estado actual es `EXPIRED`). Ver sección de pendientes para la corrección
> sugerida.

---

### 1.6. Dashboard

- Es **puramente de lectura** — no tiene entidades propias, solo agrega
  datos de Leads, Customers, Quotes y Reminders mediante queries de
  agregación (`GROUP BY`, `COUNT`, `SUM`).
- La decisión de **no usar vistas SQL** fue intencional: las vistas no se
  parametrizan bien por tenant dentro de Flyway, y las queries JPQL con
  proyecciones (`QuoteStatusAggregation`) son igual de eficientes y más
  fáciles de versionar.
- `quotesByStatus` siempre trae las 5 keys inicializadas en `{count:0,
  total:0}` antes de sobrescribir con los resultados reales — esto evita que
  el frontend tenga que manejar `undefined` para estados sin cotizaciones.

---

## 2. Configuración y entornos

### 2.1. Archivos de configuración

```
src/main/resources/
├── application.yml        Config base compartida (perfiles, JPA, Flyway, JWT exp, CORS, actuator, springdoc)
├── application-dev.yml     Perfil de desarrollo
└── application-prod.yml    Perfil de producción
```

Perfil activo por defecto: `dev` (`spring.profiles.active: dev` en
`application.yml`).

### 2.2. `application-dev.yml` — resumen

| Config | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/solventa_dev` |
| `spring.datasource.username/password` | `postgres` / `postgres` |
| `spring.jpa.show-sql` | `true` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `app.jwt.secret` | hardcodeado (≥64 chars, **solo para dev**) |
| `spring.mail.*` | Mailtrap (sandbox SMTP — no llega a bandejas reales) |
| `logging.level` | `DEBUG` para `com.solventa`, Hibernate SQL, Spring Security |
| `server.port` | `8080` |

### 2.3. `application-prod.yml` — resumen

Todo viene de **variables de entorno**:

```bash
DB_URL=jdbc:postgresql://host:5432/solventa_prod
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...          # generar con: openssl rand -base64 64
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=...
CORS_ALLOWED_ORIGINS=https://solventaio.com
PORT=8080
```

Diferencias clave vs dev:
- `logging.level.root: WARN`, logs a archivo (`/var/log/solventa/app.log`,
  rotación 10MB/30 días)
- `server.error.include-message: never` / `include-stacktrace: never` — no
  se filtra información interna en errores
- `springdoc.api-docs.enabled: false` /
  `springdoc.swagger-ui.enabled: false`
- `server.compression.enabled: true`

### 2.4. Cómo correr el proyecto

```bash
# Dev (perfil por defecto)
mvn spring-boot:run

# Prod
java -jar solventa-backend.jar --spring.profiles.active=prod
# o
SPRING_PROFILES_ACTIVE=prod java -jar solventa-backend.jar
```

Requisitos: PostgreSQL corriendo, base de datos `solventa_dev` creada
(Flyway crea las tablas, no la base de datos en sí).

---

## 3. Testing

No hay tests automatizados (JUnit) — ver pendientes. El testing actual es
**manual vía archivo `.http`** (cliente HTTP nativo de IntelliJ).

Flujo recomendado para probar todo el sistema:

1. `POST /auth/login` con `roberto@ferreterialopez.mx` / `Admin123` →
   guardar `accessToken` en variable `@token`.
2. Probar cada módulo en orden: Leads → Customers → FollowUps/Reminders →
   Quotes → Dashboard.
3. **Test de aislamiento multi-tenant** (crítico, correr después de cualquier
   cambio en `TenantContext`/`JwtAuthFilter`/repositorios):
   - Registrar una segunda empresa (`POST /auth/register` con otro email).
   - Hacer `GET /leads` con el token de esa segunda empresa.
   - **Debe retornar `totalElements: 0`** — si retorna datos de la primera
     empresa, hay una fuga de datos entre tenants.

---

## 4. Pendientes conocidos (no son bugs del MVP, son alcance futuro)

### 4.1. Seguridad / permisos

- **No hay autorización por rol** (`@PreAuthorize`). El JWT incluye
  `ROLE_ADMIN` / `ROLE_SELLER` pero ningún endpoint los usa. Actualmente
  cualquier usuario autenticado de un tenant puede hacer cualquier operación
  (incluyendo, por ejemplo, que un `SELLER` elimine cotizaciones de otros
  vendedores).
- **`assignedToId` sin validar tenant**: en `LeadService.create/update` y
  `CustomerService.create/update`, se hace
  `userRepository.findById(UUID.fromString(req.getAssignedToId()))` **sin
  verificar que ese usuario pertenezca al mismo tenant**. Si alguien envía
  el UUID de un usuario de otra empresa (adivinándolo), quedaría asignado.
  Fix sugerido: agregar `findByIdAndTenantId` en `UserRepository` y usarlo
  en ambos servicios.

### 4.2. Funcionalidad incompleta

- **Invitación de usuarios**: las columnas `invitation_token`,
  `invitation_expires_at`, `invitation_accepted` existen en `users` y la
  carpeta `users/` tiene `model` y `repository`, pero **no hay
  `UserService` ni `UserController`**. No existe forma de que un ADMIN
  invite a un SELLER desde la API — los usuarios SELLER solo existen porque
  se insertaron en el seed SQL.
- **Envío real de emails**: `spring-boot-starter-mail` está en el `pom.xml`
  y configurado con Mailtrap en dev, pero `AuthService.forgotPassword()`
  solo hace `log.info(...)` con el token. Falta integrar `JavaMailSender`
  (o SendGrid en prod) para enviar el link de recuperación.
- **`audit_logs`**: tabla creada en `V1__init.sql`, sin entidad ni
  repositorio ni lógica que la escriba. Si se va a usar, decidir si será vía
  `@Aspect` (AOP) genérico o llamadas explícitas en cada servicio.

### 4.3. Inconsistencias menores

- **`QuoteExpirationJob` no escribe en `quote_status_history`** al pasar
  `SENT → EXPIRED` automáticamente. Fix sugerido: inyectar
  `QuoteStatusHistoryRepository` en el job y guardar un registro con
  `changedBy = null` (representa "Sistema" — `QuoteStatusHistoryResponse.from()`
  ya maneja `changedBy == null` mostrando `"Sistema"`).
- **`/reminders/today` depende de la zona horaria del servidor**
  (`OffsetDateTime.now()`). Si el servidor de producción corre en UTC y los
  usuarios están en México (UTC-6), "hoy" para el sistema y "hoy" para el
  usuario pueden no coincidir cerca de la medianoche. Considerar pasar la
  zona horaria del usuario como parámetro o estandarizar el servidor a
  `America/Mexico_City`.
- **Swagger UI nunca funcionó** pese a tener `springdoc-openapi-starter-webmvc-ui`
  configurado (`OpenApiConfig`, `WebMvcConfig`, rutas públicas en
  `SecurityConfig`, `ant_path_matcher`). Quedó en `404`/`No static resource`.
  El equipo usa `.http` / Postman. Si se retoma, probar primero con un
  proyecto Spring Boot 3.5 + springdoc 2.8.x limpio para descartar conflicto
  de versiones antes de depurar la configuración existente.

### 4.4. Sin tests automatizados

No hay `src/test/java` con contenido relevante — `spring-boot-starter-test`
y `spring-security-test` están en el `pom.xml` pero sin tests escritos. Antes
de escalar el equipo de backend, priorizar al menos:
- Tests de integración del flujo de auth (register/login/refresh)
- Test de aislamiento multi-tenant (automatizar el test manual de la sección 3)
- Tests de la máquina de estados de `Quote`

---

## 5. Roadmap sugerido (orden recomendado)

1. **Conectar el frontend** a los 4 módulos que aún usan `placeholderData`:
   Customers, Quotes, FollowUps/Reminders, Dashboard (Leads y Auth ya están
   conectados).
2. **Fix de seguridad**: validar tenant en `assignedToId` (sección 4.1).
3. **Permisos por rol** (`@PreAuthorize` en endpoints sensibles: eliminar,
   cambiar estado, ver datos de otros vendedores).
4. **Módulo de gestión de usuarios**: `UserController` con invitación,
   listado de equipo, activar/desactivar usuarios.
5. **Envío real de emails** (reset password + invitaciones) vía SendGrid.
6. **Tests automatizados** — mínimo los 3 mencionados en 4.4.
7. **Fix `QuoteExpirationJob`** — registrar en `quote_status_history`.
8. **Implementar `audit_logs`** si el negocio lo requiere para
  cumplimiento/trazabilidad.
9. **Deploy a producción**: Railway o VPS + dominio `solventaio.com` +
  HTTPS + monitoreo (Sentry / Uptime Robot) — según lo planeado en el
  documento de ejecución del MVP.

---

## 6. Glosario rápido para un dev nuevo

| Término | Significado en este proyecto |
|---|---|
| **Tenant** | Una empresa registrada (`companies` row). Todo dato de negocio pertenece a un tenant |
| **TenantContext** | ThreadLocal que guarda el `tenant_id` del request actual |
| **Soft delete** | `deleted_at IS NOT NULL` = "eliminado" pero conservado en BD |
| **Entidad polimórfica** | `FollowUp`/`Reminder`: un mismo registro puede referirse a un `Lead` o a un `Customer` vía `entity_type` + `entity_id` |
| **Pipeline abierto** | Cotizaciones en `DRAFT` o `SENT` — aún no cerradas (ganadas/perdidas/vencidas) |
| **Estado terminal** | `CONVERTED` (Lead) o `WON`/`LOST`/`EXPIRED` (Quote) — no admiten más cambios de estado |
