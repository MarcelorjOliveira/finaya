-- Atualizar a tabela pix_transfers
ALTER TABLE pix_transfers MODIFY COLUMN idempotency_key BINARY(16) NOT NULL;

-- Atualizar a tabela idempotency_records  
ALTER TABLE idempotency_records MODIFY COLUMN record_key BINARY(16) NOT NULL;

-- Adicionar índices para UUIDs
CREATE INDEX idx_pix_transfers_idempotency_key ON pix_transfers (idempotency_key);
CREATE UNIQUE INDEX uk_idempotency_records_key ON idempotency_records (record_key);