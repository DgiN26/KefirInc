package com.example.TransactionSaga.model.entity;

import com.example.TransactionSaga.model.converter.StringToStringConverter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compensation_logs")
public class CompensationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "saga_step_id")
    private Long sagaStepId;

    @Column(name = "compensation_type", nullable = false)
    private String compensationType; // FULL, PARTIAL, ITEM_SPECIFIC

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Convert(converter = StringToStringConverter.class)
    @Column(name = "compensated_items", columnDefinition = "text")
    private String compensatedItems; // JSON список товаров для компенсации

    @Column(name = "status", nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Convert(converter = StringToStringConverter.class)
    @Column(name = "compensation_data", columnDefinition = "text")
    private String compensationData; // JSON дополнительные данные

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Long getSagaStepId() { return sagaStepId; }
    public void setSagaStepId(Long sagaStepId) { this.sagaStepId = sagaStepId; }

    public String getCompensationType() { return compensationType; }
    public void setCompensationType(String compensationType) { this.compensationType = compensationType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCompensatedItems() { return compensatedItems; }
    public void setCompensatedItems(String compensatedItems) { this.compensatedItems = compensatedItems; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        if ("IN_PROGRESS".equals(status) && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            this.completedAt = LocalDateTime.now();
            if (this.startedAt != null) {
                this.durationMs = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
            }
        }
    }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getCompensationData() { return compensationData; }
    public void setCompensationData(String compensationData) { this.compensationData = compensationData; }

    // Вспомогательные методы
    public boolean isPending() { return "PENDING".equals(this.status); }
    public boolean isInProgress() { return "IN_PROGRESS".equals(this.status); }
    public boolean isCompleted() { return "COMPLETED".equals(this.status); }
    public boolean isFailed() { return "FAILED".equals(this.status); }

    public void incrementRetryCount() { this.retryCount++; }
}