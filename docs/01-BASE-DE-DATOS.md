# Solventa CRM — Base de Datos (MVP v1.0)

> Ver `00-ARQUITECTURA-Y-SEGURIDAD.md` para el contexto general.

## 1. Migraciones Flyway

```
src/main/resources/db/migration/
├── V1__init.sql        Esquema completo (10 tablas + triggers + extensión pgcrypto)
└── V2__seed_data.sql    Datos de prueba (1 empresa, 3 usuarios, 5 leads, 3 clientes,
                         5 cotizaciones + líneas + historial, 6 seguimientos, 5 recordatorios)
```

Configuración (`application.yml`):
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: false
```

`spring.jpa.hibernate.ddl-auto: validate` en ambos perfiles (dev/prod) —
**Hibernate nunca crea ni modifica tablas**, solo valida que las entidades
coincidan con el esquema. Todo cambio de esquema debe ir en una nueva
migración `V3__...sql`, `V4__...sql`, etc.

---

## 2. Diagrama de relaciones (resumen)

```
companies (tenant)
   │
   ├──< users (tenant_id)
   │       │
   │       ├──< leads (assigned_to)
   │       ├──< customers (assigned_to)
   │       ├──< quotes (created_by)
   │       ├──< follow_ups (user_id)
   │       ├──< reminders (user_id)
   │       └──< quote_status_history (changed_by)
   │
   ├──< leads (tenant_id)
   │       └──> customers (lead_id)  [un lead convertido apunta a su customer]
   │
   ├──< customers (tenant_id)
   │       └──< quotes (customer_id)
   │
   ├──< quotes (tenant_id)
   │       ├──< quote_lines (quote_id)
   │       └──< quote_status_history (quote_id)
   │
   ├──< follow_ups (tenant_id)     [polimórfico: entity_type + entity_id → leads | customers]
   ├──< reminders (tenant_id)      [polimórfico: entity_type + entity_id → leads | customers]
   └──< audit_logs (tenant_id)     [tabla creada pero NO usada por código aún — ver pendientes]
