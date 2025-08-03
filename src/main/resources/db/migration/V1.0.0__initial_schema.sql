-- Simple initial setup just to test Flyway works

CREATE SCHEMA IF NOT EXISTS hr_app;

-- Simple test table
CREATE TABLE hr_app.system_info (
    id SERIAL PRIMARY KEY,
    app_name VARCHAR(255) DEFAULT 'hr-platform',
    version VARCHAR(50) DEFAULT '1.0.0',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial data
INSERT INTO hr_app.system_info (app_name, version) 
VALUES ('hr-platform', '1.0.0');