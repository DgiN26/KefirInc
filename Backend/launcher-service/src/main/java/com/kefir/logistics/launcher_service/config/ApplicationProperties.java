package com.kefir.logistics.launcher_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "launcher")
@Validated
public class ApplicationProperties {

    @NotEmpty
    private String baseDir = "../";

    @NotEmpty
    private String logsDir = "./logs";

    @Min(1000)
    private int startupDelayMs = 5000;

    @Min(5)
    private int healthCheckTimeoutSec = 30;

    private boolean autoRestart = true;

    private Map<String, ServiceConfig> services;

    // Конструктор по умолчанию
    public ApplicationProperties() {
    }

    // Геттеры и сеттеры
    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getLogsDir() {
        return logsDir;
    }

    public void setLogsDir(String logsDir) {
        this.logsDir = logsDir;
    }

    public int getStartupDelayMs() {
        return startupDelayMs;
    }

    public void setStartupDelayMs(int startupDelayMs) {
        this.startupDelayMs = startupDelayMs;
    }

    public int getHealthCheckTimeoutSec() {
        return healthCheckTimeoutSec;
    }

    public void setHealthCheckTimeoutSec(int healthCheckTimeoutSec) {
        this.healthCheckTimeoutSec = healthCheckTimeoutSec;
    }

    public boolean isAutoRestart() {
        return autoRestart;
    }

    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }

    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services;
    }

    public ServiceConfig getServiceConfig(String serviceId) {
        return services != null ? services.get(serviceId) : null;
    }

    public static class ServiceConfig {

        private String id;

        @NotEmpty
        private String directory;

        @Min(1024)
        private int port;

        @NotEmpty
        private String displayName;

        private String healthEndpoint = "/actuator/health";

        public ServiceConfig() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getHealthEndpoint() {
            return healthEndpoint;
        }

        public void setHealthEndpoint(String healthEndpoint) {
            this.healthEndpoint = healthEndpoint;
        }

        @Override
        public String toString() {
            return String.format("ServiceConfig{id='%s', directory='%s', port=%d, displayName='%s'}",
                    id, directory, port, displayName);
        }
    }

    @Override
    public String toString() {
        return String.format("ApplicationProperties{baseDir='%s', logsDir='%s', servicesCount=%d}",
                baseDir, logsDir, services != null ? services.size() : 0);
    }
}