-- =============================================================================
--  Solventa CRM — V2 Seed Data (sin UUIDs hardcodeados)
-- =============================================================================

-- Variables temporales para referenciar IDs entre inserts
DO $$
DECLARE
v_tenant_id     UUID := 'a1b2c3d4-0000-0000-0000-000000000001';
    v_admin_id      UUID := 'b1b2c3d4-0000-0000-0000-000000000001';
    v_seller1_id    UUID := 'b1b2c3d4-0000-0000-0000-000000000002';
    v_seller2_id    UUID := 'b1b2c3d4-0000-0000-0000-000000000003';
    v_lead1_id      UUID := 'c1000000-0000-0000-0000-000000000001';
    v_lead2_id      UUID := 'c1000000-0000-0000-0000-000000000002';
    v_lead3_id      UUID := 'c1000000-0000-0000-0000-000000000003';
    v_lead4_id      UUID := 'c1000000-0000-0000-0000-000000000004';
    v_lead5_id      UUID := 'c1000000-0000-0000-0000-000000000005';
    v_cust1_id      UUID := 'd1000000-0000-0000-0000-000000000001';
    v_cust2_id      UUID := 'd1000000-0000-0000-0000-000000000002';
    v_cust3_id      UUID := 'd1000000-0000-0000-0000-000000000003';
    v_quote1_id     UUID := 'e1000000-0000-0000-0000-000000000001';
    v_quote2_id     UUID := 'e1000000-0000-0000-0000-000000000002';
    v_quote3_id     UUID := 'e1000000-0000-0000-0000-000000000003';
    v_quote4_id     UUID := 'e1000000-0000-0000-0000-000000000004';
    v_quote5_id     UUID := 'e1000000-0000-0000-0000-000000000005';
BEGIN

-- =============================================================================
--  1. EMPRESA
-- =============================================================================
INSERT INTO companies (id, name, industry, rfc, plan, status)
VALUES (
           v_tenant_id,
           'Ferreteria Lopez SA de CV',
           'Comercio',
           'FLO920101AB1',
           'BASIC',
           'ACTIVE'
       );

-- =============================================================================
--  2. USUARIOS  (password = Admin123)
-- =============================================================================
INSERT INTO users (id, tenant_id, name, email, password_hash, role, is_active, invitation_accepted)
VALUES
    (
        v_admin_id, v_tenant_id,
        'Roberto Lopez',
        'roberto@ferreterialopez.mx',
        '$2a$12$OE41w9finVVKOjDpom/4QecHCbTxpUAcYXW1bq.YDaP55W/PtqQ7.',
        'ADMIN', true, true
    ),
    (
        v_seller1_id, v_tenant_id,
        'Ana Martinez',
        'ana@ferreterialopez.mx',
        '$2a$12$OE41w9finVVKOjDpom/4QecHCbTxpUAcYXW1bq.YDaP55W/PtqQ7.',
        'SELLER', true, true
    ),
    (
        v_seller2_id, v_tenant_id,
        'Carlos Herrera',
        'carlos@ferreterialopez.mx',
        '$2a$12$OE41w9finVVKOjDpom/4QecHCbTxpUAcYXW1bq.YDaP55W/PtqQ7.',
        'SELLER', true, true
    );

-- =============================================================================
--  3. LEADS
-- =============================================================================
INSERT INTO leads (id, tenant_id, assigned_to, name, company, email, phone, source, status, priority, notes)
VALUES
    (
        v_lead1_id, v_tenant_id, v_seller1_id,
        'Mario Sanchez', 'Constructora Sanchez',
        'mario@constructora.mx', '5512345678',
        'WHATSAPP', 'NEW', 'HIGH',
        'Interesado en compra de material para obra en Monterrey. Presupuesto aprox 200K.'
    ),
    (
        v_lead2_id, v_tenant_id, v_seller1_id,
        'Patricia Vega', 'Remodelaciones Vega',
        'pvega@remodelaciones.mx', '5598765432',
        'REFERRAL', 'CONTACTED', 'MEDIUM',
        'Referida por cliente existente. Necesita herramienta electrica para taller.'
    ),
    (
        v_lead3_id, v_tenant_id, v_seller2_id,
        'Jorge Fuentes', 'Taller Fuentes',
        'jfuentes@taller.mx', '5511223344',
        'PHONE', 'QUALIFIED', 'HIGH',
        'Taller mecanico con 5 empleados. Busca proveedor regular de refacciones.'
    ),
    (
        v_lead4_id, v_tenant_id, v_seller2_id,
        'Sofia Ramirez', null,
        'sofia.ramirez@gmail.com', '5544332211',
        'WEBSITE', 'NEW', 'LOW',
        'Persona fisica. Busca materiales para remodelacion de casa.'
    ),
    (
        v_lead5_id, v_tenant_id, v_seller1_id,
        'Alejandro Torres', 'Desarrolladora Torres',
        'atorres@desarrolladora.mx', '5566778899',
        'EMAIL', 'CONVERTED', 'HIGH',
        'Lead convertido. Proyecto de 8 departamentos en Guadalajara.'
    );

