-- Fix para la secuencia de Envers que Hibernate espera

-- 1. Primero crear la nueva secuencia
CREATE SEQUENCE IF NOT EXISTS hr_app.REVINFO_SEQ START WITH 1 INCREMENT BY 50;

-- 2. Cambiar el default de la tabla
ALTER TABLE hr_app.revinfo ALTER COLUMN rev SET DEFAULT nextval('hr_app.REVINFO_SEQ');

-- 3. Ahora sí podemos drop la secuencia antigua (con CASCADE)
DROP SEQUENCE IF EXISTS hr_app.hibernate_sequence CASCADE;