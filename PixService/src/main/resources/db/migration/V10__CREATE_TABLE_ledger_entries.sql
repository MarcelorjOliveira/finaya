CREATE TABLE IF NOT EXISTS ledger_entries (
    id BINARY(16) PRIMARY KEY,
    wallet_id BINARY(16) NOT NULL,
    transaction_id BINARY(16) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    type ENUM('DEPOSIT', 'WITHDRAWAL', 'PIX_OUT', 'PIX_IN', 'PIX_RESERVED') NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wallet_id (wallet_id),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_wallet_created (wallet_id, created_at),
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);