-- =============================================================================
--  4. CLIENTES
-- =============================================================================
INSERT INTO customers (id, tenant_id, lead_id, assigned_to, name, company, email, phone, notes)
VALUES
    (
        v_cust1_id, v_tenant_id, v_lead5_id, v_seller1_id,
        'Alejandro Torres', 'Desarrolladora Torres',
        'atorres@desarrolladora.mx', '5566778899',
        'Cliente frecuente. Proyecto actual: 8 departamentos Guadalajara. Pago puntual.'
    ),
    (
        v_cust2_id, v_tenant_id, null, v_seller2_id,
        'Inmobiliaria del Norte SA', 'Inmobiliaria del Norte SA',
        'compras@inmonorte.mx', '8112345678',
        'Cliente corporativo. Compras mensuales promedio 80K. Requiere factura.'
    ),
    (
        v_cust3_id, v_tenant_id, null, v_seller1_id,
        'Acabados Premium SA', 'Acabados Premium SA',
        'ventas@acabadospremium.mx', '5533445566',
        'Distribuidor de acabados. Compra por volumen. Descuento especial del 8%.'
    );

-- =============================================================================
--  5. COTIZACIONES
-- =============================================================================
INSERT INTO quotes (id, tenant_id, customer_id, created_by, title, status, discount_pct, tax_pct, subtotal, total, issued_at, expires_at, notes)
VALUES
    (
        v_quote1_id, v_tenant_id, v_cust1_id, v_seller1_id,
        'Material construccion - Proyecto 8 Deptos Guadalajara',
        'SENT', 5.00, 16.00, 185000.00, 203548.00,
        CURRENT_DATE - INTERVAL '5 days',
        CURRENT_DATE + INTERVAL '25 days',
        'Incluye flete a Guadalajara. Entrega en 2 partes.'
    ),
    (
        v_quote2_id, v_tenant_id, v_cust2_id, v_seller2_id,
        'Pedido mensual noviembre - Inmobiliaria del Norte',
        'WON', 3.00, 16.00, 78000.00, 88066.80,
        CURRENT_DATE - INTERVAL '15 days',
        CURRENT_DATE - INTERVAL '5 days',
        'Pedido recurrente. Factura requerida en 3 dias habiles.'
    ),
    (
        v_quote3_id, v_tenant_id, v_cust3_id, v_seller1_id,
        'Herramienta electrica y consumibles Q4',
        'DRAFT', 0.00, 16.00, 42500.00, 49300.00,
        CURRENT_DATE,
        CURRENT_DATE + INTERVAL '30 days',
        null
    ),
    (
        v_quote4_id, v_tenant_id, v_cust1_id, v_seller1_id,
        'Acabados y pisos - Torre A',
        'LOST', 0.00, 16.00, 95000.00, 110200.00,
        CURRENT_DATE - INTERVAL '30 days',
        CURRENT_DATE - INTERVAL '10 days',
        'Perdida por precio. Cliente fue con proveedor de Guadalajara.'
    ),
    (
        v_quote5_id, v_tenant_id, v_cust2_id, v_seller2_id,
        'Pedido diciembre - Inmobiliaria del Norte',
        'DRAFT', 3.00, 16.00, 82000.00, 92574.40,
        CURRENT_DATE,
        CURRENT_DATE + INTERVAL '20 days',
        null
    );

-- =============================================================================
--  6. LINEAS DE COTIZACION
-- =============================================================================
INSERT INTO quote_lines (quote_id, description, quantity, unit_price, subtotal, sort_order)
VALUES
    (v_quote1_id, 'Varilla corrugada 3/8 pulgada (ton)',       8.00,  14500.00, 116000.00, 0),
    (v_quote1_id, 'Cemento Cruz Azul 50kg (bulto)',           200.00,    195.00,  39000.00, 1),
    (v_quote1_id, 'Block 15x20x40 cm (millar)',                10.00,   3000.00,  30000.00, 2),
    (v_quote2_id, 'Pintura vinilica interior cubeta 19L',      40.00,    850.00,  34000.00, 0),
    (v_quote2_id, 'Mosaico 45x45 modelo Sahara (m2)',         350.00,    120.00,  42000.00, 1),
    (v_quote2_id, 'Flete y maniobras',                          1.00,   2000.00,   2000.00, 2),
    (v_quote3_id, 'Taladro percutor DeWalt 1/2 pulgada',        5.00,   3200.00,  16000.00, 0),
    (v_quote3_id, 'Amoladora angular 4.5 pulgada Bosch',        5.00,   2100.00,  10500.00, 1),
    (v_quote3_id, 'Disco de corte 4.5 pulgada (caja 25 pzas)', 20.00,    800.00,  16000.00, 2);

