ALTER TABLE members
    ADD COLUMN login_locked_until DATETIME NULL AFTER password_updated_at,
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0 AFTER login_locked_until;
