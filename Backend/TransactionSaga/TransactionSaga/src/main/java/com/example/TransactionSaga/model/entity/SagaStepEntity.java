package com.example.TransactionSaga.model.entity;

import com.example.TransactionSaga.model.converter.StringToStringConverter;
import com.example.TransactionSaga.model.enums.SagaStepType;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_steps")
public class SagaStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false)
    private SagaStepType stepType;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED, COMPENSATED, SKIPPED

    @Convert(converter = StringToStringConverter.class)
    @Column(name = "step_data", columnDefinition = "text")
    private String stepData;

    @Convert(converter = StringToStringConverter.class)
    @Column(name = "compensation_data", columnDefinition = "text")
    private String compensationData;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "retry_delay_minutes")
    private Integer retryDelayMinutes = 5;

    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "compensated_at")
    private LocalDateTime compensatedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "is_compensatable")
    private Boolean isCompensatable = true;

    @Column(name = "depends_on_step_id")
    private Long dependsOnStepId;

    // === Конструкторы ===
    public SagaStepEntity() {}

    public SagaStepEntity(String transactionId, SagaStepType stepType, String status) {
        this.transactionId = transactionId;
        this.stepType = stepType;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // === Геттеры и сеттеры ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    // Возвращает строковое представление enum
    public String getStepType() {
        return stepType != null ? stepType.name() : "";
    }

    // Возвращает enum
    public SagaStepType getStepTypeEnum() {
        return stepType;
    }

    public void setStepType(SagaStepType stepType) {
        this.stepType = stepType;
    }

    // Установка из строки
    public void setStepTypeFromString(String stepTypeStr) {
        try {
            this.stepType = SagaStepType.valueOf(stepTypeStr);
        } catch (IllegalArgumentException e) {
            this.stepType = null;
        }
    }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        if ("IN_PROGRESS".equals(status) && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        } else if ("COMPLETED".equals(status) || "FAILED".equals(status) || "COMPENSATED".equals(status)) {
            this.completedAt = LocalDateTime.now();
            if (this.startedAt != null) {
                this.durationMs = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
            }
        }
    }

    public String getStepData() { return stepData; }
    public void setStepData(String stepData) { this.stepData = stepData; }

    public String getCompensationData() { return compensationData; }
    public void setCompensationData(String compensationData) { this.compensationData = compensationData; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getRetryDelayMinutes() { return retryDelayMinutes; }
    public void setRetryDelayMinutes(Integer retryDelayMinutes) { this.retryDelayMinutes = retryDelayMinutes; }

    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCompensatedAt() { return compensatedAt; }
    public void setCompensatedAt(LocalDateTime compensatedAt) { this.compensatedAt = compensatedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Boolean getIsCompensatable() { return isCompensatable; }
    public void setIsCompensatable(Boolean isCompensatable) { this.isCompensatable = isCompensatable; }

    public Long getDependsOnStepId() { return dependsOnStepId; }
    public void setDependsOnStepId(Long dependsOnStepId) { this.dependsOnStepId = dependsOnStepId; }

    // === Вспомогательные методы ===
    public void incrementRetryCount() {
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        this.retryCount++;
        this.nextRetryTime = LocalDateTime.now().plusMinutes(
                this.retryDelayMinutes != null ? this.retryDelayMinutes : 5
        );
    }

    public boolean canRetry() {
        if (this.retryCount == null) this.retryCount = 0;
        if (this.maxRetries == null) this.maxRetries = 3;
        if (this.status == null) this.status = "PENDING";

        return this.retryCount < this.maxRetries &&
                ("FAILED".equals(this.status) || "PENDING".equals(this.status));
    }

    public boolean shouldRetryNow() {
        if (!canRetry()) return false;
        if (this.nextRetryTime == null) return true;
        return LocalDateTime.now().isAfter(this.nextRetryTime);
    }

    public boolean isPending() { return "PENDING".equals(this.status); }
    public boolean isInProgress() { return "IN_PROGRESS".equals(this.status); }
    public boolean isCompleted() { return "COMPLETED".equals(this.status); }
    public boolean isFailed() { return "FAILED".equals(this.status); }
    public boolean isCompensated() { return "COMPENSATED".equals(this.status); }
    public boolean isSkipped() { return "SKIPPED".equals(this.status); }

    public void markAsCompensated() {
        this.status = "COMPENSATED";
        this.compensatedAt = LocalDateTime.now();
    }

    public void markAsSkipped() {
        this.status = "SKIPPED";
        this.completedAt = LocalDateTime.now();
    }
}