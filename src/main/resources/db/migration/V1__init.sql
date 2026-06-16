-- =============================================================================
--  Solventa CRM — V1 Initial Schema
--  Todas las tablas del MVP en orden de dependencias
-- =============================================================================

-- ── Extensión para UUID ───────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
--  1. COMPANIES (Tenants)
-- =============================================================================
CREATE TABLE companies (
                           id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                           name        VARCHAR(200) NOT NULL,
                           industry    VARCHAR(100),
                           rfc         VARCHAR(13),
                           logo_url    TEXT,
                           plan        VARCHAR(20)  NOT NULL DEFAULT 'FREE'
                               CHECK (plan IN ('FREE', 'BASIC', 'PROFESSIONAL')),
                           status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                               CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
                           created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE companies IS 'Tenant raíz. Cada empresa suscrita es un tenant independiente.';

-- =============================================================================
--  2. USERS
-- =============================================================================
CREATE TABLE users (
                       id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                       tenant_id               UUID        NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                       name                    VARCHAR(150) NOT NULL,
                       email                   VARCHAR(200) NOT NULL,
                       password_hash           VARCHAR(255) NOT NULL,
                       role                    VARCHAR(20)  NOT NULL DEFAULT 'SELLER'
                           CHECK (role IN ('ADMIN', 'SELLER')),
                       is_active               BOOLEAN     NOT NULL DEFAULT TRUE,

    -- Invitación
                       invitation_token        VARCHAR(255),
                       invitation_expires_at   TIMESTAMPTZ,
                       invitation_accepted     BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Recuperación de contraseña
                       reset_token             VARCHAR(255),
                       reset_token_expires_at  TIMESTAMPTZ,

                       created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Email único por tenant
                       CONSTRAINT uq_users_email_tenant UNIQUE (tenant_id, email)
);

COMMENT ON TABLE users IS 'Usuarios del sistema. Pertenecen a exactamente un tenant.';

-- Índices usuarios
CREATE INDEX idx_users_tenant_id   ON users(tenant_id);
CREATE INDEX idx_users_email       ON users(email);
CREATE INDEX idx_users_inv_token   ON users(invitation_token) WHERE invitation_token IS NOT NULL;
CREATE INDEX idx_users_reset_token ON users(reset_token)      WHERE reset_token IS NOT NULL;

-- =============================================================================
--  3. LEADS
-- =============================================================================
CREATE TABLE leads (
                       id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                       tenant_id   UUID        NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                       assigned_to UUID        REFERENCES users(id) ON DELETE SET NULL,

                       name        VARCHAR(150) NOT NULL,
                       company     VARCHAR(200),
                       email       VARCHAR(200),
                       phone       VARCHAR(30),

                       source      VARCHAR(20)  NOT NULL DEFAULT 'OTHER'
                           CHECK (source IN ('WHATSAPP','EMAIL','REFERRAL','WEBSITE','PHONE','OTHER')),
                       status      VARCHAR(20)  NOT NULL DEFAULT 'NEW'
                           CHECK (status IN ('NEW','CONTACTED','QUALIFIED','CONVERTED','DISCARDED')),
                       priority    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM'
                           CHECK (priority IN ('LOW','MEDIUM','HIGH')),

                       notes       TEXT,
                       deleted_at  TIMESTAMPTZ,          -- soft delete
                       created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE leads IS 'Prospectos en proceso comercial.';

-- Índices leads
CREATE INDEX idx_leads_tenant_id   ON leads(tenant_id);
CREATE INDEX idx_leads_assigned_to ON leads(assigned_to);
CREATE INDEX idx_leads_status      ON leads(tenant_id, status);
CREATE INDEX idx_leads_priority    ON leads(tenant_id, priority);
CREATE INDEX idx_leads_deleted_at  ON leads(deleted_at) WHERE deleted_at IS NULL;

-- =============================================================================
--  4. CUSTOMERS
-- =============================================================================
CREATE TABLE customers (
                           id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                           tenant_id   UUID        NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                           assigned_to UUID        REFERENCES users(id) ON DELETE SET NULL,
                           lead_id     UUID        REFERENCES leads(id) ON DELETE SET NULL,

                           name        VARCHAR(150) NOT NULL,
                           company     VARCHAR(200),
                           email       VARCHAR(200),
                           phone       VARCHAR(30),
                           address     TEXT,
                           rfc         VARCHAR(13),

                           notes       TEXT,
                           deleted_at  TIMESTAMPTZ,
                           created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Email único por tenant (si se proporciona)
                           CONSTRAINT uq_customers_email_tenant UNIQUE (tenant_id, email)
);

COMMENT ON TABLE customers IS 'Leads convertidos en clientes activos.';

-- Índices customers
CREATE INDEX idx_customers_tenant_id   ON customers(tenant_id);
CREATE INDEX idx_customers_assigned_to ON customers(assigned_to);
CREATE INDEX idx_customers_lead_id     ON customers(lead_id);
CREATE INDEX idx_customers_deleted_at  ON customers(deleted_at) WHERE deleted_at IS NULL;

-- =============================================================================
--  5. QUOTES (Cotizaciones)
-- =============================================================================
CREATE TABLE quotes (
                        id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                        tenant_id    UUID         NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                        customer_id  UUID         NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
                        created_by   UUID         REFERENCES users(id) ON DELETE SET NULL,

                        title        VARCHAR(300) NOT NULL,
                        status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT','SENT','WON','LOST','EXPIRED')),

                        discount_pct NUMERIC(5,2) NOT NULL DEFAULT 0.00
                            CHECK (discount_pct >= 0 AND discount_pct <= 100),
                        tax_pct      NUMERIC(5,2) NOT NULL DEFAULT 16.00
                            CHECK (tax_pct >= 0),
                        subtotal     NUMERIC(14,2) NOT NULL DEFAULT 0.00,
                        total        NUMERIC(14,2) NOT NULL DEFAULT 0.00,

                        notes        TEXT,
                        issued_at    DATE         NOT NULL DEFAULT CURRENT_DATE,
                        expires_at   DATE         NOT NULL,

                        deleted_at   TIMESTAMPTZ,
                        created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                        updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE quotes IS 'Cotizaciones asociadas a clientes.';

-- Índices quotes
CREATE INDEX idx_quotes_tenant_id   ON quotes(tenant_id);
CREATE INDEX idx_quotes_customer_id ON quotes(customer_id);
CREATE INDEX idx_quotes_status      ON quotes(tenant_id, status);
CREATE INDEX idx_quotes_expires_at  ON quotes(expires_at) WHERE status = 'SENT';
CREATE INDEX idx_quotes_deleted_at  ON quotes(deleted_at) WHERE deleted_at IS NULL;

-- =============================================================================
--  6. QUOTE LINES (Líneas de cotización)
-- =============================================================================
CREATE TABLE quote_lines (
                             id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                             quote_id     UUID          NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
                             description  VARCHAR(500)  NOT NULL,
                             quantity     NUMERIC(10,2) NOT NULL DEFAULT 1
                                 CHECK (quantity > 0),
                             unit_price   NUMERIC(14,2) NOT NULL DEFAULT 0.00
                                 CHECK (unit_price >= 0),
                             subtotal     NUMERIC(14,2) NOT NULL DEFAULT 0.00,
                             sort_order   INTEGER       NOT NULL DEFAULT 0,
                             created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE quote_lines IS 'Líneas de detalle de una cotización.';

CREATE INDEX idx_quote_lines_quote_id ON quote_lines(quote_id);

-- =============================================================================
--  7. QUOTE STATUS HISTORY
-- =============================================================================
CREATE TABLE quote_status_history (
                                      id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                      quote_id    UUID        NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
                                      old_status  VARCHAR(20),
                                      new_status  VARCHAR(20)  NOT NULL,
                                      changed_by  UUID        REFERENCES users(id) ON DELETE SET NULL,
                                      changed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE quote_status_history IS 'Historial inmutable de cambios de estado de cotizaciones.';

CREATE INDEX idx_qsh_quote_id ON quote_status_history(quote_id);

-- =============================================================================
--  8. FOLLOW UPS (Seguimientos)
-- =============================================================================
CREATE TABLE follow_ups (
                            id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                            tenant_id        UUID        NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                            user_id          UUID        REFERENCES users(id) ON DELETE SET NULL,

    -- Polimórfico: aplica a Lead o Customer
                            entity_type      VARCHAR(20) NOT NULL
                                CHECK (entity_type IN ('LEAD','CUSTOMER')),
                            entity_id        UUID        NOT NULL,

                            type             VARCHAR(20) NOT NULL
                                CHECK (type IN ('CALL','EMAIL','MEETING','WHATSAPP','OTHER')),
                            interaction_date TIMESTAMPTZ NOT NULL,
                            notes            TEXT        NOT NULL,
                            result           TEXT,

                            created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE follow_ups IS 'Registro de interacciones con leads y clientes.';

-- Índices follow_ups
CREATE INDEX idx_follow_ups_tenant_id   ON follow_ups(tenant_id);
CREATE INDEX idx_follow_ups_entity      ON follow_ups(entity_type, entity_id);
CREATE INDEX idx_follow_ups_user_id     ON follow_ups(user_id);
CREATE INDEX idx_follow_ups_date        ON follow_ups(interaction_date DESC);

-- =============================================================================
--  9. REMINDERS (Recordatorios)
-- =============================================================================
CREATE TABLE reminders (
                           id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                           tenant_id   UUID        NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                           user_id     UUID        REFERENCES users(id) ON DELETE CASCADE,

                           entity_type VARCHAR(20) NOT NULL
                               CHECK (entity_type IN ('LEAD','CUSTOMER')),
                           entity_id   UUID        NOT NULL,

                           remind_at   TIMESTAMPTZ NOT NULL,
                           description TEXT        NOT NULL,
                           is_done     BOOLEAN     NOT NULL DEFAULT FALSE,

                           created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE reminders IS 'Recordatorios de seguimiento asociados a leads o clientes.';

-- Índices reminders
CREATE INDEX idx_reminders_tenant_id  ON reminders(tenant_id);
CREATE INDEX idx_reminders_user_today ON reminders(tenant_id, user_id, remind_at)
    WHERE is_done = FALSE;
CREATE INDEX idx_reminders_entity     ON reminders(entity_type, entity_id);

-- =============================================================================
--  10. AUDIT LOG
-- =============================================================================
CREATE TABLE audit_logs (
                            id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                            tenant_id   UUID        REFERENCES companies(id) ON DELETE SET NULL,
                            user_id     UUID        REFERENCES users(id)    ON DELETE SET NULL,
                            action      VARCHAR(20) NOT NULL
                                CHECK (action IN ('CREATE','UPDATE','DELETE','LOGIN','LOGOUT')),
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID,
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE audit_logs IS 'Log de auditoría inmutable. Solo INSERT, nunca UPDATE ni DELETE.';

-- Índices audit_logs
CREATE INDEX idx_audit_logs_tenant_id   ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_user_id     ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity      ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at  ON audit_logs(created_at DESC);

-- =============================================================================
--  11. TRIGGERS — updated_at automático
-- =============================================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar trigger a todas las tablas con updated_at
CREATE TRIGGER trg_companies_updated_at
    BEFORE UPDATE ON companies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_leads_updated_at
    BEFORE UPDATE ON leads
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_quotes_updated_at
    BEFORE UPDATE ON quotes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_reminders_updated_at
    BEFORE UPDATE ON reminders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();