```

---

## 3. Tablas

### 3.1. `companies` — el tenant

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | `gen_random_uuid()` |
| `name` | VARCHAR(200) | |
| `industry` | VARCHAR(100) | nullable |
| `rfc` | VARCHAR(13) | nullable |
| `logo_url` | TEXT | nullable, no usado en MVP |
| `plan` | VARCHAR(20) | `FREE` \| `BASIC` \| `PROFESSIONAL` (default `FREE`) |
| `status` | VARCHAR(20) | `ACTIVE` \| `SUSPENDED` \| `CANCELLED` (default `ACTIVE`) |
| `created_at` / `updated_at` | TIMESTAMPTZ | trigger automático |

Entidad: `tenant/model/Company.java`

---

### 3.2. `users`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `tenant_id` | UUID FK → companies | `ON DELETE CASCADE` |
| `name` | VARCHAR(150) | |
| `email` | VARCHAR(200) | **único por tenant** (`uq_users_email_tenant`) |
| `password_hash` | VARCHAR(255) | BCrypt |
| `role` | VARCHAR(20) | `ADMIN` \| `SELLER` (default `SELLER`) |
| `is_active` | BOOLEAN | default `true` |
| `invitation_token` / `invitation_expires_at` / `invitation_accepted` | — | campos preparados, sin flujo de invitación implementado todavía |
| `reset_token` / `reset_token_expires_at` | — | usados por `forgot-password` / `reset-password` |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Índices: `tenant_id`, `email`, `invitation_token` (parcial, where not null),
`reset_token` (parcial, where not null).

Entidad: `users/model/User.java`, enum `Role`

---

### 3.3. `leads`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `tenant_id` | UUID FK → companies | |
| `assigned_to` | UUID FK → users, nullable | `ON DELETE SET NULL` |
| `name` | VARCHAR(150) | requerido |
| `company` | VARCHAR(200) | nullable |
| `email` | VARCHAR(200) | nullable |
| `phone` | VARCHAR(30) | nullable |
| `source` | VARCHAR(20) | `WHATSAPP` \| `EMAIL` \| `REFERRAL` \| `WEBSITE` \| `PHONE` \| `OTHER` |
| `status` | VARCHAR(20) | `NEW` \| `CONTACTED` \| `QUALIFIED` \| `CONVERTED` \| `DISCARDED` |
| `priority` | VARCHAR(10) | `LOW` \| `MEDIUM` \| `HIGH` |
| `notes` | TEXT | nullable |
| `deleted_at` | TIMESTAMPTZ | soft delete |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Índices: `tenant_id`, `assigned_to`, `(tenant_id, status)`,
`(tenant_id, priority)`, `deleted_at` (parcial, where null).

Entidad: `leads/model/Lead.java`, enums `LeadSource`, `LeadStatus`, `LeadPriority`

---

### 3.4. `customers`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `tenant_id` | UUID FK → companies | |
| `assigned_to` | UUID FK → users, nullable | |
| `lead_id` | UUID FK → leads, nullable | si el cliente vino de un lead convertido |
| `name` | VARCHAR(150) | requerido |
| `company` | VARCHAR(200) | nullable |
| `email` | VARCHAR(200) | nullable, **único por tenant** (`uq_customers_email_tenant`) |
| `phone` | VARCHAR(30) | nullable |
| `address` | TEXT | nullable |
| `rfc` | VARCHAR(13) | nullable |
| `notes` | TEXT | nullable |
| `deleted_at` | TIMESTAMPTZ | soft delete |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Entidad: `customers/model/Customer.java`

---

### 3.5. `quotes`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `tenant_id` | UUID FK → companies | |
| `customer_id` | UUID FK → customers | requerido, `ON DELETE CASCADE` |
| `created_by` | UUID FK → users, nullable | |
| `title` | VARCHAR(300) | requerido |
| `status` | VARCHAR(20) | `DRAFT` \| `SENT` \| `WON` \| `LOST` \| `EXPIRED` (default `DRAFT`) |
| `discount_pct` | NUMERIC(5,2) | 0–100, default 0 |
| `tax_pct` | NUMERIC(5,2) | default 16.00 (IVA México) |
| `subtotal` | NUMERIC(14,2) | calculado |
| `total` | NUMERIC(14,2) | calculado |
| `notes` | TEXT | nullable |
| `issued_at` | DATE | default `CURRENT_DATE` |
| `expires_at` | DATE | requerido, debe ser futura al crear |
| `deleted_at` | TIMESTAMPTZ | soft delete |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Índices: `tenant_id`, `customer_id`, `(tenant_id, status)`,
`expires_at` (parcial, where status='SENT' — usado por el job de vencimiento),
`deleted_at` (parcial, where null).

Entidad: `quotes/model/Quote.java`, enum `QuoteStatus`.
Método clave: `recalculateTotals()` (ver `03-REGLAS-DE-NEGOCIO.md`).

---

### 3.6. `quote_lines`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `quote_id` | UUID FK → quotes | `ON DELETE CASCADE` |
| `description` | VARCHAR(500) | requerido |
| `quantity` | NUMERIC(10,2) | > 0 |
| `unit_price` | NUMERIC(14,2) | ≥ 0 |
| `subtotal` | NUMERIC(14,2) | = `quantity * unit_price` |
| `sort_order` | INTEGER | orden de despliegue |
| `created_at` | TIMESTAMPTZ | |

Sin `updated_at` — las líneas se reemplazan completas en cada `PUT` de la
cotización (`orphanRemoval = true` en la relación `Quote.lines`).

Entidad: `quotes/model/QuoteLine.java`. Método clave: `calculateSubtotal()`.

---

### 3.7. `quote_status_history`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `quote_id` | UUID FK → quotes | `ON DELETE CASCADE` |
| `old_status` | VARCHAR(20) | nullable (primer registro) |
| `new_status` | VARCHAR(20) | requerido |
| `changed_by` | UUID FK → users, nullable | |
| `changed_at` | TIMESTAMPTZ | default `NOW()` |

**Inmutable** — solo se insertan registros, nunca se actualizan ni eliminan.
Se llena desde `QuoteService.changeStatus()`.

⚠️ El job automático de vencimiento (`QuoteExpirationJob`) **no** inserta un
registro aquí al pasar `SENT → EXPIRED` — ver pendientes.

Entidad: `quotes/model/QuoteStatusHistory.java`

---

### 3.8. `follow_ups` — seguimientos (polimórfico)

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `tenant_id` | UUID FK → companies | |
| `user_id` | UUID FK → users, nullable | quién registró el seguimiento |
| `entity_type` | VARCHAR(20) | `LEAD` \| `CUSTOMER` |
| `entity_id` | UUID | **sin FK formal** — apunta a `leads.id` o `customers.id` según `entity_type` |
| `type` | VARCHAR(20) | `CALL` \| `EMAIL` \| `MEETING` \| `WHATSAPP` \| `OTHER` |
| `interaction_date` | TIMESTAMPTZ | requerido |
| `notes` | TEXT | requerido |
| `result` | TEXT | nullable |
| `created_at` | TIMESTAMPTZ | |

Sin soft delete — `DELETE` es físico.

La integridad referencial de `entity_id` se valida **a nivel aplicación**
(`FollowUpService.validateEntityExists`), no a nivel de base de datos —
es la naturaleza de un diseño polimórfico sin FK real.

Índices: `tenant_id`, `(entity_type, entity_id)`, `user_id`,
`interaction_date DESC`.

Entidad: `followups/model/FollowUp.java`, enums `EntityType`, `FollowUpType`
(`EntityType` es compartido con `Reminder`).

---

### 3.9. `reminders` — recordatorios (polimórfico)

| Columna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `tenant_id` | UUID FK → companies | |
| `user_id` | UUID FK → users | `ON DELETE CASCADE` — el recordatorio es de un usuario |
| `entity_type` | VARCHAR(20) | `LEAD` \| `CUSTOMER` |
| `entity_id` | UUID | igual que en `follow_ups`, sin FK formal |
| `remind_at` | TIMESTAMPTZ | requerido |
| `description` | TEXT | requerido |
| `is_done` | BOOLEAN | default `false` |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Índice especial:
```sql
CREATE INDEX idx_reminders_user_today ON reminders(tenant_id, user_id, remind_at)
    WHERE is_done = FALSE;
