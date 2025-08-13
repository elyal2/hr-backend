-- Organization Module Migration
-- V1.1.0 - Organization Module

-- Constants for organization module
DO $$
BEGIN
    -- Employee types
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'employee_type_enum') THEN
        CREATE TYPE hr_app.employee_type_enum AS ENUM (
            'employee', 'contractor', 'intern', 'consultant', 'temporary'
        );
    END IF;
    
    -- Contract types
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'contract_type_enum') THEN
        CREATE TYPE hr_app.contract_type_enum AS ENUM (
            'full_time', 'part_time', 'fixed_term', 'indefinite', 'project_based'
        );
    END IF;
    
    -- Movement reasons
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'movement_reason_enum') THEN
        CREATE TYPE hr_app.movement_reason_enum AS ENUM (
            'promotion', 'lateral_move', 'demotion', 'restructuring', 'new_position',
            'temporary_assignment', 'return_from_assignment', 'termination', 'resignation'
        );
    END IF;
END $$;

-- Organizational Levels table
CREATE TABLE IF NOT EXISTS hr_app.organizational_levels (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    description text,
    level_order integer NOT NULL,
    status varchar(50) NOT NULL DEFAULT 'active',
    date_created timestamptz DEFAULT CURRENT_TIMESTAMP,
    date_updated timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT organizational_levels_pk PRIMARY KEY (id, tenant_id)
);

-- Organizational Units table
CREATE TABLE IF NOT EXISTS hr_app.organizational_units (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    description text,
    parent_unit_id varchar(100),
    parent_unit_tenant_id varchar(100),
    level_id varchar(100),
    level_tenant_id varchar(100),
    cost_center varchar(255),
    location varchar(255),
    country varchar(255),
    status varchar(50) NOT NULL DEFAULT 'active',
    date_created timestamptz DEFAULT CURRENT_TIMESTAMP,
    date_updated timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT organizational_units_pk PRIMARY KEY (id, tenant_id),
    CONSTRAINT organizational_units_parent_fk FOREIGN KEY (parent_unit_id, parent_unit_tenant_id)
        REFERENCES hr_app.organizational_units(id, tenant_id) ON DELETE SET NULL,
    CONSTRAINT organizational_units_level_fk FOREIGN KEY (level_id, level_tenant_id)
        REFERENCES hr_app.organizational_levels(id, tenant_id) ON DELETE SET NULL
);

-- Job Positions table
CREATE TABLE IF NOT EXISTS hr_app.job_positions (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    unit_id varchar(100),
    unit_tenant_id varchar(100),
    level_id varchar(100),
    level_tenant_id varchar(100),
    job_code varchar(255),
    status varchar(50) NOT NULL DEFAULT 'active',
    date_created timestamptz DEFAULT CURRENT_TIMESTAMP,
    date_updated timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT job_positions_pk PRIMARY KEY (id, tenant_id),
    CONSTRAINT job_positions_unit_fk FOREIGN KEY (unit_id, unit_tenant_id)
        REFERENCES hr_app.organizational_units(id, tenant_id) ON DELETE SET NULL,
    CONSTRAINT job_positions_level_fk FOREIGN KEY (level_id, level_tenant_id)
        REFERENCES hr_app.organizational_levels(id, tenant_id) ON DELETE SET NULL
);

-- Employees table (extends User)
CREATE TABLE IF NOT EXISTS hr_app.employees (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    employee_id varchar(255) NOT NULL,
    first_name varchar(255) NOT NULL,
    last_name varchar(255) NOT NULL,
    email varchar(320) NOT NULL,
    date_of_birth date,
    gender varchar(255),
    national_id varchar(255),
    tax_id varchar(255),
    employee_type varchar(255) NOT NULL DEFAULT 'employee',
    contract_type varchar(255),
    hire_date date NOT NULL,
    termination_date date,
    current_salary decimal(15,2),
    currency varchar(3) DEFAULT 'USD',
    status varchar(50) NOT NULL DEFAULT 'active',
    date_created timestamptz DEFAULT CURRENT_TIMESTAMP,
    date_updated timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT employees_pk PRIMARY KEY (id, tenant_id),
    CONSTRAINT employees_user_fk FOREIGN KEY (id, tenant_id)
        REFERENCES hr_app.users(id, tenant_id) ON DELETE CASCADE
);

