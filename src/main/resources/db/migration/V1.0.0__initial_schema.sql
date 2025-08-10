-- Initial consolidated schema for development

-- Schema
CREATE SCHEMA IF NOT EXISTS hr_app;

-- Session tenant function
CREATE OR REPLACE FUNCTION hr_app.current_tenant()
RETURNS text LANGUAGE sql STABLE AS $$
  SELECT current_setting('app.current_tenant', true)
$$;

-- Envers revinfo table (long id)
CREATE TABLE IF NOT EXISTS hr_app.revinfo (
  id BIGSERIAL PRIMARY KEY,
  timestamp BIGINT NOT NULL
);

-- Tenants table
CREATE TABLE IF NOT EXISTS hr_app.tenant (
  id varchar(100) NOT NULL,
  tenant_id varchar(100) NOT NULL,
  name varchar(255) NOT NULL,
  domain varchar(255),
  status varchar(50) NOT NULL DEFAULT 'pending',
  date_created timestamptz,
  date_status_update timestamptz,
  max_users integer,
  subscription_plan varchar(50),
  CONSTRAINT tenant_pk PRIMARY KEY (id, tenant_id)
);

-- Unique per tenant_id regardless of id
CREATE UNIQUE INDEX IF NOT EXISTS tenant_tenant_id_uidx ON hr_app.tenant(tenant_id);

-- Tenant extended attributes
CREATE TABLE IF NOT EXISTS hr_app.tenant_extended_attributes (
  id varchar(100) NOT NULL,
  tenant_id varchar(100) NOT NULL,
  attribute_key varchar(255) NOT NULL,
  attribute_value varchar(1000),
  attribute_type varchar(100),
  CONSTRAINT tenant_ext_attr_pk PRIMARY KEY (id, tenant_id, attribute_key),
  CONSTRAINT tenant_ext_attr_fk FOREIGN KEY (id, tenant_id)
    REFERENCES hr_app.tenant(id, tenant_id) ON DELETE CASCADE
);

-- Users table
CREATE TABLE IF NOT EXISTS hr_app.users (
  id varchar(100) NOT NULL,
  tenant_id varchar(100) NOT NULL,
  firstName varchar(255),
  lastName varchar(255),
  email varchar(320) NOT NULL,
  status varchar(50) NOT NULL DEFAULT 'pending',
  date_created timestamptz,
  date_status_update timestamptz,
  last_login timestamptz,
  CONSTRAINT users_pk PRIMARY KEY (id, tenant_id)
);

-- Unique email per tenant
CREATE UNIQUE INDEX IF NOT EXISTS users_tenant_email_uidx
  ON hr_app.users(tenant_id, email);

-- Useful secondary indexes
CREATE INDEX IF NOT EXISTS users_tenant_status_idx ON hr_app.users(tenant_id, status);
CREATE INDEX IF NOT EXISTS users_tenant_date_created_idx ON hr_app.users(tenant_id, date_created);
CREATE INDEX IF NOT EXISTS tenant_status_idx ON hr_app.tenant(status);
CREATE UNIQUE INDEX IF NOT EXISTS tenant_domain_uidx ON hr_app.tenant(domain) WHERE domain IS NOT NULL;

