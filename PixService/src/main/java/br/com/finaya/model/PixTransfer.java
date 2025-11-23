package br.com.finaya.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "pix_transfers")
public class PixTransfer {

    @Id
    @Column(name = "end_to_end_id")
    private UUID endToEndId;

    @Column(name = "from_wallet_id", nullable = false)
    private UUID fromWalletId;

    @Column(name = "to_wallet_id", nullable = false)
    private UUID toWalletId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey; 

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Enums
    public enum TransferStatus {
        PENDING, CONFIRMED, REJECTED
    }

    // Constructors
    public PixTransfer() {}

    public PixTransfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount, UUID idempotencyKey) {
        this.endToEndId = UUID.randomUUID();
    	this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = TransferStatus.PENDING;
    }

    // Business methods
    public void confirm() {
        if (this.status != TransferStatus.PENDING) {
            throw new IllegalStateException("Only pending transfers can be confirmed");
        }
        this.status = TransferStatus.CONFIRMED;
    }

    public void reject() {
        if (this.status != TransferStatus.PENDING) {
            throw new IllegalStateException("Only pending transfers can be rejected");
        }
        this.status = TransferStatus.REJECTED;
    }

    // Getters and Setters
    public UUID getEndToEndId() { return endToEndId; }
    public void setEndToEndId(UUID endToEndId) { this.endToEndId = endToEndId; }

    public UUID getFromWalletId() { return fromWalletId; }
    public void setFromWalletId(UUID fromWalletId) { this.fromWalletId = fromWalletId; }

    public UUID getToWalletId() { return toWalletId; }
    public void setToWalletId(UUID toWalletId) { this.toWalletId = toWalletId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }

    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}