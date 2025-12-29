package com.kefir.logistics.launcher_service.model.dto;

import com.kefir.logistics.launcher_service.model.enums.DemoScenarioType;
import com.kefir.logistics.launcher_service.model.enums.ErrorType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DemoScenarioDTO {
    private DemoScenarioType scenarioType;
    private String title;
    private String description;
    private List<String> steps;
    private List<ErrorType> simulatedErrors;
    private Map<String, Object> testData;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean running;
    private Map<String, Object> results;

    // ДОБАВИТЬ:
    private String operationId;           // ID операции
    private String status;                // Статус выполнения
    private String errorMessage;          // Сообщение об ошибке
    private int executionTimeSeconds;     // Время выполнения в секундах
    private List<String> executedServices; // Какие сервисы были запущены
    private Map<String, Object> metrics;  // Метрики выполнения

    // Конструкторы
    public DemoScenarioDTO() {}

    public DemoScenarioDTO(DemoScenarioType scenarioType, String title) {
        this.scenarioType = scenarioType;
        this.title = title;
        this.startTime = LocalDateTime.now();
        this.running = true;
        this.status = "STARTED";
    }

    // Геттеры и сеттеры для существующих полей
    public DemoScenarioType getScenarioType() { return scenarioType; }
    public void setScenarioType(DemoScenarioType scenarioType) { this.scenarioType = scenarioType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public List<ErrorType> getSimulatedErrors() { return simulatedErrors; }
    public void setSimulatedErrors(List<ErrorType> simulatedErrors) { this.simulatedErrors = simulatedErrors; }

    public Map<String, Object> getTestData() { return testData; }
    public void setTestData(Map<String, Object> testData) { this.testData = testData; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }

    public Map<String, Object> getResults() { return results; }
    public void setResults(Map<String, Object> results) { this.results = results; }

    // ДОБАВИТЬ геттеры и сеттеры для новых полей:
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getExecutionTimeSeconds() { return executionTimeSeconds; }
    public void setExecutionTimeSeconds(int executionTimeSeconds) { this.executionTimeSeconds = executionTimeSeconds; }

    public List<String> getExecutedServices() { return executedServices; }
    public void setExecutedServices(List<String> executedServices) { this.executedServices = executedServices; }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }

    // Вспомогательные методы
    public void complete() {
        this.endTime = LocalDateTime.now();
        this.running = false;
        this.status = "COMPLETED";

        if (this.startTime != null && this.endTime != null) {
            this.executionTimeSeconds = (int) java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }

    public void fail(String errorMessage) {
        this.endTime = LocalDateTime.now();
        this.running = false;
        this.status = "FAILED";
        this.errorMessage = errorMessage;
    }
}