-- User roles
CREATE TABLE IF NOT EXISTS hr_app.user_roles (
  id varchar(100) NOT NULL,
  tenant_id varchar(100) NOT NULL,
  role varchar(255) NOT NULL,
  CONSTRAINT user_roles_pk PRIMARY KEY (id, tenant_id, role),
  CONSTRAINT user_roles_fk FOREIGN KEY (id, tenant_id)
    REFERENCES hr_app.users(id, tenant_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS user_roles_tenant_role_idx ON hr_app.user_roles(tenant_id, role);

-- User extended attributes
CREATE TABLE IF NOT EXISTS hr_app.user_extended_attributes (
  id varchar(100) NOT NULL,
  tenant_id varchar(100) NOT NULL,
  attribute_key varchar(255) NOT NULL,
  attribute_value varchar(1000),
  attribute_type varchar(100),
  CONSTRAINT user_ext_attr_pk PRIMARY KEY (id, tenant_id, attribute_key),
  CONSTRAINT user_ext_attr_fk FOREIGN KEY (id, tenant_id)
    REFERENCES hr_app.users(id, tenant_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS user_ext_attr_tenant_idx ON hr_app.user_extended_attributes(tenant_id);

-- Tenant extended attributes tenant filter index
CREATE INDEX IF NOT EXISTS tenant_ext_attr_tenant_idx ON hr_app.tenant_extended_attributes(tenant_id);

-- Envers audit tables (optional in dev to silence warnings)
CREATE TABLE IF NOT EXISTS hr_app.tenant_aud (
  id varchar(100) NOT NULL,
  tenant_id varchar(100) NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  date_status_update timestamptz,
  status varchar(50),
  PRIMARY KEY (id, rev, tenant_id)
);
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema='hr_app' AND table_name='tenant_aud' AND constraint_name='fk_tenant_aud_rev'
  ) THEN
    EXECUTE 'ALTER TABLE hr_app.tenant_aud ADD CONSTRAINT fk_tenant_aud_rev FOREIGN KEY (rev) REFERENCES hr_app.revinfo';
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS hr_app.users_aud (
  id varchar(100) NOT NULL,
  tenant_id varchar(100) NOT NULL,
  rev bigint NOT NULL,
  revtype smallint,
  date_status_update timestamptz,
  status varchar(50),
  PRIMARY KEY (id, rev, tenant_id)
);
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema='hr_app' AND table_name='users_aud' AND constraint_name='fk_users_aud_rev'
  ) THEN
    EXECUTE 'ALTER TABLE hr_app.users_aud ADD CONSTRAINT fk_users_aud_rev FOREIGN KEY (rev) REFERENCES hr_app.revinfo';
  END IF;
END $$;

-- RLS enable
ALTER TABLE hr_app.tenant ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.tenant_extended_attributes ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.user_extended_attributes ENABLE ROW LEVEL SECURITY;

-- RLS policies
DROP POLICY IF EXISTS tenant_isolation_select ON hr_app.tenant;
CREATE POLICY tenant_isolation_select ON hr_app.tenant
  FOR SELECT USING (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS tenant_isolation_mod ON hr_app.tenant;
CREATE POLICY tenant_isolation_mod ON hr_app.tenant
  FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS tenant_ext_attr_isolation ON hr_app.tenant_extended_attributes;
CREATE POLICY tenant_ext_attr_isolation ON hr_app.tenant_extended_attributes
  FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS users_isolation ON hr_app.users;
CREATE POLICY users_isolation ON hr_app.users
  FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS user_roles_isolation ON hr_app.user_roles;
CREATE POLICY user_roles_isolation ON hr_app.user_roles
  FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

DROP POLICY IF EXISTS user_ext_attr_isolation ON hr_app.user_extended_attributes;
CREATE POLICY user_ext_attr_isolation ON hr_app.user_extended_attributes
  FOR ALL USING (tenant_id = hr_app.current_tenant()) WITH CHECK (tenant_id = hr_app.current_tenant());

-- Constraints
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema='hr_app' AND table_name='users' AND constraint_name='users_status_chk'
  ) THEN
    EXECUTE 'ALTER TABLE hr_app.users
              ADD CONSTRAINT users_status_chk CHECK (status IN (''active'',''inactive'',''suspended'',''pending'',''deleted''))';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema='hr_app' AND table_name='tenant' AND constraint_name='tenant_status_chk'
  ) THEN
    EXECUTE 'ALTER TABLE hr_app.tenant
              ADD CONSTRAINT tenant_status_chk CHECK (status IN (''active'',''inactive'',''suspended'',''pending''))';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema='hr_app' AND table_name='tenant' AND constraint_name='tenant_max_users_chk'
  ) THEN
    EXECUTE 'ALTER TABLE hr_app.tenant
              ADD CONSTRAINT tenant_max_users_chk CHECK (max_users IS NULL OR max_users >= 0)';
  END IF;
END $$;

-- System info (optional)
CREATE TABLE IF NOT EXISTS hr_app.system_info (
    id SERIAL PRIMARY KEY,
    app_name VARCHAR(255) DEFAULT 'hr-platform',
    version VARCHAR(50) DEFAULT '1.0.0',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO hr_app.system_info (app_name, version) VALUES ('hr-platform', '1.0.0');