-- Employee Assignments table (for historical tracking)
CREATE TABLE IF NOT EXISTS hr_app.employee_assignments (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    employee_id varchar(100) NOT NULL,
    employee_tenant_id varchar(100) NOT NULL,
    position_id varchar(100),
    position_tenant_id varchar(100),
    unit_id varchar(100),
    unit_tenant_id varchar(100),
    manager_id varchar(100),
    manager_tenant_id varchar(100),
    start_date date NOT NULL,
    end_date date,
    salary decimal(15,2),
    currency varchar(3) DEFAULT 'USD',
    movement_reason varchar(255),
    notes text,
    date_created timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT employee_assignments_pk PRIMARY KEY (id, tenant_id),
    CONSTRAINT employee_assignments_employee_fk FOREIGN KEY (employee_id, employee_tenant_id)
        REFERENCES hr_app.employees(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT employee_assignments_position_fk FOREIGN KEY (position_id, position_tenant_id)
        REFERENCES hr_app.job_positions(id, tenant_id) ON DELETE SET NULL,
    CONSTRAINT employee_assignments_unit_fk FOREIGN KEY (unit_id, unit_tenant_id)
        REFERENCES hr_app.organizational_units(id, tenant_id) ON DELETE SET NULL,
    CONSTRAINT employee_assignments_manager_fk FOREIGN KEY (manager_id, manager_tenant_id)
        REFERENCES hr_app.employees(id, tenant_id) ON DELETE SET NULL
);

-- Temporary Replacements table
CREATE TABLE IF NOT EXISTS hr_app.temporary_replacements (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    original_employee_id varchar(100) NOT NULL,
    original_employee_tenant_id varchar(100) NOT NULL,
    replacement_employee_id varchar(100) NOT NULL,
    replacement_employee_tenant_id varchar(100) NOT NULL,
    position_id varchar(100),
    position_tenant_id varchar(100),
    start_date date NOT NULL,
    end_date date,
    reason text,
    status varchar(50) NOT NULL DEFAULT 'active',
    date_created timestamptz DEFAULT CURRENT_TIMESTAMP,
    date_updated timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT temporary_replacements_pk PRIMARY KEY (id, tenant_id),
    CONSTRAINT temporary_replacements_original_fk FOREIGN KEY (original_employee_id, original_employee_tenant_id)
        REFERENCES hr_app.employees(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT temporary_replacements_replacement_fk FOREIGN KEY (replacement_employee_id, replacement_employee_tenant_id)
        REFERENCES hr_app.employees(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT temporary_replacements_position_fk FOREIGN KEY (position_id, position_tenant_id)
        REFERENCES hr_app.job_positions(id, tenant_id) ON DELETE SET NULL
);

-- Salary History table
CREATE TABLE IF NOT EXISTS hr_app.salary_history (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    employee_id varchar(100) NOT NULL,
    employee_tenant_id varchar(100) NOT NULL,
    old_salary decimal(15,2),
    new_salary decimal(15,2) NOT NULL,
    currency varchar(3) DEFAULT 'USD',
    effective_date date NOT NULL,
    reason text,
    approved_by varchar(100),
    approved_by_tenant_id varchar(100),
    date_created timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT salary_history_pk PRIMARY KEY (id, tenant_id),
    CONSTRAINT salary_history_employee_fk FOREIGN KEY (employee_id, employee_tenant_id)
        REFERENCES hr_app.employees(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT salary_history_approved_by_fk FOREIGN KEY (approved_by, approved_by_tenant_id)
        REFERENCES hr_app.employees(id, tenant_id) ON DELETE SET NULL
);

-- Extended attributes for organization entities
CREATE TABLE IF NOT EXISTS hr_app.organizational_levels_extended_attributes (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    attribute_key varchar(255) NOT NULL,
    attribute_value varchar(1000),
    attribute_type varchar(100),
    CONSTRAINT org_levels_ext_attr_pk PRIMARY KEY (id, tenant_id, attribute_key),
    CONSTRAINT org_levels_ext_attr_fk FOREIGN KEY (id, tenant_id)
        REFERENCES hr_app.organizational_levels(id, tenant_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS hr_app.organizational_units_extended_attributes (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    attribute_key varchar(255) NOT NULL,
    attribute_value varchar(1000),
    attribute_type varchar(100),
    CONSTRAINT org_units_ext_attr_pk PRIMARY KEY (id, tenant_id, attribute_key),
    CONSTRAINT org_units_ext_attr_fk FOREIGN KEY (id, tenant_id)
        REFERENCES hr_app.organizational_units(id, tenant_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS hr_app.job_positions_extended_attributes (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    attribute_key varchar(255) NOT NULL,
    attribute_value varchar(1000),
    attribute_type varchar(100),
    CONSTRAINT job_positions_ext_attr_pk PRIMARY KEY (id, tenant_id, attribute_key),
    CONSTRAINT job_positions_ext_attr_fk FOREIGN KEY (id, tenant_id)
        REFERENCES hr_app.job_positions(id, tenant_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS hr_app.employees_extended_attributes (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    attribute_key varchar(255) NOT NULL,
    attribute_value varchar(1000),
    attribute_type varchar(100),
    CONSTRAINT employees_ext_attr_pk PRIMARY KEY (id, tenant_id, attribute_key),
    CONSTRAINT employees_ext_attr_fk FOREIGN KEY (id, tenant_id)
        REFERENCES hr_app.employees(id, tenant_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS organizational_levels_tenant_status_idx ON hr_app.organizational_levels(tenant_id, status);
CREATE INDEX IF NOT EXISTS organizational_levels_tenant_order_idx ON hr_app.organizational_levels(tenant_id, level_order);
CREATE INDEX IF NOT EXISTS organizational_units_tenant_status_idx ON hr_app.organizational_units(tenant_id, status);
CREATE INDEX IF NOT EXISTS organizational_units_parent_idx ON hr_app.organizational_units(parent_unit_id, parent_unit_tenant_id);
CREATE INDEX IF NOT EXISTS organizational_units_level_idx ON hr_app.organizational_units(level_id, level_tenant_id);
CREATE INDEX IF NOT EXISTS job_positions_tenant_status_idx ON hr_app.job_positions(tenant_id, status);
CREATE INDEX IF NOT EXISTS job_positions_unit_idx ON hr_app.job_positions(unit_id, unit_tenant_id);
CREATE INDEX IF NOT EXISTS job_positions_level_idx ON hr_app.job_positions(level_id, level_tenant_id);
CREATE INDEX IF NOT EXISTS employees_tenant_status_idx ON hr_app.employees(tenant_id, status);
CREATE INDEX IF NOT EXISTS employees_employee_id_idx ON hr_app.employees(tenant_id, employee_id);
CREATE INDEX IF NOT EXISTS employees_email_idx ON hr_app.employees(tenant_id, email);
CREATE INDEX IF NOT EXISTS employees_hire_date_idx ON hr_app.employees(tenant_id, hire_date);
CREATE INDEX IF NOT EXISTS employee_assignments_employee_idx ON hr_app.employee_assignments(employee_id, employee_tenant_id);
CREATE INDEX IF NOT EXISTS employee_assignments_position_idx ON hr_app.employee_assignments(position_id, position_tenant_id);
CREATE INDEX IF NOT EXISTS employee_assignments_unit_idx ON hr_app.employee_assignments(unit_id, unit_tenant_id);
CREATE INDEX IF NOT EXISTS employee_assignments_manager_idx ON hr_app.employee_assignments(manager_id, manager_tenant_id);
CREATE INDEX IF NOT EXISTS employee_assignments_date_range_idx ON hr_app.employee_assignments(start_date, end_date);
CREATE INDEX IF NOT EXISTS temporary_replacements_original_idx ON hr_app.temporary_replacements(original_employee_id, original_employee_tenant_id);
CREATE INDEX IF NOT EXISTS temporary_replacements_replacement_idx ON hr_app.temporary_replacements(replacement_employee_id, replacement_employee_tenant_id);
CREATE INDEX IF NOT EXISTS temporary_replacements_status_idx ON hr_app.temporary_replacements(tenant_id, status);
CREATE INDEX IF NOT EXISTS salary_history_employee_idx ON hr_app.salary_history(employee_id, employee_tenant_id);
CREATE INDEX IF NOT EXISTS salary_history_effective_date_idx ON hr_app.salary_history(effective_date);

-- Unique constraints
CREATE UNIQUE INDEX IF NOT EXISTS employees_tenant_employee_id_uidx ON hr_app.employees(tenant_id, employee_id);
CREATE UNIQUE INDEX IF NOT EXISTS employees_tenant_email_uidx ON hr_app.employees(tenant_id, email);
CREATE UNIQUE INDEX IF NOT EXISTS organizational_levels_tenant_order_uidx ON hr_app.organizational_levels(tenant_id, level_order);

-- RLS enable
ALTER TABLE hr_app.organizational_levels ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.organizational_units ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.job_positions ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.employees ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.employee_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.temporary_replacements ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.salary_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.organizational_levels_extended_attributes ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.organizational_units_extended_attributes ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.job_positions_extended_attributes ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.employees_extended_attributes ENABLE ROW LEVEL SECURITY;

-- RLS policies
DROP POLICY IF EXISTS organizational_levels_isolation ON hr_app.organizational_levels;
CREATE POLICY organizational_levels_isolation ON hr_app.organizational_levels
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS organizational_units_isolation ON hr_app.organizational_units;
CREATE POLICY organizational_units_isolation ON hr_app.organizational_units
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS job_positions_isolation ON hr_app.job_positions;
CREATE POLICY job_positions_isolation ON hr_app.job_positions
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS employees_isolation ON hr_app.employees;
CREATE POLICY employees_isolation ON hr_app.employees
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS employee_assignments_isolation ON hr_app.employee_assignments;
CREATE POLICY employee_assignments_isolation ON hr_app.employee_assignments
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS temporary_replacements_isolation ON hr_app.temporary_replacements;
CREATE POLICY temporary_replacements_isolation ON hr_app.temporary_replacements
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS salary_history_isolation ON hr_app.salary_history;
CREATE POLICY salary_history_isolation ON hr_app.salary_history
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS org_levels_ext_attr_isolation ON hr_app.organizational_levels_extended_attributes;
CREATE POLICY org_levels_ext_attr_isolation ON hr_app.organizational_levels_extended_attributes
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS org_units_ext_attr_isolation ON hr_app.organizational_units_extended_attributes;
CREATE POLICY org_units_ext_attr_isolation ON hr_app.organizational_units_extended_attributes
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS job_positions_ext_attr_isolation ON hr_app.job_positions_extended_attributes;
CREATE POLICY job_positions_ext_attr_isolation ON hr_app.job_positions_extended_attributes
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS employees_ext_attr_isolation ON hr_app.employees_extended_attributes;
CREATE POLICY employees_ext_attr_isolation ON hr_app.employees_extended_attributes
    FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

-- Constraints
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='organizational_levels' AND constraint_name='org_levels_status_chk'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.organizational_levels
                  ADD CONSTRAINT org_levels_status_chk CHECK (status IN (''active'',''inactive'',''deleted''))';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='organizational_units' AND constraint_name='org_units_status_chk'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.organizational_units
                  ADD CONSTRAINT org_units_status_chk CHECK (status IN (''active'',''inactive'',''deleted''))';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='job_positions' AND constraint_name='job_positions_status_chk'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.job_positions
                  ADD CONSTRAINT job_positions_status_chk CHECK (status IN (''active'',''inactive'',''deleted''))';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='employees' AND constraint_name='employees_status_chk'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.employees
                  ADD CONSTRAINT employees_status_chk CHECK (status IN (''active'',''inactive'',''terminated'',''resigned''))';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='temporary_replacements' AND constraint_name='temp_replacements_status_chk'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.temporary_replacements
                  ADD CONSTRAINT temp_replacements_status_chk CHECK (status IN (''active'',''completed'',''cancelled''))';
    END IF;
END $$;

-- Audit tables for organization entities
CREATE TABLE IF NOT EXISTS hr_app.organizational_levels_aud (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    rev bigint NOT NULL,
    revtype smallint,
    name varchar(255),
    description text,
    level_order integer,
    status varchar(50),
    date_created timestamptz,
    date_updated timestamptz,
    PRIMARY KEY (id, rev, tenant_id)
);

CREATE TABLE IF NOT EXISTS hr_app.organizational_units_aud (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    rev bigint NOT NULL,
    revtype smallint,
    name varchar(255),
    description text,
    parent_unit_id varchar(100),
    parent_unit_tenant_id varchar(100),
    level_id varchar(100),
    level_tenant_id varchar(100),
    cost_center varchar(255),
    location varchar(255),
    country varchar(255),
    status varchar(50),
    date_created timestamptz,
    date_updated timestamptz,
    PRIMARY KEY (id, rev, tenant_id)
);

CREATE TABLE IF NOT EXISTS hr_app.job_positions_aud (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    rev bigint NOT NULL,
    revtype smallint,
    title varchar(255),
    description text,
    unit_id varchar(100),
    unit_tenant_id varchar(100),
    level_id varchar(100),
    level_tenant_id varchar(100),
    job_code varchar(255),
    status varchar(50),
    date_created timestamptz,
    date_updated timestamptz,
    PRIMARY KEY (id, rev, tenant_id)
);

CREATE TABLE IF NOT EXISTS hr_app.employees_aud (
    id varchar(100) NOT NULL,
    tenant_id varchar(100) NOT NULL,
    rev bigint NOT NULL,
    revtype smallint,
    employee_id varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    email varchar(320),
    date_of_birth date,
    gender varchar(255),
    national_id varchar(255),
    tax_id varchar(255),
    employee_type varchar(255),
    contract_type varchar(255),
    hire_date date,
    termination_date date,
    current_salary decimal(15,2),
    currency varchar(3),
    status varchar(50),
    date_created timestamptz,
    date_updated timestamptz,
    PRIMARY KEY (id, rev, tenant_id)
);

-- Foreign key constraints for audit tables
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='organizational_levels_aud' AND constraint_name='fk_org_levels_aud_rev'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.organizational_levels_aud ADD CONSTRAINT fk_org_levels_aud_rev FOREIGN KEY (rev) REFERENCES hr_app.revinfo';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='organizational_units_aud' AND constraint_name='fk_org_units_aud_rev'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.organizational_units_aud ADD CONSTRAINT fk_org_units_aud_rev FOREIGN KEY (rev) REFERENCES hr_app.revinfo';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='job_positions_aud' AND constraint_name='fk_job_positions_aud_rev'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.job_positions_aud ADD CONSTRAINT fk_job_positions_aud_rev FOREIGN KEY (rev) REFERENCES hr_app.revinfo';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema='hr_app' AND table_name='employees_aud' AND constraint_name='fk_employees_aud_rev'
    ) THEN
        EXECUTE 'ALTER TABLE hr_app.employees_aud ADD CONSTRAINT fk_employees_aud_rev FOREIGN KEY (rev) REFERENCES hr_app.revinfo';
    END IF;
END $$;
