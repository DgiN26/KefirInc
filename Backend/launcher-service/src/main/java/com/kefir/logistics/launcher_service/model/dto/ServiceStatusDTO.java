package com.kefir.logistics.launcher_service.model.dto;

import com.kefir.logistics.launcher_service.model.enums.ServiceState;
import com.kefir.logistics.launcher_service.model.enums.ServiceType;

import java.time.LocalDateTime;
import java.util.Map;

public class ServiceStatusDTO {
    private ServiceType serviceType;
    private ServiceState state;
    private Integer pid;
    private LocalDateTime startedAt;
    private LocalDateTime lastChecked;
    private String healthUrl;
    private String logPath;
    private String errorMessage;
    private Map<String, Object> metrics;
    private String version;

    private boolean portOpen;
    private int startupTimeSeconds;
    private int restartCount;
    private String lastError;
    private LocalDateTime lastErrorTime;

    private String missionRole;
    private boolean requiredForMission;  // <-- БЫЛО ДУБЛИРОВАНИЕ
    private boolean isHealthy;
    private int port;
    private String serviceName;
    private LocalDateTime lastHealthCheck;
    private boolean isManaged = true;

    // Конструкторы
    public ServiceStatusDTO() {}

    public ServiceStatusDTO(ServiceType serviceType, ServiceState state) {
        this.serviceType = serviceType;
        this.state = state;
        this.startedAt = LocalDateTime.now();
        this.lastChecked = LocalDateTime.now();
        this.restartCount = 0;
        this.lastHealthCheck = LocalDateTime.now();
        this.isManaged = (state != ServiceState.EXTERNAL);
    }

    // Геттеры и сеттеры
    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public ServiceState getState() { return state; }
    public void setState(ServiceState state) {
        this.state = state;
        if (state == ServiceState.EXTERNAL) {
            this.isManaged = false;
        }
    }

    public Integer getPid() { return pid; }
    public void setPid(Integer pid) { this.pid = pid; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getLastChecked() { return lastChecked; }
    public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }

    public String getHealthUrl() { return healthUrl; }
    public void setHealthUrl(String healthUrl) { this.healthUrl = healthUrl; }

