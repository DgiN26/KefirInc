package com.kefir.logistics.launcher_service.controller;

import com.kefir.logistics.launcher_service.service.ServiceOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private ServiceOrchestrator serviceOrchestrator;

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "KEFIR Launcher Service");
        response.put("version", "2.0.0");
        response.put("status", "running");
        response.put("mission", "Управление всей системой KEFIR (бекенд + фронтенд)");
        response.put("endpoints", Map.of(
                "root", "/",
                "health", "/health",
                "autoStart", "/autostart",
                "config", "/api/v1/config/info",
                "startCompleteSystem", "/api/v1/services/start-complete (POST)",
                "systemStatus", "/api/v1/services/system-status",
                "stopSystem", "/api/v1/services/stop-all (POST)",
                "listServices", "/api/v1/services/list"
        ));
        response.put("frontend", "http://localhost:3000 (после запуска)");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "launcher-service");
        response.put("version", "2.0.0");
        response.put("time", java.time.LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/autostart")
    public Map<String, Object> autoStart() {
        logger.info("🔧 Запрос автозапуска системы");

        Map<String, Object> response = new HashMap<>();

        new Thread(() -> {
            try {
                // Ждем 1 секунду перед стартом
                Thread.sleep(1000);

                logger.info("🚀 Начинаю автозапуск всей системы KEFIR...");
                Map<String, Object> result = serviceOrchestrator.startCompleteSystem();

                logger.info("✅ Автозапуск завершен: {}", result.get("status"));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("❌ Автозапуск прерван");
            } catch (Exception e) {
                logger.error("❌ Ошибка автозапуска: {}", e.getMessage());
            }
        }).start();

        response.put("status", "initiated");
        response.put("message", "Автозапуск всей системы KEFIR инициирован");
        response.put("note", "Система запускается в фоновом режиме");
        response.put("checkStatus", "GET http://localhost:8099/api/v1/services/system-status");
        response.put("openFrontend", "http://localhost:3000 (через 30 секунд)");
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong from KEFIR Launcher v2.0";
    }
}