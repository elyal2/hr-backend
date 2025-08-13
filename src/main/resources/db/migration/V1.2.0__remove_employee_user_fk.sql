-- Remove foreign key constraint that requires employees to have corresponding user records
-- This allows employees to exist independently of user accounts

ALTER TABLE hr_app.employees DROP CONSTRAINT IF EXISTS employees_user_fk;
