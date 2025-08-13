-- V1.4.0: Reorganizar estructura de posiciones - Script consolidado
-- Eliminar niveles organizativos y crear categorías de posiciones
-- Agregar nivel jerárquico a posiciones

-- ========== PARTE 1: CREAR NUEVAS TABLAS ==========

-- 1. Crear nueva tabla de categorías de posiciones
CREATE TABLE hr_app.position_categories (
    id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated TIMESTAMP,
    PRIMARY KEY (id, tenant_id)
);

-- 2. Agregar atributos extendidos para categorías
CREATE TABLE hr_app.position_categories_extended_attributes (
    id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    attribute_key VARCHAR(255) NOT NULL,
    attribute_value VARCHAR(1000),
    attribute_type VARCHAR(100) DEFAULT 'STRING',
    PRIMARY KEY (id, tenant_id, attribute_key),
    FOREIGN KEY (id, tenant_id) REFERENCES hr_app.position_categories(id, tenant_id)
);

-- 3. Agregar auditoría para categorías
CREATE TABLE hr_app.position_categories_aud (
    id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    date_created TIMESTAMP NOT NULL,
    date_updated TIMESTAMP,
    rev BIGINT NOT NULL,
    revtype SMALLINT,
    PRIMARY KEY (id, tenant_id, rev),
    FOREIGN KEY (rev) REFERENCES hr_app.revinfo
);

-- ========== PARTE 2: MODIFICAR TABLAS EXISTENTES ==========

-- 4. Modificar tabla de posiciones - agregar nivel jerárquico y categoría
ALTER TABLE hr_app.job_positions 
ADD COLUMN hierarchical_level INTEGER NOT NULL DEFAULT 1;

ALTER TABLE hr_app.job_positions 
ADD COLUMN category_id VARCHAR(100);

ALTER TABLE hr_app.job_positions 
ADD COLUMN category_tenant_id VARCHAR(100);

-- 5. Agregar foreign key para categorías
ALTER TABLE hr_app.job_positions 
ADD CONSTRAINT fk_job_positions_category 
FOREIGN KEY (category_id, category_tenant_id) 
REFERENCES hr_app.position_categories(id, tenant_id);

-- 6. Actualizar job_positions_aud para incluir las nuevas columnas
ALTER TABLE hr_app.job_positions_aud 
ADD COLUMN hierarchical_level INTEGER;

ALTER TABLE hr_app.job_positions_aud 
ADD COLUMN category_id VARCHAR(100);

ALTER TABLE hr_app.job_positions_aud 
ADD COLUMN category_tenant_id VARCHAR(100);

-- ========== PARTE 3: ELIMINAR ESTRUCTURAS ANTIGUAS ==========

-- 7. Eliminar foreign keys y columnas antiguas de posiciones
ALTER TABLE hr_app.job_positions 
DROP CONSTRAINT IF EXISTS job_positions_level_fk;

ALTER TABLE hr_app.job_positions 
DROP COLUMN IF EXISTS level_id;

ALTER TABLE hr_app.job_positions 
DROP COLUMN IF EXISTS level_tenant_id;

-- 8. Eliminar columnas antiguas de job_positions_aud
ALTER TABLE hr_app.job_positions_aud 
DROP COLUMN IF EXISTS level_id;

ALTER TABLE hr_app.job_positions_aud 
DROP COLUMN IF EXISTS level_tenant_id;

-- 9. Eliminar foreign keys y columnas de unidades organizativas
ALTER TABLE hr_app.organizational_units 
DROP CONSTRAINT IF EXISTS organizational_units_level_fk;

ALTER TABLE hr_app.organizational_units 
DROP COLUMN IF EXISTS level_id;

ALTER TABLE hr_app.organizational_units 
DROP COLUMN IF EXISTS level_tenant_id;

-- 10. Eliminar columnas de organizational_units_aud
ALTER TABLE hr_app.organizational_units_aud 
DROP COLUMN IF EXISTS level_id;

ALTER TABLE hr_app.organizational_units_aud 
DROP COLUMN IF EXISTS level_tenant_id;

-- 11. Eliminar tabla de niveles organizativos (sin migrar datos)
DROP TABLE IF EXISTS hr_app.organizational_levels_extended_attributes;
DROP TABLE IF EXISTS hr_app.organizational_levels_aud;
DROP TABLE IF EXISTS hr_app.organizational_levels;

-- ========== PARTE 4: CONFIGURAR SEGURIDAD Y OPTIMIZACIÓN ==========

-- 12. RLS para nuevas tablas
ALTER TABLE hr_app.position_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.position_categories_extended_attributes ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.position_categories_aud ENABLE ROW LEVEL SECURITY;

-- 13. Políticas RLS
CREATE POLICY position_categories_tenant_policy ON hr_app.position_categories
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::VARCHAR);

CREATE POLICY position_categories_ext_tenant_policy ON hr_app.position_categories_extended_attributes
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::VARCHAR);

CREATE POLICY position_categories_aud_tenant_policy ON hr_app.position_categories_aud
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::VARCHAR);

-- 14. Índices para optimizar consultas
CREATE INDEX idx_job_positions_hierarchical_level ON hr_app.job_positions(tenant_id, hierarchical_level);
CREATE INDEX idx_job_positions_category ON hr_app.job_positions(tenant_id, category_id);
CREATE INDEX idx_position_categories_name ON hr_app.position_categories(tenant_id, name);