    public String getLogPath() { return logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public boolean isPortOpen() { return portOpen; }
    public void setPortOpen(boolean portOpen) { this.portOpen = portOpen; }

    public int getStartupTimeSeconds() { return startupTimeSeconds; }
    public void setStartupTimeSeconds(int startupTimeSeconds) { this.startupTimeSeconds = startupTimeSeconds; }

    public int getRestartCount() { return restartCount; }
    public void setRestartCount(int restartCount) { this.restartCount = restartCount; }
    public void incrementRestartCount() { this.restartCount++; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) {
        this.lastError = lastError;
        this.lastErrorTime = LocalDateTime.now();
    }

    public LocalDateTime getLastErrorTime() { return lastErrorTime; }
    public void setLastErrorTime(LocalDateTime lastErrorTime) { this.lastErrorTime = lastErrorTime; }

    // НОВЫЕ МЕТОДЫ (БЕЗ ДУБЛИРОВАНИЯ)

    public String getMissionRole() { return missionRole; }
    public void setMissionRole(String missionRole) { this.missionRole = missionRole; }

    // ТОЛЬКО ОДИН МЕТОД isRequiredForMission() - убрать дублирование
    public boolean isRequiredForMission() {
        return requiredForMission;
    }

    public void setRequiredForMission(boolean requiredForMission) {
        this.requiredForMission = requiredForMission;
    }

    public boolean isHealthy() {
        if (state == null) {
            return false;
        }
        if (state == ServiceState.EXTERNAL) {
            return portOpen;
        }
        return state.isRunning() && portOpen;
    }

    public void setHealthy(boolean healthy) {
        this.isHealthy = healthy;
    }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getServiceName() {
        if (serviceName != null) {
            return serviceName;
        }
        return serviceType != null ? serviceType.getDisplayName() : "Unknown";
    }

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public LocalDateTime getLastHealthCheck() { return lastHealthCheck; }
    public void setLastHealthCheck(LocalDateTime lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }

    public boolean isManaged() { return isManaged; }
    public void setManaged(boolean managed) { this.isManaged = managed; }

    // Вспомогательные методы
    public void updateHealthCheck(boolean portOpen, String errorMessage) {
        this.lastChecked = LocalDateTime.now();
        this.lastHealthCheck = LocalDateTime.now();
        this.portOpen = portOpen;

        if (errorMessage != null) {
            this.errorMessage = errorMessage;
            this.lastError = errorMessage;
            this.lastErrorTime = LocalDateTime.now();
            this.isHealthy = false;
        } else {
            this.isHealthy = true && portOpen;
        }
    }

    public boolean isRunning() {
        return state != null && state.isRunning();
    }

    public boolean isExternal() {
        return state == ServiceState.EXTERNAL;
    }

    // Builder pattern
    public static ServiceStatusDTOBuilder builder() {
        return new ServiceStatusDTOBuilder();
    }

    public static class ServiceStatusDTOBuilder {
        private ServiceType serviceType;
        private ServiceState state;
        private Integer pid;
        private LocalDateTime startedAt;
        private LocalDateTime lastChecked;
        private String healthUrl;
        private String logPath;
        private String errorMessage;
        private Map<String, Object> metrics;
        private String version;
        private String missionRole;
        private boolean requiredForMission;
        private int port;
        private String serviceName;
        private boolean portOpen;
        private boolean isManaged = true;

        public ServiceStatusDTOBuilder serviceType(ServiceType serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public ServiceStatusDTOBuilder state(ServiceState state) {
            this.state = state;
            if (state == ServiceState.EXTERNAL) {
                this.isManaged = false;
            }
            return this;
        }

        public ServiceStatusDTOBuilder pid(Integer pid) {
            this.pid = pid;
            return this;
        }

        public ServiceStatusDTOBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ServiceStatusDTOBuilder lastChecked(LocalDateTime lastChecked) {
            this.lastChecked = lastChecked;
            return this;
        }

        public ServiceStatusDTOBuilder healthUrl(String healthUrl) {
            this.healthUrl = healthUrl;
            return this;
        }

        public ServiceStatusDTOBuilder logPath(String logPath) {
            this.logPath = logPath;
            return this;
        }

        public ServiceStatusDTOBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ServiceStatusDTOBuilder metrics(Map<String, Object> metrics) {
            this.metrics = metrics;
            return this;
        }

        public ServiceStatusDTOBuilder version(String version) {
            this.version = version;
            return this;
        }

        public ServiceStatusDTOBuilder missionRole(String missionRole) {
            this.missionRole = missionRole;
            return this;
        }

        public ServiceStatusDTOBuilder requiredForMission(boolean requiredForMission) {
            this.requiredForMission = requiredForMission;
            return this;
        }

        public ServiceStatusDTOBuilder port(int port) {
            this.port = port;
            return this;
        }

        public ServiceStatusDTOBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public ServiceStatusDTOBuilder portOpen(boolean portOpen) {
            this.portOpen = portOpen;
            return this;
        }

        public ServiceStatusDTOBuilder managed(boolean isManaged) {
            this.isManaged = isManaged;
            return this;
        }

        public ServiceStatusDTO build() {
            ServiceStatusDTO dto = new ServiceStatusDTO();
            dto.setServiceType(serviceType);
            dto.setState(state);
            dto.setPid(pid);
            dto.setStartedAt(startedAt);
            dto.setLastChecked(lastChecked);
            dto.setHealthUrl(healthUrl);
            dto.setLogPath(logPath);
            dto.setErrorMessage(errorMessage);
            dto.setMetrics(metrics);
            dto.setVersion(version);
            dto.setMissionRole(missionRole);
            dto.setRequiredForMission(requiredForMission);
            dto.setPort(port);
            dto.setServiceName(serviceName);
            dto.setPortOpen(portOpen);
            dto.setManaged(isManaged);
            return dto;
        }
    }

    @Override
    public String toString() {
        return String.format("ServiceStatusDTO{service=%s, state=%s, port=%d, mission='%s', required=%s, managed=%s, healthy=%s}",
                getServiceName(),
                state != null ? state.getDescription() : "null",
                port,
                missionRole != null ? missionRole : "null",
                requiredForMission,
                isManaged,
                isHealthy());
    }
}