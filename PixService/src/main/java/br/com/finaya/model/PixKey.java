package br.com.finaya.model;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "pix_keys",
    indexes = {
        @Index(name = "idx_pix_keys_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_pix_keys_key_value", columnList = "key_value"),
        @Index(name = "idx_pix_keys_status", columnList = "status"),
        @Index(name = "idx_pix_keys_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pix_keys_key_value", columnNames = {"key_value"})
    }
)
public class PixKey {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "wallet_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID walletId;

    @Column(name = "key_value", nullable = false, length = 255)
    private String keyValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PixKeyType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PixKeyStatus status = PixKeyStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public PixKey() {
    }
    
    public PixKey(UUID walletId, String keyValue, PixKeyType type) {
    	this.id = UUID.randomUUID();
    	this.walletId = walletId;
        this.keyValue = keyValue;
        this.type = type;
        this.status = PixKeyStatus.ACTIVE;
    }

    public PixKey(UUID id, UUID walletId, String keyValue, PixKeyType type) {
        this.id = id;
        this.walletId = walletId;
        this.keyValue = keyValue;
        this.type = type;
        this.status = PixKeyStatus.ACTIVE;
    }

    public PixKey(UUID id, UUID walletId, String keyValue, PixKeyType type, PixKeyStatus status, 
                       LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        this.id = id;
        this.walletId = walletId;
        this.keyValue = keyValue;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // Enums
    public enum PixKeyType {
        EMAIL,
        PHONE,
        EVP
    }

    public enum PixKeyStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED
    }

    // Business methods
    public void activate() {
        this.status = PixKeyStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void inactivate() {
        this.status = PixKeyStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void block() {
        this.status = PixKeyStatus.BLOCKED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return PixKeyStatus.ACTIVE.equals(this.status);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    public PixKeyType getType() {
        return type;
    }

    public void setType(PixKeyType type) {
        this.type = type;
    }

    public PixKeyStatus getStatus() {
        return status;
    }

    public void setStatus(PixKeyStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PixKey)) return false;

        PixKey that = (PixKey) o;

        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    // toString
    @Override
    public String toString() {
        return "PixKey{" +
                "id=" + id +
                ", walletId=" + walletId +
                ", keyValue='" + keyValue + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}
