-- Account table with multi-tenant support and extended attributes

-- Function for RLS (Row Level Security)
CREATE OR REPLACE FUNCTION get_current_tenant()
RETURNS VARCHAR AS $$
DECLARE
    tenant_id VARCHAR;
BEGIN
    tenant_id := current_setting('app.current_tenant', true);
    IF tenant_id IS NULL OR tenant_id = '' THEN
        tenant_id := 'default';
    END IF;
    RETURN tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Account table
CREATE TABLE hr_app.account (
    id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL DEFAULT get_current_tenant(),
    name VARCHAR(255),
    surname VARCHAR(255),
    status INTEGER DEFAULT 1 CHECK (status IN (1, 2, 3, 4)),
    email VARCHAR(255),
    date_registered TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    date_status_update TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, tenant_id),
    UNIQUE (email, tenant_id)
);

-- Extended Attributes table
CREATE TABLE hr_app.account_extended_attributes (
    id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    attribute_key VARCHAR(255) NOT NULL,
    attribute_value VARCHAR(1000),
    attribute_type VARCHAR(50) DEFAULT 'STRING',
    PRIMARY KEY (id, tenant_id, attribute_key),
    FOREIGN KEY (id, tenant_id) REFERENCES hr_app.account(id, tenant_id) ON DELETE CASCADE
);

-- Enable Row Level Security
ALTER TABLE hr_app.account ENABLE ROW LEVEL SECURITY;
ALTER TABLE hr_app.account_extended_attributes ENABLE ROW LEVEL SECURITY;

-- RLS Policies - only see data from your tenant
CREATE POLICY tenant_isolation_policy ON hr_app.account
    FOR ALL
    TO PUBLIC
    USING (tenant_id = get_current_tenant());

CREATE POLICY tenant_isolation_policy ON hr_app.account_extended_attributes
    FOR ALL
    TO PUBLIC
    USING (tenant_id = get_current_tenant());

-- Audit tables for Envers
CREATE TABLE hr_app.account_aud (
    id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    name VARCHAR(255),
    surname VARCHAR(255),
    status INTEGER,
    email VARCHAR(255),
    date_registered TIMESTAMP WITH TIME ZONE,
    date_status_update TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, tenant_id, rev)
);

-- Revision info table (required by Envers)
CREATE SEQUENCE IF NOT EXISTS hr_app.hibernate_sequence START 1;

CREATE TABLE hr_app.revinfo (
    rev INTEGER NOT NULL DEFAULT nextval('hr_app.hibernate_sequence'),
    revtstmp BIGINT,
    PRIMARY KEY (rev)
);

-- Indexes for performance
CREATE INDEX idx_account_tenant ON hr_app.account(tenant_id);
CREATE INDEX idx_account_email ON hr_app.account(email, tenant_id);
CREATE INDEX idx_account_status ON hr_app.account(status, tenant_id);
CREATE INDEX idx_account_attributes_tenant ON hr_app.account_extended_attributes(tenant_id);
CREATE INDEX idx_account_attributes_key ON hr_app.account_extended_attributes(attribute_key, tenant_id);

-- Test data
DO $$
BEGIN
    -- Set context for demo tenant
    PERFORM set_config('app.current_tenant', 'demo-tenant', false);
    
    -- Insert test accounts
    INSERT INTO hr_app.account (id, name, surname, email, status)
    VALUES 
        ('acc-001', 'John', 'Doe', 'john.doe@demo.com', 1),
        ('acc-002', 'Jane', 'Smith', 'jane.smith@demo.com', 2),
        ('acc-003', 'Admin', 'User', 'admin@demo.com', 1)
    ON CONFLICT (id, tenant_id) DO NOTHING;
    
    -- Insert extended attributes
    INSERT INTO hr_app.account_extended_attributes (id, tenant_id, attribute_key, attribute_value, attribute_type)
    VALUES 
        ('acc-001', 'demo-tenant', 'department', 'Engineering', 'STRING'),
        ('acc-001', 'demo-tenant', 'level', 'Senior', 'STRING'),
        ('acc-002', 'demo-tenant', 'department', 'Marketing', 'STRING'),
        ('acc-003', 'demo-tenant', 'role', 'Administrator', 'STRING')
    ON CONFLICT (id, tenant_id, attribute_key) DO NOTHING;

    -- Test different tenant
    PERFORM set_config('app.current_tenant', 'other-tenant', false);
    
    INSERT INTO hr_app.account (id, name, surname, email, status)
    VALUES ('acc-100', 'Other', 'User', 'user@other.com', 1)
    ON CONFLICT (id, tenant_id) DO NOTHING;

END $$;