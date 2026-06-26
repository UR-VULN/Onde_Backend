ALTER TABLE members
    ADD COLUMN password_updated_at DATETIME NULL AFTER password;

UPDATE members
SET password_updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE password_updated_at IS NULL;

CREATE TABLE IF NOT EXISTS member_password_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_password_history_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    INDEX idx_member_password_history_member (member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
