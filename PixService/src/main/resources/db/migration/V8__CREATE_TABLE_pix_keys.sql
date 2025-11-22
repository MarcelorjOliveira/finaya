CREATE TABLE IF NOT EXISTS pix_keys (
    id BINARY(16) PRIMARY KEY,
    wallet_id BINARY(16) NOT NULL,
    key_value VARCHAR(255) NOT NULL,
    type ENUM('EMAIL', 'PHONE', 'EVP') NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_pix_key (key_value),
    INDEX idx_wallet_id (wallet_id),
    INDEX idx_key_status (key_value, status),
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);