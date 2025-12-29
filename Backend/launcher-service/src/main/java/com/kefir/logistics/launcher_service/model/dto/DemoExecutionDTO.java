package com.kefir.logistics.launcher_service.model.dto;

import com.kefir.logistics.launcher_service.model.enums.DemoScenarioType;
import java.time.LocalDateTime;
import java.util.Map;

public class DemoExecutionDTO {
    private String executionId;
    private DemoScenarioType scenarioType;
    private String scenarioTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // STARTED, RUNNING, COMPLETED, FAILED, CANCELLED
    private String errorMessage;
    private int executionTimeSeconds;
    private Map<String, Object> parameters;
    private Map<String, Object> results;
    private Map<String, Object> metrics;
    private String executedBy; // Кто запустил
    private String sessionId; // ID сессии

    // Конструкторы
    public DemoExecutionDTO() {
        this.executionId = "DEMO_" + System.currentTimeMillis();
        this.startTime = LocalDateTime.now();
        this.status = "STARTED";
    }

    public DemoExecutionDTO(DemoScenarioType scenarioType, String scenarioTitle) {
        this();
        this.scenarioType = scenarioType;
        this.scenarioTitle = scenarioTitle;
    }

    // Геттеры и сеттеры
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public DemoScenarioType getScenarioType() { return scenarioType; }
    public void setScenarioType(DemoScenarioType scenarioType) { this.scenarioType = scenarioType; }

    public String getScenarioTitle() { return scenarioTitle; }
    public void setScenarioTitle(String scenarioTitle) { this.scenarioTitle = scenarioTitle; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getExecutionTimeSeconds() { return executionTimeSeconds; }
    public void setExecutionTimeSeconds(int executionTimeSeconds) { this.executionTimeSeconds = executionTimeSeconds; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public Map<String, Object> getResults() { return results; }
    public void setResults(Map<String, Object> results) { this.results = results; }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }

    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    // Вспомогательные методы
    public void complete(Map<String, Object> results) {
        this.endTime = LocalDateTime.now();
        this.status = "COMPLETED";
        this.results = results;
        calculateExecutionTime();
    }

    public void fail(String errorMessage) {
        this.endTime = LocalDateTime.now();
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        calculateExecutionTime();
    }

    private void calculateExecutionTime() {
        if (startTime != null && endTime != null) {
            this.executionTimeSeconds = (int) java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }
}