package com.example.TransactionSaga.model.entity;

import com.example.TransactionSaga.model.converter.MapToStringConverter;
import com.example.TransactionSaga.model.converter.ObjectToStringConverter;
import com.example.TransactionSaga.model.enums.TransactionStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.example.TransactionSaga.model.enums.TransactionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "collector_id", nullable = false)
    private String collectorId;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.CREATED;

    @Convert(converter = MapToStringConverter.class)
    @Column(name = "scanned_items", columnDefinition = "jsonb")
    private Map<String, Integer> scannedItems = new HashMap<>();

    @Convert(converter = MapToStringConverter.class)
    @Column(name = "missing_items", columnDefinition = "jsonb")
    private Map<String, Integer> missingItems = new HashMap<>();

    @Convert(converter = ObjectToStringConverter.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "pause_reason", length = 1000)
    private String pauseReason;

    @Column(name = "client_decision", length = 50)
    private String clientDecision;

    @Column(name = "office_notes", length = 2000)
    private String officeNotes;

    @Column(name = "office_problem_id")
    private Long officeProblemId;

    @Column(name = "timeout_minutes")
    private Integer timeoutMinutes = 1440;

    @Column(name = "estimated_completion_time")
    private LocalDateTime estimatedCompletionTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "client_notified_at")
    private LocalDateTime clientNotifiedAt;

    @Column(name = "client_responded_at")
    private LocalDateTime clientRespondedAt;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Конструкторы
    public TransactionEntity() {}

    public TransactionEntity(String orderId, String collectorId, String clientId) {
        this.orderId = orderId;
        this.collectorId = collectorId;
        this.clientId = clientId;
        this.startedAt = LocalDateTime.now();
        this.estimatedCompletionTime = LocalDateTime.now().plusMinutes(30);
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Integer> getScannedItems() { return scannedItems; }
    public void setScannedItems(Map<String, Integer> scannedItems) {
        this.scannedItems = scannedItems != null ? scannedItems : new HashMap<>();
    }

    public Map<String, Integer> getMissingItems() { return missingItems; }
    public void setMissingItems(Map<String, Integer> missingItems) {
        this.missingItems = missingItems != null ? missingItems : new HashMap<>();
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public String getPauseReason() { return pauseReason; }
    public void setPauseReason(String pauseReason) { this.pauseReason = pauseReason; }

    public String getClientDecision() { return clientDecision; }
    public void setClientDecision(String clientDecision) { this.clientDecision = clientDecision; }

    public String getOfficeNotes() { return officeNotes; }
    public void setOfficeNotes(String officeNotes) { this.officeNotes = officeNotes; }

    public Long getOfficeProblemId() { return officeProblemId; }
    public void setOfficeProblemId(Long officeProblemId) { this.officeProblemId = officeProblemId; }

    public Integer getTimeoutMinutes() { return timeoutMinutes; }
    public void setTimeoutMinutes(Integer timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }

    public LocalDateTime getEstimatedCompletionTime() { return estimatedCompletionTime; }
    public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }

    public LocalDateTime getClientNotifiedAt() { return clientNotifiedAt; }
    public void setClientNotifiedAt(LocalDateTime clientNotifiedAt) { this.clientNotifiedAt = clientNotifiedAt; }

    public LocalDateTime getClientRespondedAt() { return clientRespondedAt; }
    public void setClientRespondedAt(LocalDateTime clientRespondedAt) { this.clientRespondedAt = clientRespondedAt; }

    public LocalDateTime getResumedAt() { return resumedAt; }
    public void setResumedAt(LocalDateTime resumedAt) { this.resumedAt = resumedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    // Вспомогательные методы
    public boolean isActive() { return status == TransactionStatus.ACTIVE; }
    public boolean isPaused() { return status == TransactionStatus.PAUSED; }
    public boolean isWaitingForClient() { return status == TransactionStatus.WAITING_CLIENT; }
    public boolean isWaitingForOffice() { return status == TransactionStatus.WAITING_OFFICE; }
    public boolean isCompensating() { return status == TransactionStatus.COMPENSATING; }
    public boolean isCompleted() { return status == TransactionStatus.COMPLETED; }
    public boolean isCancelled() { return status == TransactionStatus.CANCELLED; }
    public boolean isTimeout() { return status == TransactionStatus.TIMEOUT; }

    public void addScannedItem(String productId, int quantity) {
        this.scannedItems.merge(productId, quantity, Integer::sum);
        this.updatedAt = LocalDateTime.now();
    }

    public void addMissingItem(String productId, int quantity) {
        this.missingItems.put(productId, quantity);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeMissingItem(String productId) {
        this.missingItems.remove(productId);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasMissingItems() {
        return !this.missingItems.isEmpty();
    }

    public int getTotalScannedQuantity() {
        return this.scannedItems.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalMissingQuantity() {
        return this.missingItems.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }

    public boolean isTimedOut() {
        if (this.clientNotifiedAt == null) return false;
        LocalDateTime timeoutThreshold = this.clientNotifiedAt.plusMinutes(this.timeoutMinutes);
        return LocalDateTime.now().isAfter(timeoutThreshold);
    }

    public long getRemainingTimeoutMinutes() {
        if (this.clientNotifiedAt == null) return this.timeoutMinutes;
        LocalDateTime timeoutTime = this.clientNotifiedAt.plusMinutes(this.timeoutMinutes);
        return java.time.Duration.between(LocalDateTime.now(), timeoutTime).toMinutes();
    }
}