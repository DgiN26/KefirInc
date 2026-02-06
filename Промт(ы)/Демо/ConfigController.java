package com.kefir.logistics.launcher_service.controller;

import com.kefir.logistics.launcher_service.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Autowired
    private ApplicationProperties appProperties;

    @GetMapping("/info")
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> response = new HashMap<>();

        response.put("status", "OK");
        response.put("service", "Launcher Service Config");

        if (appProperties != null) {
            response.put("baseDir", appProperties.getBaseDir());
            response.put("logsDir", appProperties.getLogsDir());
            response.put("startupDelayMs", appProperties.getStartupDelayMs());
            response.put("healthCheckTimeoutSec", appProperties.getHealthCheckTimeoutSec());
            response.put("autoRestart", appProperties.isAutoRestart());

            if (appProperties.getServices() != null) {
                response.put("servicesCount", appProperties.getServices().size());
                response.put("services", appProperties.getServices());
            } else {
                response.put("servicesCount", 0);
                response.put("services", "No services configured");
            }
        } else {
            response.put("error", "ApplicationProperties not autowired");
        }

        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    @GetMapping("/check")
    public Map<String, String> checkConfig() {
        Map<String, String> response = new HashMap<>();

        if (appProperties == null) {
            response.put("status", "ERROR");
            response.put("message", "Configuration not loaded");
        } else if (appProperties.getServices() == null || appProperties.getServices().isEmpty()) {
            response.put("status", "WARNING");
            response.put("message", "Configuration loaded but no services defined");
        } else {
            response.put("status", "OK");
            response.put("message", "Configuration loaded successfully");
            response.put("servicesCount", String.valueOf(appProperties.getServices().size()));
        }

        return response;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "pong");
        response.put("service", "launcher-service");
        response.put("time", java.time.LocalDateTime.now().toString());
        return response;
    }
}