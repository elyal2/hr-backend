-- V1.6.0: Agregar tabla de tasas de cambio de monedas
-- Tabla para configurar tasas de conversión con EUR como moneda base

-- Tabla de tasas de cambio
CREATE TABLE IF NOT EXISTS hr_app.currency_exchange_rates (
    id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    exchange_rate DECIMAL(15,6) NOT NULL,
    effective_date DATE NOT NULL,
    expiry_date DATE,
    source VARCHAR(100) DEFAULT 'manual',
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated TIMESTAMP,
    PRIMARY KEY (id, tenant_id)
);

-- Índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_currency_rates_from_to ON hr_app.currency_exchange_rates(tenant_id, from_currency, to_currency);
CREATE INDEX IF NOT EXISTS idx_currency_rates_effective_date ON hr_app.currency_exchange_rates(tenant_id, effective_date);
CREATE INDEX IF NOT EXISTS idx_currency_rates_status ON hr_app.currency_exchange_rates(tenant_id, status);

-- Restricción única para evitar duplicados de tasas activas
CREATE UNIQUE INDEX IF NOT EXISTS idx_currency_rates_unique_active 
ON hr_app.currency_exchange_rates(tenant_id, from_currency, to_currency, effective_date) 
WHERE status = 'active';

-- Tabla de auditoría
CREATE TABLE IF NOT EXISTS hr_app.currency_exchange_rates_aud (
    id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    exchange_rate DECIMAL(15,6) NOT NULL,
    effective_date DATE NOT NULL,
    expiry_date DATE,
    source VARCHAR(100),
    notes TEXT,
    status VARCHAR(50) NOT NULL,
    date_created TIMESTAMP NOT NULL,
    date_updated TIMESTAMP,
    rev BIGINT NOT NULL,
    revtype SMALLINT,
    PRIMARY KEY (id, tenant_id, rev),
    FOREIGN KEY (rev) REFERENCES hr_app.revinfo
);

-- Habilitar RLS
ALTER TABLE hr_app.currency_exchange_rates ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.currency_exchange_rates_aud ENABLE ROW LEVEL SECURITY;

-- Políticas RLS
DROP POLICY IF EXISTS currency_rates_isolation ON hr_app.currency_exchange_rates;
CREATE POLICY currency_rates_isolation ON hr_app.currency_exchange_rates
    FOR ALL USING (tenant_id = hr_app.current_tenant()) 
    WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS currency_rates_aud_isolation ON hr_app.currency_exchange_rates_aud;
CREATE POLICY currency_rates_aud_isolation ON hr_app.currency_exchange_rates_aud
    FOR ALL USING (tenant_id = hr_app.current_tenant()) 
    WITH CHECK (tenant_id = hr_app.current_tenant());

-- Datos iniciales para EUR como moneda base
-- Insertar tasas básicas (1 EUR = 1 EUR, 1 EUR = 1.10 USD, etc.)
INSERT INTO hr_app.currency_exchange_rates (id, tenant_id, from_currency, to_currency, exchange_rate, effective_date, source, status)
VALUES 
    -- EUR como base (1 EUR = 1 EUR)
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'EUR', 1.000000, CURRENT_DATE, 'system', 'active'),
    -- Tasas comunes (aproximadas)
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'USD', 1.100000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'GBP', 0.860000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'JPY', 160.000000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'CAD', 1.480000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'AUD', 1.650000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'CHF', 0.950000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'CNY', 7.800000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'INR', 91.000000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'BRL', 5.400000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'MXN', 18.500000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'ARS', 950.000000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'CLP', 1050.000000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'COP', 4200.000000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'PEN', 4.100000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'UYU', 42.000000, CURRENT_DATE, 'system', 'active'),
    (gen_random_uuid()::text, 'demo-tenant', 'EUR', 'VES', 35.000000, CURRENT_DATE, 'system', 'active');
