package com.kefir.logistics.launcher_service.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demo_scenarios")
public class DemoScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_type", nullable = false)
    private String scenarioType;

    @Column(name = "scenario_name", nullable = false)
    private String scenarioName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status")
    private String status; // CREATED, RUNNING, COMPLETED, FAILED

    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData; // JSON с результатами

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Конструкторы
    public DemoScenarioEntity() {
        this.createdAt = LocalDateTime.now();
        this.status = "CREATED";
    }

    public DemoScenarioEntity(String scenarioType, String scenarioName) {
        this();
        this.scenarioType = scenarioType;
        this.scenarioName = scenarioName;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResultData() { return resultData; }
    public void setResultData(String resultData) { this.resultData = resultData; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}