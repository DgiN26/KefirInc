package com.kefir.logistics.launcher_service.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "demo_executions")
public class DemoExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demo_scenario_id", nullable = false)
    private Long demoScenarioId;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED, PARTIAL

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "executed_by")
    private String executedBy;

    @Column(name = "metrics", columnDefinition = "TEXT")
    private String metrics; // JSON с метриками

    // Конструкторы
    public DemoExecutionEntity() {
        this.executionTime = LocalDateTime.now();
    }

    public DemoExecutionEntity(Long demoScenarioId, String status) {
        this();
        this.demoScenarioId = demoScenarioId;
        this.status = status;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDemoScenarioId() { return demoScenarioId; }
    public void setDemoScenarioId(Long demoScenarioId) { this.demoScenarioId = demoScenarioId; }

    public LocalDateTime getExecutionTime() { return executionTime; }
    public void setExecutionTime(LocalDateTime executionTime) { this.executionTime = executionTime; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }

    public String getMetrics() { return metrics; }
    public void setMetrics(String metrics) { this.metrics = metrics; }

    // Метод для расчета длительности
    public void calculateDuration(LocalDateTime endTime) {
        if (endTime != null && executionTime != null) {
            this.durationSeconds = Duration.between(executionTime, endTime).getSeconds();
        }
    }
}