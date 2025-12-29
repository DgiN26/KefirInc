package com.kefir.logistics.launcher_service.controller;

import com.kefir.logistics.launcher_service.service.ServiceOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MainController {

    @Autowired
    private ApplicationContext context;

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "KEFIR Launcher Service");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("endpoints", Map.of(
                "root", "/",
                "health", "/health",
                "autoStart", "/autostart",
                "config", "/api/v1/config/info",
                "servicesStatus", "/api/v1/services/status",
                "startAll", "/api/v1/services/start-all (POST)",
                "listServices", "/api/v1/services/list"
        ));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "launcher-service");
        response.put("time", java.time.LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/autostart")
    public Map<String, Object> autoStart() {
        Map<String, Object> response = new HashMap<>();

        new Thread(() -> {
            try {
                // Ждем 1 секунду перед стартом
                Thread.sleep(1000);

                // Получаем ServiceOrchestrator из контекста Spring
                ServiceOrchestrator orchestrator = context.getBean(ServiceOrchestrator.class);

                // Запускаем все сервисы
                orchestrator.startAllServices();

                System.out.println("Auto-start of all services completed");

            } catch (Exception e) {
                System.err.println("Error in auto-start: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        response.put("status", "initiated");
        response.put("message", "Auto-start of all services has been initiated");
        response.put("checkStatus", "http://localhost:8099/api/v1/services/status");
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}