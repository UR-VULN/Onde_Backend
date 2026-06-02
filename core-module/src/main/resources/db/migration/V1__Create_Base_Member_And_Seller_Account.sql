CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100),
    name VARCHAR(100),
    provider_id VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_member_provider (provider, provider_id),
    INDEX idx_member_email (email),
    INDEX idx_member_role (role),
    INDEX idx_member_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS seller_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    bank_name VARCHAR(50) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    business_number VARCHAR(20) NOT NULL,
    representative_name VARCHAR(50) NOT NULL,
    opened_at VARCHAR(8) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_seller_accounts_member (member_id),
    UNIQUE KEY uk_seller_accounts_business_number (business_number),
    CONSTRAINT fk_seller_accounts_member
        FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
