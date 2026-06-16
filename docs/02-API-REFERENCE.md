# Solventa CRM — Referencia de API (MVP v1.0)

> Base URL: `http://localhost:8080/api` (dev). En producción según
> `CORS_ALLOWED_ORIGINS` / dominio configurado.
>
> Todas las respuestas usan el wrapper `ApiResponse<T>`:
> ```json
> { "success": true, "message": "...", "data": { } }
> ```
>
> Todos los endpoints, **excepto `/auth/**`**, requieren:
> ```
> Authorization: Bearer <accessToken>
> ```

---

## 1. Auth — `/api/auth` (público, sin token)

| Método | Ruta | Body | Descripción |
|---|---|---|---|
| POST | `/auth/register` | `RegisterRequest` | Crea Company + User(ADMIN). Retorna `AuthResponse` (201) |
| POST | `/auth/login` | `LoginRequest` | Retorna `AuthResponse` (200) |
| POST | `/auth/refresh` | `RefreshTokenRequest` | Retorna nuevo `AuthResponse` (200) |
| POST | `/auth/forgot-password` | `ForgotPasswordRequest` | Siempre 200. Genera `reset_token` (30 min), se loguea en consola (no envía email real) |
| POST | `/auth/reset-password` | `ResetPasswordRequest` | Cambia password con el token recibido |

### `RegisterRequest`
```json
{
  "companyName": "string (2-200)",
  "name": "string (2-150)",
  "email": "email válido",
  "password": "string (min 8)"
}
```

### `LoginRequest`
```json
{ "email": "...", "password": "..." }
```

