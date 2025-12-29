package com.kefir.logistics.launcher_service.model.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

public class SystemHealthDTO {
    private String systemName;
    private String overallStatus; // UP, DOWN, DEGRADED
    private LocalDateTime checkTime;
    private Map<String, ServiceHealthDTO> services;
    private List<String> warnings;
    private List<String> errors;
    private SystemMetricsDTO metrics;

    // Конструкторы
    public SystemHealthDTO() {
        this.systemName = "KEFIR Logistics System";
        this.checkTime = LocalDateTime.now();
        this.overallStatus = "UP";
    }

    // Геттеры и сеттеры
    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }

    public Map<String, ServiceHealthDTO> getServices() { return services; }
    public void setServices(Map<String, ServiceHealthDTO> services) { this.services = services; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public SystemMetricsDTO getMetrics() { return metrics; }
    public void setMetrics(SystemMetricsDTO metrics) { this.metrics = metrics; }
}

class ServiceHealthDTO {
    private String serviceName;
    private String status; // UP, DOWN, UNKNOWN
    private int responseTimeMs;
    private LocalDateTime lastCheck;
    private String healthEndpoint;
    private Map<String, Object> details;

    // Конструкторы
    public ServiceHealthDTO() {}

    public ServiceHealthDTO(String serviceName, String status) {
        this.serviceName = serviceName;
        this.status = status;
        this.lastCheck = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(int responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public LocalDateTime getLastCheck() { return lastCheck; }
    public void setLastCheck(LocalDateTime lastCheck) { this.lastCheck = lastCheck; }

    public String getHealthEndpoint() { return healthEndpoint; }
    public void setHealthEndpoint(String healthEndpoint) { this.healthEndpoint = healthEndpoint; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}

class SystemMetricsDTO {
    private int totalServices;
    private int healthyServices;
    private int unhealthyServices;
    private double healthPercentage;
    private long totalMemoryMB;
    private long usedMemoryMB;
    private double memoryUsagePercentage;
    private int cpuUsagePercentage;
    private int activeThreads;
    private int totalTransactions;
    private int failedTransactions;
    private double successRate;

    // Геттеры и сеттеры
    public int getTotalServices() { return totalServices; }
    public void setTotalServices(int totalServices) { this.totalServices = totalServices; }

    public int getHealthyServices() { return healthyServices; }
    public void setHealthyServices(int healthyServices) { this.healthyServices = healthyServices; }

    public int getUnhealthyServices() { return unhealthyServices; }
    public void setUnhealthyServices(int unhealthyServices) { this.unhealthyServices = unhealthyServices; }

    public double getHealthPercentage() { return healthPercentage; }
    public void setHealthPercentage(double healthPercentage) { this.healthPercentage = healthPercentage; }

    public long getTotalMemoryMB() { return totalMemoryMB; }
    public void setTotalMemoryMB(long totalMemoryMB) { this.totalMemoryMB = totalMemoryMB; }

    public long getUsedMemoryMB() { return usedMemoryMB; }
    public void setUsedMemoryMB(long usedMemoryMB) { this.usedMemoryMB = usedMemoryMB; }

    public double getMemoryUsagePercentage() { return memoryUsagePercentage; }
    public void setMemoryUsagePercentage(double memoryUsagePercentage) { this.memoryUsagePercentage = memoryUsagePercentage; }

    public int getCpuUsagePercentage() { return cpuUsagePercentage; }
    public void setCpuUsagePercentage(int cpuUsagePercentage) { this.cpuUsagePercentage = cpuUsagePercentage; }

    public int getActiveThreads() { return activeThreads; }
    public void setActiveThreads(int activeThreads) { this.activeThreads = activeThreads; }

    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }

    public int getFailedTransactions() { return failedTransactions; }
    public void setFailedTransactions(int failedTransactions) { this.failedTransactions = failedTransactions; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
}