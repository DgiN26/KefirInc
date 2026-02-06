package com.kefir.logistics.launcher_service.service;


import com.kefir.logistics.launcher_service.model.dto.ServiceStatusDTO;
import com.kefir.logistics.launcher_service.model.enums.ServiceState;
import com.kefir.logistics.launcher_service.model.enums.ServiceType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServiceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitor.class);

    @Autowired
    private ServiceOrchestrator serviceOrchestrator;

    private final Map<ServiceType, ServiceHealthInfo> healthCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        logger.info("Service Monitor initialized");

        // Инициализируем кэш для всех сервисов
        for (ServiceType serviceType : ServiceType.values()) {
            healthCache.put(serviceType, new ServiceHealthInfo());
        }
    }

    /**
     * Проверяет здоровье всех сервисов каждые 30 секунд
     */
    @Scheduled(fixedDelay = 30000)
    public void checkAllServicesHealth() {
        logger.debug("Running scheduled health check for all services");

        for (ServiceType serviceType : ServiceType.values()) {
            ServiceHealthInfo healthInfo = checkServiceHealth(serviceType);
            healthCache.put(serviceType, healthInfo);

            // Если сервис должен быть запущен, но не отвечает
            ServiceStatusDTO status = serviceOrchestrator.getServiceStatus(serviceType);
            if (status != null && status.getState().isRunning() && !healthInfo.isHealthy()) {
                logger.warn("Service {} is running but not responding (HTTP: {})",
                        serviceType.getDisplayName(), healthInfo.getLastHttpStatus());

                // Можно добавить автоматический restart:
                // if (healthInfo.getConsecutiveFailures() > 3) {
                //     logger.info("Auto-restarting service {} due to health failures",
                //         serviceType.getDisplayName());
                //     serviceOrchestrator.restartService(serviceType);
                // }
            }
        }
    }

    /**
     * Проверяет здоровье конкретного сервиса
     */
    public ServiceHealthInfo checkServiceHealth(ServiceType serviceType) {
        ServiceHealthInfo healthInfo = healthCache.getOrDefault(serviceType, new ServiceHealthInfo());
        healthInfo.setLastCheckTime(LocalDateTime.now());

        try {
            // Пробуем несколько эндпоинтов
            List<String> endpoints = Arrays.asList(
                    String.format("http://localhost:%d/actuator/health", serviceType.getPort()),
                    String.format("http://localhost:%d/actuator/info", serviceType.getPort()),
                    String.format("http://localhost:%d", serviceType.getPort())
            );

            boolean isHealthy = false;
            String response = null;
            int httpStatus = 0;

            for (String endpoint : endpoints) {
                try {
                    URL url = new URL(endpoint);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    httpStatus = connection.getResponseCode();

                    if (httpStatus >= 200 && httpStatus < 300) {
                        isHealthy = true;
                        response = "HTTP " + httpStatus;
                        break;
                    }

                    connection.disconnect();

                } catch (IOException e) {
                    // Пробуем следующий endpoint
                    continue;
                }
            }

            healthInfo.setHealthy(isHealthy);
            healthInfo.setLastHttpStatus(httpStatus);
            healthInfo.setLastResponse(response);

            if (isHealthy) {
                healthInfo.setConsecutiveFailures(0);
                healthInfo.setLastSuccessTime(LocalDateTime.now());
                logger.debug("Service {} is healthy (HTTP: {})",
                        serviceType.getDisplayName(), httpStatus);
            } else {
                healthInfo.incrementFailures();
                logger.warn("Service {} is unhealthy (HTTP: {})",
                        serviceType.getDisplayName(), httpStatus);
            }

        } catch (Exception e) {
            logger.error("Error checking health for {}: {}",
                    serviceType.getDisplayName(), e.getMessage());
            healthInfo.setHealthy(false);
            healthInfo.setLastResponse("Error: " + e.getMessage());
            healthInfo.incrementFailures();
        }

        return healthInfo;
    }

    /**
     * Получает статус здоровья для всех сервисов
     */
    public Map<ServiceType, ServiceHealthInfo> getAllServicesHealth() {
        Map<ServiceType, ServiceHealthInfo> result = new HashMap<>();

        for (ServiceType serviceType : ServiceType.values()) {
            ServiceHealthInfo healthInfo = healthCache.get(serviceType);
            if (healthInfo == null ||
                    healthInfo.getLastCheckTime() == null ||
                    healthInfo.getLastCheckTime().isBefore(LocalDateTime.now().minusMinutes(1))) {

                // Если данные устарели, обновляем
                healthInfo = checkServiceHealth(serviceType);
            }
            result.put(serviceType, healthInfo);
        }

        return result;
    }

    /**
     * Получает агрегированную статистику
     */
    public Map<String, Object> getHealthSummary() {
        Map<ServiceType, ServiceHealthInfo> healthData = getAllServicesHealth();

        int totalServices = ServiceType.values().length;
        int healthyServices = 0;
        int unhealthyServices = 0;
        int unknownServices = 0;

        List<Map<String, Object>> detailedStatus = new ArrayList<>();

        for (Map.Entry<ServiceType, ServiceHealthInfo> entry : healthData.entrySet()) {
            ServiceHealthInfo healthInfo = entry.getValue();

            if (healthInfo.isHealthy()) {
                healthyServices++;
            } else if (healthInfo.getConsecutiveFailures() > 0) {
                unhealthyServices++;
            } else {
                unknownServices++;
            }

            Map<String, Object> serviceStatus = new HashMap<>();
            serviceStatus.put("service", entry.getKey().getDisplayName());
            serviceStatus.put("healthy", healthInfo.isHealthy());
            serviceStatus.put("lastCheck", healthInfo.getLastCheckTime());
            serviceStatus.put("responseTime", healthInfo.getResponseTime());
            serviceStatus.put("consecutiveFailures", healthInfo.getConsecutiveFailures());

            detailedStatus.add(serviceStatus);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("timestamp", LocalDateTime.now());
        summary.put("totalServices", totalServices);
        summary.put("healthyServices", healthyServices);
        summary.put("unhealthyServices", unhealthyServices);
        summary.put("unknownServices", unknownServices);
        summary.put("healthPercentage", totalServices > 0 ?
                (healthyServices * 100.0 / totalServices) : 0);
        summary.put("detailedStatus", detailedStatus);

        return summary;
    }

    /**
     * Внутренний класс для хранения информации о здоровье сервиса
     */
    public static class ServiceHealthInfo {
        private boolean healthy = false;
        private LocalDateTime lastCheckTime;
        private LocalDateTime lastSuccessTime;
        private int consecutiveFailures = 0;
        private int lastHttpStatus = 0;
        private String lastResponse;
        private long responseTime; // в миллисекундах

        // Геттеры и сеттеры
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }

        public LocalDateTime getLastCheckTime() { return lastCheckTime; }
        public void setLastCheckTime(LocalDateTime lastCheckTime) { this.lastCheckTime = lastCheckTime; }

        public LocalDateTime getLastSuccessTime() { return lastSuccessTime; }
        public void setLastSuccessTime(LocalDateTime lastSuccessTime) { this.lastSuccessTime = lastSuccessTime; }

        public int getConsecutiveFailures() { return consecutiveFailures; }
        public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
        public void incrementFailures() { this.consecutiveFailures++; }

        public int getLastHttpStatus() { return lastHttpStatus; }
        public void setLastHttpStatus(int lastHttpStatus) { this.lastHttpStatus = lastHttpStatus; }

        public String getLastResponse() { return lastResponse; }
        public void setLastResponse(String lastResponse) { this.lastResponse = lastResponse; }

        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    }
}