```
Optimizado para la query de "recordatorios de hoy" del Dashboard.

Entidad: `followups/model/Reminder.java`

---

### 3.10. `audit_logs` ⚠️ no implementado

| Columna | Tipo |
|---|---|
| `id` | UUID PK |
| `tenant_id` | UUID FK → companies, nullable |
| `user_id` | UUID FK → users, nullable |
| `action` | `CREATE` \| `UPDATE` \| `DELETE` \| `LOGIN` \| `LOGOUT` |
| `entity_type` | VARCHAR(50) |
| `entity_id` | UUID, nullable |
| `old_value` / `new_value` | JSONB |
| `ip_address` | VARCHAR(45) |
| `created_at` | TIMESTAMPTZ |

**La tabla existe en el esquema (V1) pero ningún código la escribe.** No hay
entidad Java ni repositorio. Si se decide implementar auditoría, esta tabla
ya está lista — solo falta la capa de aplicación (probablemente un
`AOP @Aspect` o un listener de Hibernate `@PostUpdate`/`@PostPersist`).

---

## 4. Triggers

`update_updated_at()` — función PL/pgSQL que actualiza `updated_at = NOW()`
en cada `UPDATE`. Aplicada vía `BEFORE UPDATE` trigger a:
`companies`, `users`, `leads`, `customers`, `quotes`, `reminders`.

`quote_lines` y `follow_ups` no tienen `updated_at` (se reemplazan/eliminan
completos, no se editan in-place).

---

## 5. Extensiones de PostgreSQL

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```
Provee `gen_random_uuid()`, usado como `DEFAULT` en todas las PKs.

---

## 6. Datos de prueba (V2__seed_data.sql)

El seed crea **un tenant completo y funcional** para desarrollo:

### Empresa
| Campo | Valor |
|---|---|
| `id` | `a1b2c3d4-0000-0000-0000-000000000001` |
| `name` | Ferreteria Lopez SA de CV |
| `plan` | BASIC |

### Usuarios — contraseña para todos: `Admin123`

| Nombre | Email | Rol | ID |
|---|---|---|---|
| Roberto Lopez | `roberto@ferreterialopez.mx` | ADMIN | `b1b2c3d4-...0001` |
| Ana Martinez | `ana@ferreterialopez.mx` | SELLER | `b1b2c3d4-...0002` |
| Carlos Herrera | `carlos@ferreterialopez.mx` | SELLER | `b1b2c3d4-...0003` |

> Hash BCrypt usado: `$2a$12$OE41w9finVVKOjDpom/4QecHCbTxpUAcYXW1bq.YDaP55W/PtqQ7.`
> (corresponde a la contraseña `Admin123` con factor 12)

### Leads (5)
1. Mario Sanchez — Constructora Sanchez — `NEW` / `HIGH`
2. Patricia Vega — Remodelaciones Vega — `CONTACTED` / `MEDIUM`
3. Jorge Fuentes — Taller Fuentes — `QUALIFIED` / `HIGH`
4. Sofia Ramirez — (persona física, sin empresa) — `NEW` / `LOW`
5. Alejandro Torres — Desarrolladora Torres — `CONVERTED` / `HIGH`
   *(este lead generó el customer #1)*

### Clientes (3)
1. Alejandro Torres — Desarrolladora Torres — `lead_id` = lead #5
2. Inmobiliaria del Norte SA — cliente corporativo, sin lead de origen
3. Acabados Premium SA — distribuidor, sin lead de origen

### Cotizaciones (5)
| Título | Cliente | Estado | Total |
|---|---|---|---|
| Material construcción — Proyecto 8 Deptos GDL | Torres | `SENT` | $203,548.00 |
| Pedido mensual noviembre — Inmobiliaria del Norte | Inmobiliaria | `WON` | $88,066.80 |
| Herramienta eléctrica y consumibles Q4 | Acabados Premium | `DRAFT` | $49,300.00 |
| Acabados y pisos — Torre A | Torres | `LOST` | $110,200.00 |
| Pedido diciembre — Inmobiliaria del Norte | Inmobiliaria | `DRAFT` | $92,574.40 |

Cada cotización con `SENT`/`WON`/`LOST` tiene su(s) registro(s)
correspondiente(s) en `quote_status_history`.

### Seguimientos (6) y Recordatorios (5)
Distribuidos entre los leads y clientes anteriores, asignados a Ana y Carlos.
Los recordatorios usan `NOW() + INTERVAL` por lo que **siempre aparecen como
"de hoy"** sin importar cuándo se corra el seed — útil para probar
`/api/reminders/today` y el Dashboard en cualquier momento.

---

## 7. Cómo resetear la base de datos en desarrollo

```sql
DROP DATABASE solventa_dev;
CREATE DATABASE solventa_dev;
```

Al reiniciar Spring Boot, Flyway vuelve a correr `V1` y `V2` desde cero.
