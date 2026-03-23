-- Widen audit_log.action from VARCHAR(10) to VARCHAR(100)
-- to accommodate action names like SET_CATEGORIES, UPDATE_TEAM, etc.
ALTER TABLE audit_log ALTER COLUMN action TYPE VARCHAR(100);
