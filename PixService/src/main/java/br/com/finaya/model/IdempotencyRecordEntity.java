package br.com.finaya.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "record_key", nullable = false, unique = true)
    private String recordKey;
    
    @Column(name = "result_type")
    private String resultType;
    
    @Column(name = "result_data", columnDefinition = "JSON")
    private String resultData;
    
    @Column(name = "error_type")
    private String errorType;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public IdempotencyRecordEntity() {}
    
    public IdempotencyRecordEntity(String recordKey) {
        this.recordKey = recordKey;
    }
    
    public void markSuccess(Object result) {
        this.resultType = result != null ? result.getClass().getName() : null;
        this.resultData = result != null ? result.toString() : null;
        this.errorType = null;
        this.errorMessage = null;
    }

    public void markError(String errorType, String errorMessage) {
        this.resultType = null;
        this.resultData = null;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getRecordKey() { return recordKey; }
    public void setRecordKey(String recordKey) { this.recordKey = recordKey; }
    
    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }
    
    public String getResultData() { return resultData; }
    public void setResultData(String resultData) { this.resultData = resultData; }
    
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
