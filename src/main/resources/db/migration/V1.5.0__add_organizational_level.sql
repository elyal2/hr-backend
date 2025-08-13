-- V1.5.0: Agregar nivel jerárquico organizacional a unidades

-- Agregar columna organizational_level a organizational_units
ALTER TABLE hr_app.organizational_units
ADD COLUMN organizational_level INTEGER NOT NULL DEFAULT 1;

-- Agregar columna organizational_level a organizational_units_aud
ALTER TABLE hr_app.organizational_units_aud
ADD COLUMN organizational_level INTEGER;

-- Crear índice para optimizar consultas por nivel organizacional
CREATE INDEX idx_organizational_units_level ON hr_app.organizational_units(tenant_id, organizational_level);

-- Actualizar datos existentes (opcional - establecer niveles basados en la estructura actual)
-- Por defecto, todas las unidades tendrán nivel 1, el usuario deberá ajustar manualmente
