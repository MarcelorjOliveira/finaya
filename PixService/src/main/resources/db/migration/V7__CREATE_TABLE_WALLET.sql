CREATE TABLE IF NOT EXISTS wallets (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);