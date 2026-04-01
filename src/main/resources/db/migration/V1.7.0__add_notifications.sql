-- Migration V1.7.0: Add notifications table for user notifications
-- Purpose: Store system notifications for users about organizational events
-- (employee hiring, termination, salary changes, replacements, assignments)

CREATE TABLE hr_app.notifications (
    id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,      -- EMPLOYEE_HIRED, SALARY_CHANGED, etc.
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    entity_type VARCHAR(100),       -- 'Employee', 'SalaryHistory', etc.
    entity_id VARCHAR(255),
    read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, tenant_id)
);

-- Enable Row Level Security for tenant isolation
ALTER TABLE hr_app.notifications ENABLE ROW LEVEL SECURITY;

-- RLS policy: users can only see notifications for their tenant
CREATE POLICY tenant_isolation ON hr_app.notifications
    USING (tenant_id = current_setting('app.current_tenant', true));

-- Index for efficient queries: get unread notifications for a user, sorted by date
CREATE INDEX idx_notifications_user_unread 
    ON hr_app.notifications(tenant_id, user_id, read, created_at DESC);

-- Additional index for user-based queries (all notifications for a user)
CREATE INDEX idx_notifications_user_created 
    ON hr_app.notifications(tenant_id, user_id, created_at DESC);

-- Index for entity lookups (find notifications related to specific entities)
CREATE INDEX idx_notifications_entity 
    ON hr_app.notifications(tenant_id, entity_type, entity_id);
