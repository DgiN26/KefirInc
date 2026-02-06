package com.kefir.logistics.launcher_service.controller;

import com.kefir.logistics.launcher_service.model.dto.ServiceStatusDTO;
import com.kefir.logistics.launcher_service.model.enums.ServiceType;
import com.kefir.logistics.launcher_service.service.ServiceOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Service Management", description = "Управление системой KEFIR (бекенд + фронтенд)")
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
    private final ServiceOrchestrator serviceOrchestrator;

    @Autowired
    public ServiceController(ServiceOrchestrator serviceOrchestrator) {
        this.serviceOrchestrator = serviceOrchestrator;
    }

    @GetMapping("/health")
    @Operation(summary = "Проверить здоровье контроллера")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "KEFIR Launcher Service Controller");
        response.put("version", "2.0.0");
        response.put("mission", "Управление всей системой KEFIR (бекенд + фронтенд)");
        response.put("capabilities", Arrays.asList(
                "Запуск всей системы одним запросом",
                "Остановка всей системы",
                "Мониторинг статуса",
                "Управление отдельными сервисами"
        ));
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start-complete")
    @Operation(summary = "ЗАПУСТИТЬ ВСЮ СИСТЕМУ KEFIR (основной метод)")
    public ResponseEntity<Map<String, Object>> startCompleteSystem() {
        logger.info("🚀🚀🚀 API ВЫЗОВ: Запуск всей системы KEFIR");

        try {
            Map<String, Object> result = serviceOrchestrator.startCompleteSystem();
            logger.info("✅ Запуск системы завершен: {}", result.get("status"));
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ КРИТИЧЕСКАЯ ОШИБКА запуска системы: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "SYSTEM_START_FAILED");
            errorResponse.put("error", "Не удалось запустить систему KEFIR");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("recommendation", "Проверьте: 1) Директории сервисов 2) Свободные порты 3) Установлен ли Maven и Node.js");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/start-all")
    @Operation(summary = "Запустить всю систему (старое API для совместимости)")
    public ResponseEntity<Map<String, Object>> startAllServices() {
        logger.info("🚀 API ВЫЗОВ: Запуск системы (legacy endpoint)");

        try {
            Map<String, Object> result = serviceOrchestrator.startCompleteSystem();

            // Форматируем для обратной совместимости
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Вся система KEFIR запущена");
            response.put("status", "SYSTEM_STARTED");
            response.put("systemResult", result);
            response.put("backendServices", 7); // ApiGateway, Auth, User, Sklad, Collector, Backet, Office
            response.put("frontend", "React на порту 3000");
            response.put("frontendUrl", "http://localhost:3000");
            response.put("timestamp", System.currentTimeMillis());
            response.put("checkStatus", "GET /api/v1/services/system-status");

            logger.info("✅ Система запущена через legacy endpoint");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Ошибка: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Ошибка запуска системы");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/system-status")
    @Operation(summary = "Получить статус всей системы KEFIR")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        logger.info("🔍 API ВЫЗОВ: Получение статуса системы");

        try {
            Map<String, Object> status = serviceOrchestrator.getSystemStatus();
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("❌ Ошибка получения статуса: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "STATUS_CHECK_FAILED");
            errorResponse.put("error", "Не удалось получить статус системы");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/stop-all")
    @Operation(summary = "ОСТАНОВИТЬ ВСЮ СИСТЕМУ KEFIR")
    public ResponseEntity<Map<String, Object>> stopAllServices() {
        logger.info("🛑 API ВЫЗОВ: Остановка всей системы");

        try {
            Map<String, Object> result = serviceOrchestrator.stopCompleteSystem();
            logger.info("✅ Система остановлена: {}", result.get("status"));
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Ошибка остановки системы: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "SYSTEM_STOP_FAILED");
            errorResponse.put("error", "Не удалось остановить систему");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/restart-system")
    @Operation(summary = "ПЕРЕЗАПУСТИТЬ ВСЮ СИСТЕМУ")
    public ResponseEntity<Map<String, Object>> restartSystem() {
        logger.info("🔄 API ВЫЗОВ: Перезапуск всей системы");

        try {
            // 1. Останавливаем систему
            Map<String, Object> stopResult = serviceOrchestrator.stopCompleteSystem();
            logger.info("✅ Система остановлена для перезапуска");

            // 2. Ждем 5 секунд
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 3. Запускаем заново
            Map<String, Object> startResult = serviceOrchestrator.startCompleteSystem();

            // 4. Формируем общий ответ
            Map<String, Object> response = new HashMap<>();
            response.put("operation", "system_restart");
            response.put("stopResult", stopResult);
            response.put("startResult", startResult);
            response.put("status", "SYSTEM_RESTARTED");
            response.put("message", "Система успешно перезапущена");
            response.put("timestamp", System.currentTimeMillis());

            logger.info("✅ Система перезапущена успешно");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Ошибка перезапуска системы: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "SYSTEM_RESTART_FAILED");
            errorResponse.put("error", "Не удалось перезапустить систему");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "Список всех сервисов системы")
    public ResponseEntity<Map<String, Object>> listAllServices() {
        logger.info("📋 API ВЫЗОВ: Получение списка сервисов");

        Map<String, Object> response = new HashMap<>();

        // Бекенд сервисы
        List<Map<String, Object>> backendServices = new ArrayList<>();
        String[] services = {"ApiGateway", "Auth", "User", "Sklad", "Collector", "Backet", "Office"};
        int[] ports = {8080, 8097, 8081, 8082, 8084, 8083, 8085};

        for (int i = 0; i < services.length; i++) {
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("name", services[i]);
            serviceInfo.put("port", ports[i]);
            serviceInfo.put("type", "backend");
            serviceInfo.put("technology", "Spring Boot (Maven)");
            serviceInfo.put("startCommand", String.format("mvn spring-boot:run -Dserver.port=%d", ports[i]));
            backendServices.add(serviceInfo);
        }

        // Фронтенд
        Map<String, Object> frontendInfo = new HashMap<>();
        frontendInfo.put("name", "kefir-react-app");
        frontendInfo.put("port", 3000);
        frontendInfo.put("type", "frontend");
        frontendInfo.put("technology", "React (Node.js)");
        frontendInfo.put("startCommand", "npm start");

        response.put("system", "KEFIR Logistics Platform");
        response.put("version", "1.0.0");
        response.put("backendServices", backendServices);
        response.put("frontend", frontendInfo);
        response.put("totalServices", services.length + 1);
        response.put("description", "Полная система управления логистикой с микросервисной архитектурой");
        response.put("launcher", "Управляется через Launcher Service (порт 8099)");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/start/{serviceId}")
    @Operation(summary = "Запустить конкретный сервис")
    public ResponseEntity<ServiceStatusDTO> startService(@PathVariable String serviceId) {
        logger.info("🚀 API ВЫЗОВ: Запуск сервиса {}", serviceId);

        try {
            ServiceType serviceType = ServiceType.fromId(serviceId);
            ServiceStatusDTO result = serviceOrchestrator.startService(serviceType);
            logger.info("✅ Сервис {} запущен", serviceId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Неизвестный сервис: {}", serviceId);

            ServiceStatusDTO errorResponse = new ServiceStatusDTO();
            errorResponse.setErrorMessage("Неизвестный сервис: " + serviceId);
            errorResponse.setServiceName(serviceId);
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ Ошибка запуска сервиса {}: {}", serviceId, e.getMessage());

            ServiceStatusDTO errorResponse = new ServiceStatusDTO();
            errorResponse.setErrorMessage("Ошибка запуска: " + e.getMessage());
            errorResponse.setServiceName(serviceId);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/stop/{serviceId}")
    @Operation(summary = "Остановить конкретный сервис")
    public ResponseEntity<ServiceStatusDTO> stopService(@PathVariable String serviceId) {
        logger.info("🛑 API ВЫЗОВ: Остановка сервиса {}", serviceId);

        try {
            ServiceType serviceType = ServiceType.fromId(serviceId);
            ServiceStatusDTO result = serviceOrchestrator.stopService(serviceType);
            logger.info("✅ Сервис {} остановлен", serviceId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Неизвестный сервис: {}", serviceId);

            ServiceStatusDTO errorResponse = new ServiceStatusDTO();
            errorResponse.setErrorMessage("Неизвестный сервис: " + serviceId);
            errorResponse.setServiceName(serviceId);
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ Ошибка остановки сервиса {}: {}", serviceId, e.getMessage());

            ServiceStatusDTO errorResponse = new ServiceStatusDTO();
            errorResponse.setErrorMessage("Ошибка остановки: " + e.getMessage());
            errorResponse.setServiceName(serviceId);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    @GetMapping("/status")
    @Operation(summary = "Получить статус всех сервисов")
    public ResponseEntity<Map<String, Object>> getAllStatus() {
        logger.info("📊 API ВЫЗОВ: Статус всех сервисов");

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();

        // Проверяем все сервисы
        ServiceType[] allTypes = ServiceType.values();
        for (ServiceType type : allTypes) {
            try {
                ServiceStatusDTO status = serviceOrchestrator.getServiceStatus(type);

                Map<String, Object> serviceInfo = new HashMap<>();
                serviceInfo.put("id", type.getId());
                serviceInfo.put("name", type.getDisplayName());
                serviceInfo.put("state", status.getState() != null ? status.getState().getCode() : "UNKNOWN");
                serviceInfo.put("port", status.getPort());
                serviceInfo.put("running", status.getState() != null && status.getState().isRunning());
                serviceInfo.put("error", status.getErrorMessage());

                services.add(serviceInfo);
            } catch (Exception e) {
                logger.warn("Ошибка получения статуса {}: {}", type, e.getMessage());
            }
        }

        // Получаем статус системы
        Map<String, Object> systemStatus = serviceOrchestrator.getSystemStatus();

        response.put("services", services);
        response.put("system", systemStatus);
        response.put("totalServices", services.size());
        response.put("runningServices", services.stream()
                .filter(s -> Boolean.TRUE.equals(s.get("running")))
                .count());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
    @GetMapping("/status/{serviceId}")
    @Operation(summary = "Получить статус конкретного сервиса")
    public ResponseEntity<ServiceStatusDTO> getServiceStatus(@PathVariable String serviceId) {
        logger.debug("🔍 API ВЫЗОВ: Статус сервиса {}", serviceId);

        try {
            ServiceType serviceType = ServiceType.fromId(serviceId);
            ServiceStatusDTO status = serviceOrchestrator.getServiceStatus(serviceType);

            if (status != null) {
                return ResponseEntity.ok(status);
            }

            ServiceStatusDTO notFound = new ServiceStatusDTO();
            notFound.setErrorMessage("Статус сервиса не найден: " + serviceId);
            notFound.setServiceName(serviceId);
            return ResponseEntity.status(404).body(notFound);

        } catch (IllegalArgumentException e) {
            ServiceStatusDTO errorResponse = new ServiceStatusDTO();
            errorResponse.setErrorMessage("Неизвестный сервис: " + serviceId);
            errorResponse.setServiceName(serviceId);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    @GetMapping("/quick-check")
    @Operation(summary = "Быстрая проверка системы")
    public ResponseEntity<Map<String, Object>> quickSystemCheck() {
        logger.info("⚡ API ВЫЗОВ: Быстрая проверка системы");

        Map<String, Object> response = new HashMap<>();
        response.put("check", "quick_system_check");
        response.put("timestamp", System.currentTimeMillis());

        try {
            // Проверяем ключевые порты
            int[] criticalPorts = {8080, 8097, 8083, 3000}; // Gateway, Auth, Cart, Frontend
            List<Map<String, Object>> portChecks = new ArrayList<>();

            boolean allCriticalOk = true;

            for (int port : criticalPorts) {
                Map<String, Object> check = new HashMap<>();
                check.put("port", port);
                check.put("service", getServiceNameByPort(port));

                boolean isOpen = checkPort(port);
                check.put("status", isOpen ? "OPEN" : "CLOSED");
                check.put("ok", isOpen);

                if (!isOpen) {
                    allCriticalOk = false;
                    check.put("issue", "Порт не отвечает");
                }

                portChecks.add(check);
            }

            response.put("portChecks", portChecks);
            response.put("allCriticalOk", allCriticalOk);
            response.put("systemHealth", allCriticalOk ? "HEALTHY" : "UNHEALTHY");
            response.put("recommendation", allCriticalOk ?
                    "✅ Система работает нормально" :
                    "⚠️ Проблемы с критическими сервисами. Используйте /start-complete");

        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("systemHealth", "CHECK_FAILED");
        }

        return ResponseEntity.ok(response);
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private boolean checkPort(int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getServiceNameByPort(int port) {
        switch (port) {
            case 8080: return "ApiGateway";
            case 8097: return "Auth Service";
            case 8081: return "User Service";
            case 8082: return "Sklad Service";
            case 8084: return "Collector Service";
            case 8083: return "Backet Service";
            case 8085: return "Office Service";
            case 3000: return "Frontend (React)";
            default: return "Unknown (port " + port + ")";
        }
    }
}