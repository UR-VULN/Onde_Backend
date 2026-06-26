-- JWT sub 비식별화용 고유 주체 ID (이메일 대신 사용)
ALTER TABLE members
    ADD COLUMN auth_subject_id VARCHAR(36) NULL COMMENT 'JWT subject (UUID)';

UPDATE members
SET auth_subject_id = UUID()
WHERE auth_subject_id IS NULL;

ALTER TABLE members
    MODIFY auth_subject_id VARCHAR(36) NOT NULL;

ALTER TABLE members
    ADD CONSTRAINT uk_member_auth_subject_id UNIQUE (auth_subject_id);
