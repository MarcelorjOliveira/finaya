package br.com.finaya.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Version
    private Long version;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public Wallet() {}
   
    public Wallet(UUID userId) {
    	this.id = UUID.randomUUID();
        this.userId = userId;
    }
    
    public Wallet(UUID id, UUID userId, BigDecimal balance) {
        this.id = id;
        this.userId = userId;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
