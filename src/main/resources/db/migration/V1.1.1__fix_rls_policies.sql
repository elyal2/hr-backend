-- V1.1.1: Corregir políticas RLS - eliminar políticas para tablas que ya no existen

-- Eliminar políticas RLS para tablas que ya no existen
DROP POLICY IF EXISTS organizational_levels_isolation ON hr_app.organizational_levels;
DROP POLICY IF EXISTS org_levels_ext_attr_isolation ON hr_app.organizational_levels_extended_attributes;

-- Las demás políticas RLS se mantienen ya que las tablas siguen existiendo
-- (organizational_units, job_positions, employees, etc.)