-- =============================================================================
--  7. HISTORIAL DE ESTADOS
-- =============================================================================
INSERT INTO quote_status_history (quote_id, old_status, new_status, changed_by, changed_at)
VALUES
    (v_quote1_id, 'DRAFT', 'SENT', v_seller1_id, NOW() - INTERVAL '5 days'),
    (v_quote2_id, 'DRAFT', 'SENT', v_seller2_id, NOW() - INTERVAL '15 days'),
    (v_quote2_id, 'SENT',  'WON',  v_seller2_id, NOW() - INTERVAL '8 days'),
    (v_quote4_id, 'DRAFT', 'SENT', v_seller1_id, NOW() - INTERVAL '30 days'),
    (v_quote4_id, 'SENT',  'LOST', v_seller1_id, NOW() - INTERVAL '10 days');

-- =============================================================================
--  8. SEGUIMIENTOS
-- =============================================================================
INSERT INTO follow_ups (tenant_id, user_id, entity_type, entity_id, type, interaction_date, notes, result)
VALUES
    (v_tenant_id, v_seller1_id, 'LEAD', v_lead1_id, 'WHATSAPP',
     NOW() - INTERVAL '3 days',
     'Primer contacto por WhatsApp. Se presento la empresa y se ofrecio visita.',
     'Interesado. Pidio cotizacion por correo.'),

    (v_tenant_id, v_seller1_id, 'LEAD', v_lead2_id, 'CALL',
     NOW() - INTERVAL '5 days',
     'Llamada de 15 min para conocer necesidades. Taller con 3 empleados.',
     'Agendo visita para el viernes.'),

    (v_tenant_id, v_seller2_id, 'LEAD', v_lead3_id, 'MEETING',
     NOW() - INTERVAL '2 days',
     'Reunion en sucursal. Se mostro catalogo completo.',
     'Solicito cotizacion formal. Toma decision esta semana.'),

    (v_tenant_id, v_seller1_id, 'CUSTOMER', v_cust1_id, 'EMAIL',
     NOW() - INTERVAL '5 days',
     'Se envio cotizacion por correo con desglose detallado.',
     'Confirmo recepcion. Revisara con su socio.'),

    (v_tenant_id, v_seller1_id, 'CUSTOMER', v_cust1_id, 'WHATSAPP',
     NOW() - INTERVAL '2 days',
     'Recordatorio por WhatsApp de cotizacion enviada.',
     'Dijo que responde manana.'),

    (v_tenant_id, v_seller2_id, 'CUSTOMER', v_cust2_id, 'CALL',
     NOW() - INTERVAL '1 day',
     'Llamada para confirmar pedido de diciembre.',
     'Confirmado. Pide cotizacion antes del viernes.');

-- =============================================================================
--  9. RECORDATORIOS
-- =============================================================================
INSERT INTO reminders (tenant_id, user_id, entity_type, entity_id, remind_at, description, is_done)
VALUES
    (v_tenant_id, v_seller1_id, 'CUSTOMER', v_cust1_id,
     NOW() + INTERVAL '1 hour',
     'Llamar a Alejandro Torres para confirmar cotizacion enviada', false),

    (v_tenant_id, v_seller2_id, 'CUSTOMER', v_cust2_id,
     NOW() + INTERVAL '2 hours',
     'Enviar cotizacion de diciembre a Inmobiliaria del Norte', false),

    (v_tenant_id, v_seller1_id, 'LEAD', v_lead1_id,
     NOW() + INTERVAL '3 hours',
     'Enviar cotizacion a Mario Sanchez - Constructora', false),

    (v_tenant_id, v_seller2_id, 'LEAD', v_lead3_id,
     NOW() + INTERVAL '1 day',
     'Seguimiento a Jorge Fuentes - Taller. Esperando decision.', false),

    (v_tenant_id, v_seller1_id, 'CUSTOMER', v_cust3_id,
     NOW() + INTERVAL '2 days',
     'Revisar borrador cotizacion herramientas con Roberto antes de enviar', false);

END $$;