### `AuthResponse`
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": {
    "id": "uuid",
    "name": "...",
    "email": "...",
    "role": "ADMIN | SELLER",
    "tenantId": "uuid"
  }
}
```

### `RefreshTokenRequest`
```json
{ "refreshToken": "eyJ..." }
```

### `ForgotPasswordRequest`
```json
{ "email": "..." }
```

### `ResetPasswordRequest`
```json
{ "token": "uuid generado en forgot-password", "password": "string (min 8)" }
```

**Errores posibles**: `409` email ya registrado (register), `400` credenciales
incorrectas (login), `403` cuenta desactivada (`is_active = false`), `400`
token de reset inválido o expirado.

---

## 2. Leads — `/api/leads`

| Método | Ruta | Query / Body | Descripción |
|---|---|---|---|
| GET | `/leads` | `status, priority, q, page, size` (todos opcionales) | Lista paginada |
| GET | `/leads/{id}` | — | Detalle |
| POST | `/leads` | `LeadRequest` | Crea (status inicial = `NEW`). 201 |
| PUT | `/leads/{id}` | `LeadRequest` | Actualiza todos los campos excepto `status` |
| PATCH | `/leads/{id}/status` | `{ "status": "..." }` | Cambia estado |
| POST | `/leads/{id}/convert` | — | Crea un `Customer` a partir del Lead, marca Lead como `CONVERTED`. 201 |
| DELETE | `/leads/{id}` | — | Soft delete |

### `LeadRequest`
```json
{
  "name": "string (2-150, requerido)",
  "company": "string (opcional)",
  "email": "email (opcional)",
  "phone": "string (opcional)",
  "source": "WHATSAPP | EMAIL | REFERRAL | WEBSITE | PHONE | OTHER",
  "priority": "LOW | MEDIUM | HIGH",
  "notes": "string (opcional)",
  "assignedToId": "uuid (opcional)"
}
```

### `LeadResponse`
```json
{
  "id": "uuid",
  "name": "...",
  "company": "...",
  "email": "...",
  "phone": "...",
  "source": "WHATSAPP",
  "status": "NEW",
  "priority": "HIGH",
  "notes": "...",
  "assignedTo": { "id": "uuid", "name": "...", "email": "..." } | null,
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### `PATCH /leads/{id}/status` — body
```json
{ "status": "NEW | CONTACTED | QUALIFIED | CONVERTED | DISCARDED" }
```

### `POST /leads/{id}/convert` — response `ConvertLeadResponse`
```json
{ "customerId": "uuid", "message": "Lead convertido a cliente exitosamente" }
```

**Errores**: `404` lead no existe / no pertenece al tenant, `400` cambio de
estado sobre un lead ya `CONVERTED`, `409` intento de convertir un lead ya
convertido.

---

## 3. Customers — `/api/customers`

| Método | Ruta | Query / Body | Descripción |
|---|---|---|---|
| GET | `/customers` | `q, page, size` | Lista paginada |
| GET | `/customers/{id}` | — | Detalle |
| POST | `/customers` | `CustomerRequest` | Crea cliente directo (sin lead). 201 |
| PUT | `/customers/{id}` | `CustomerRequest` | Actualiza |
| DELETE | `/customers/{id}` | — | Soft delete |

### `CustomerRequest`
```json
{
  "name": "string (2-150, requerido)",
  "company": "string (opcional)",
  "email": "email (opcional, único por tenant)",
  "phone": "string (opcional)",
  "rfc": "string max 13 (opcional)",
  "address": "string (opcional)",
  "notes": "string (opcional)",
  "assignedToId": "uuid (opcional)"
}
```

### `CustomerResponse`
```json
{
  "id": "uuid",
  "name": "...",
  "company": "...",
  "email": "...",
  "phone": "...",
  "rfc": "...",
  "address": "...",
  "notes": "...",
  "leadId": "uuid | null",
  "assignedTo": { "id": "uuid", "name": "...", "email": "..." } | null,
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

**Errores**: `409` si `email` ya existe en otro cliente del mismo tenant
(en create y en update si se cambia el email).

---

## 4. FollowUps — `/api/follow-ups`

Seguimientos polimórficos — aplican a un `Lead` o a un `Customer`.

| Método | Ruta | Query / Body | Descripción |
|---|---|---|---|
| GET | `/follow-ups` | `entityType=LEAD\|CUSTOMER`, `entityId=uuid` (requeridos) | Historial ordenado por `interactionDate DESC` |
| POST | `/follow-ups` | `FollowUpRequest` | Crea. El `user` se toma del JWT. 201 |
| DELETE | `/follow-ups/{id}` | — | Hard delete |

### `FollowUpRequest`
```json
{
  "entityType": "LEAD | CUSTOMER",
  "entityId": "uuid",
  "type": "CALL | EMAIL | MEETING | WHATSAPP | OTHER",
  "interactionDate": "ISO-8601 datetime",
  "notes": "string (requerido)",
  "result": "string (opcional)"
}
```

### `FollowUpResponse`
```json
{
  "id": "uuid",
  "entityType": "LEAD",
  "entityId": "uuid",
  "type": "CALL",
  "interactionDate": "ISO-8601",
  "notes": "...",
  "result": "...",
  "user": { "id": "uuid", "name": "..." } | null,
  "createdAt": "ISO-8601"
}
```

**Errores**: `404` si `entityId` no existe (o no pertenece al tenant) según
el `entityType` indicado.

---

## 5. Reminders — `/api/reminders`

| Método | Ruta | Query / Body | Descripción |
|---|---|---|---|
| GET | `/reminders` | `entityType, entityId` (requeridos) | Recordatorios de una entidad, ordenados por `remindAt ASC` |
| GET | `/reminders/today` | — | Recordatorios pendientes (`isDone=false`) **de hoy** para el usuario en sesión. Usado por el Dashboard |
| POST | `/reminders` | `ReminderRequest` | Crea. 201 |
| PATCH | `/reminders/{id}/complete` | — | `isDone = true` |
| DELETE | `/reminders/{id}` | — | Hard delete |

### `ReminderRequest`
```json
{
  "entityType": "LEAD | CUSTOMER",
  "entityId": "uuid",
  "remindAt": "ISO-8601 datetime",
  "description": "string (requerido)"
}
```

### `ReminderResponse`
```json
{
  "id": "uuid",
  "entityType": "LEAD",
  "entityId": "uuid",
  "remindAt": "ISO-8601",
  "description": "...",
  "isDone": false,
  "user": { "id": "uuid", "name": "..." } | null,
  "createdAt": "ISO-8601"
}
```

> Nota sobre `/reminders/today`: el rango "hoy" se calcula con el offset del
> servidor (`OffsetDateTime.now()` → inicio/fin del día local del servidor).
> Si el servidor de producción no está en zona horaria de México, revisar
> esto.

---

## 6. Quotes — `/api/quotes`

| Método | Ruta | Query / Body | Descripción |
|---|---|---|---|
| GET | `/quotes` | `status, customerId, q, page, size` (opcionales) | Lista paginada |
| GET | `/quotes/{id}` | — | Detalle completo (incluye `lines[]`) |
| GET | `/quotes/{id}/history` | — | Historial de cambios de estado |
| POST | `/quotes` | `QuoteRequest` | Crea en `DRAFT`. Calcula `subtotal`/`total`. 201 |
| PUT | `/quotes/{id}` | `QuoteRequest` | Solo si `status = DRAFT`. Reemplaza todas las líneas |
| PATCH | `/quotes/{id}/status` | `{ "status": "..." }` | Cambia estado (ver máquina de estados) |
| DELETE | `/quotes/{id}` | — | Solo si `status = DRAFT`. Soft delete |

### `QuoteRequest`
```json
{
  "customerId": "uuid (requerido)",
  "title": "string max 300 (requerido)",
  "discountPct": "0-100 (default 0)",
  "taxPct": ">= 0 (default 16.00)",
  "issuedAt": "fecha (opcional, default hoy)",
  "expiresAt": "fecha futura (requerido)",
  "notes": "string (opcional)",
  "lines": [
    {
      "description": "string (requerido)",
      "quantity": "> 0 (requerido)",
      "unitPrice": ">= 0 (requerido)"
    }
  ]
}
```
`lines` debe tener al menos un elemento.

### `QuoteResponse`
```json
{
  "id": "uuid",
  "customer": { "id": "uuid", "name": "...", "company": "..." },
  "title": "...",
  "status": "DRAFT",
  "lines": [
    { "id": "uuid", "description": "...", "quantity": 3, "unitPrice": 850, "subtotal": 2550, "sortOrder": 0 }
  ],
  "discountPct": 5.00,
  "taxPct": 16.00,
  "subtotal": 4150.00,
  "total": 4573.30,
  "notes": "...",
  "issuedAt": "2026-06-11",
  "expiresAt": "2026-07-15",
  "createdBy": { "id": "uuid", "name": "..." } | null,
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### `PATCH /quotes/{id}/status` — body
```json
{ "status": "DRAFT | SENT | WON | LOST | EXPIRED" }
```

Transiciones válidas (ver `03-REGLAS-DE-NEGOCIO.md`):
```
DRAFT → SENT
SENT  → WON | LOST | EXPIRED
WON, LOST, EXPIRED → (terminales, sin transición posible)
```

### `GET /quotes/{id}/history` — response
```json
[
  {
    "oldStatus": "DRAFT",
    "newStatus": "SENT",
    "changedByName": "Ana Martinez",
    "changedAt": "ISO-8601"
  }
]
```

**Errores**: `404` cliente o cotización no encontrados, `400` editar/eliminar
fuera de `DRAFT`, `400` transición de estado inválida o estado igual al
actual.

---

## 7. Dashboard — `/api/dashboard`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/dashboard/stats` | Métricas agregadas del tenant + del usuario en sesión |

### `DashboardStatsResponse`
```json
{
  "totalLeads": 5,
  "totalCustomers": 3,
  "openQuotesCount": 2,
  "openQuotesValue": 92474.30,
  "todayReminders": 3,
  "quotesByStatus": {
    "DRAFT":   { "count": 2, "total": 92474.30 },
    "SENT":    { "count": 0, "total": 0 },
    "WON":     { "count": 1, "total": 88066.80 },
    "LOST":    { "count": 1, "total": 110200.00 },
    "EXPIRED": { "count": 0, "total": 0 }
  }
}
```

- `totalLeads` / `totalCustomers`: conteo de registros con `deletedAt IS NULL`
  del tenant.
- `openQuotesCount` / `openQuotesValue`: suma de `count`/`total` de los
  estados `DRAFT` + `SENT` (el "pipeline abierto").
- `todayReminders`: recordatorios `isDone=false` del **usuario autenticado**
  (no de todo el tenant) cuyo `remindAt` cae dentro del día de hoy.
- `quotesByStatus`: **siempre** incluye las 5 keys (`DRAFT`, `SENT`, `WON`,
  `LOST`, `EXPIRED`), aunque el conteo sea 0 — el frontend depende de esto.

---

## 8. Tabla resumen — todos los endpoints

| Módulo | Método | Ruta |
|---|---|---|
| Auth | POST | `/auth/register` |
| Auth | POST | `/auth/login` |
| Auth | POST | `/auth/refresh` |
| Auth | POST | `/auth/forgot-password` |
| Auth | POST | `/auth/reset-password` |
| Leads | GET | `/leads` |
| Leads | GET | `/leads/{id}` |
| Leads | POST | `/leads` |
| Leads | PUT | `/leads/{id}` |
| Leads | PATCH | `/leads/{id}/status` |
| Leads | POST | `/leads/{id}/convert` |
| Leads | DELETE | `/leads/{id}` |
| Customers | GET | `/customers` |
| Customers | GET | `/customers/{id}` |
| Customers | POST | `/customers` |
| Customers | PUT | `/customers/{id}` |
| Customers | DELETE | `/customers/{id}` |
| FollowUps | GET | `/follow-ups` |
| FollowUps | POST | `/follow-ups` |
| FollowUps | DELETE | `/follow-ups/{id}` |
| Reminders | GET | `/reminders` |
| Reminders | GET | `/reminders/today` |
| Reminders | POST | `/reminders` |
| Reminders | PATCH | `/reminders/{id}/complete` |
| Reminders | DELETE | `/reminders/{id}` |
| Quotes | GET | `/quotes` |
| Quotes | GET | `/quotes/{id}` |
| Quotes | GET | `/quotes/{id}/history` |
| Quotes | POST | `/quotes` |
| Quotes | PUT | `/quotes/{id}` |
| Quotes | PATCH | `/quotes/{id}/status` |
| Quotes | DELETE | `/quotes/{id}` |
| Dashboard | GET | `/dashboard/stats` |

**Total: 32 endpoints** repartidos en 7 controllers.
