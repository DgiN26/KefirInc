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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Service Management", description = "Управление микросервисами KEFIR")
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);

    private final ServiceOrchestrator serviceOrchestrator;

    @Autowired
    public ServiceController(ServiceOrchestrator serviceOrchestrator) {
        this.serviceOrchestrator = serviceOrchestrator;
    }

    @GetMapping("/health")
    @Operation(summary = "Проверить здоровье ServiceController")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "KEFIR Launcher Service - Service Controller");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        response.put("mission", "Управление микросервисами для демонстрации решения логистической ошибки");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start-all")
    @Operation(summary = "Запустить все микросервисы KEFIR")
    public ResponseEntity<Map<String, Object>> startAllServices() {
        logger.info("🚀 ЗАПРОС: Запуск всех микросервисов KEFIR");

        try {
            List<ServiceStatusDTO> results = serviceOrchestrator.startAllServices();
            long successful = results.stream()
                    .filter(status -> status.getState() != null && status.getState().isRunning())
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Запуск всех микросервисов инициирован");
            response.put("totalServices", results.size());
            response.put("successful", successful);
            response.put("failed", results.size() - successful);
            response.put("successRate", String.format("%.1f%%", (successful * 100.0 / results.size())));
            response.put("mission", "Подготовка системы для демонстрации логистической ошибки и её решения");
            response.put("timestamp", System.currentTimeMillis());
            response.put("nextSteps", List.of(
                    "Проверить статус: GET /api/v1/services/status",
                    "Запустить демо миссии: POST /api/v1/demo/mission/complete"
            ));

            logger.info("✅ Запуск всех сервисов завершен: {}/{} успешно", successful, results.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Ошибка запуска всех сервисов: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Не удалось запустить все сервисы");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/start-mission")
    @Operation(summary = "Запустить сервисы для выполнения миссии")
    public ResponseEntity<Map<String, Object>> startMissionServices() {
        logger.info("🎯 ЗАПРОС: Запуск сервисов для выполнения миссии");

        try {
            List<ServiceStatusDTO> results = serviceOrchestrator.startMissionServices();
            long successful = results.stream()
                    .filter(status -> status.getState() != null && status.getState().isRunning())
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Запуск сервисов для миссии выполнен");
            response.put("mission", "Демонстрация логистической ошибки и решения через Transaction Saga");
            response.put("criticalServices", List.of(
                    "Transaction Saga Service (порт 8090) - КЛЮЧЕВОЙ для решения",
                    "Warehouse Service (порт 8082) - обнаружение отсутствующих товаров",
                    "Shopping Cart Service (порт 8083) - начало транзакции",
                    "Collector Service (порт 8086) - обнаружение ошибки сборщиком",
                    "Office Service (порт 8085) - связь с клиентом"
            ));
            response.put("totalServices", results.size());
            response.put("successful", successful);
            response.put("sagaAvailable", successful > 0 && results.stream()
                    .anyMatch(s -> s.getServiceType() == ServiceType.SAGA_SERVICE && s.getState().isRunning()));
            response.put("timestamp", System.currentTimeMillis());
            response.put("readyForDemo", successful >= 5); // Минимум 5 ключевых сервисов

            if (response.get("readyForDemo").equals(true)) {
                response.put("nextStep", "POST /api/v1/demo/mission/complete - выполнить полную демонстрацию миссии");
            } else {
                response.put("recommendation", "Запустите недостающие сервисы вручную или проверьте логи");
            }

            logger.info("✅ Запуск сервисов для миссии завершен: {}/{} успешно", successful, results.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Ошибка запуска сервисов для миссии: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Не удалось запустить сервисы для миссии");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("critical", "Transaction Saga Service (порт 8090) должен быть запущен");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/start/{serviceId}")
    @Operation(summary = "Запустить конкретный сервис")
    public ResponseEntity<ServiceStatusDTO> startService(@PathVariable String serviceId) {
        logger.info("🚀 ЗАПРОС: Запуск сервиса {}", serviceId);

        try {
            ServiceType serviceType = ServiceType.fromId(serviceId);
            ServiceStatusDTO result = serviceOrchestrator.startService(serviceType);

            logger.info("✅ Сервис {} запущен: {}", serviceId, result.getState());
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("❌ Неизвестный сервис: {}", serviceId);

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
        logger.info("🛑 ЗАПРОС: Остановка сервиса {}", serviceId);

        try {
            ServiceType serviceType = ServiceType.fromId(serviceId);
            ServiceStatusDTO result = serviceOrchestrator.stopService(serviceType);

            logger.info("✅ Сервис {} остановлен: {}", serviceId, result.getState());
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("❌ Неизвестный сервис: {}", serviceId);

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

    @PostMapping("/restart/{serviceId}")
    @Operation(summary = "Перезапустить конкретный сервис")
    public ResponseEntity<ServiceStatusDTO> restartService(@PathVariable String serviceId) {
        logger.info("🔄 ЗАПРОС: Перезапуск сервиса {}", serviceId);

        try {
            ServiceType serviceType = ServiceType.fromId(serviceId);
            ServiceStatusDTO result = serviceOrchestrator.restartService(serviceType);

            logger.info("✅ Сервис {} перезапущен: {}", serviceId, result.getState());
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("❌ Неизвестный сервис: {}", serviceId);

            ServiceStatusDTO errorResponse = new ServiceStatusDTO();
            errorResponse.setErrorMessage("Неизвестный сервис: " + serviceId);
            errorResponse.setServiceName(serviceId);
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ Ошибка перезапуска сервиса {}: {}", serviceId, e.getMessage());

            ServiceStatusDTO errorResponse = new ServiceStatusDTO();
            errorResponse.setErrorMessage("Ошибка перезапуска: " + e.getMessage());
            errorResponse.setServiceName(serviceId);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Получить статус всех сервисов")
    public ResponseEntity<Map<String, Object>> getAllStatus() {
        logger.debug("🔍 ЗАПРОС: Получение статуса всех сервисов");

        try {
            Map<ServiceType, ServiceStatusDTO> statusMap = serviceOrchestrator.getAllServiceStatuses();
            List<Map<String, Object>> services = new ArrayList<>();

            long runningCount = 0;
            long missionServicesRunning = 0;
            boolean sagaRunning = false;

            for (Map.Entry<ServiceType, ServiceStatusDTO> entry : statusMap.entrySet()) {
                Map<String, Object> serviceInfo = new HashMap<>();
                ServiceStatusDTO status = entry.getValue();

                serviceInfo.put("id", entry.getKey().getId());
                serviceInfo.put("name", entry.getKey().getDisplayName());
                serviceInfo.put("port", entry.getKey().getDefaultPort());
                serviceInfo.put("missionRole", getMissionRole(entry.getKey()));

                if (status.getState() != null) {
                    serviceInfo.put("state", status.getState().getCode());
                    serviceInfo.put("stateDescription", status.getState().getDescription());
                    serviceInfo.put("running", status.getState().isRunning());

                    if (status.getState().isRunning()) {
                        runningCount++;
                    }
                } else {
                    serviceInfo.put("state", "unknown");
                    serviceInfo.put("stateDescription", "Неизвестно");
                    serviceInfo.put("running", false);
                }

                // Проверка ключевых сервисов для миссии
                if (isMissionCriticalService(entry.getKey())) {
                    serviceInfo.put("missionCritical", true);
                    if (status.getState() != null && status.getState().isRunning()) {
                        missionServicesRunning++;
                    }

                    if (entry.getKey() == ServiceType.SAGA_SERVICE && status.getState() != null && status.getState().isRunning()) {
                        sagaRunning = true;
                        serviceInfo.put("saga", true);
                    }
                } else {
                    serviceInfo.put("missionCritical", false);
                }

                serviceInfo.put("pid", status.getPid());
                serviceInfo.put("startedAt", status.getStartedAt());
                serviceInfo.put("lastChecked", status.getLastChecked());
                serviceInfo.put("errorMessage", status.getErrorMessage());
                serviceInfo.put("portOpen", status.isPortOpen());
                serviceInfo.put("managed", status.isManaged());

                services.add(serviceInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("services", services);
            response.put("total", services.size());
            response.put("running", runningCount);
            response.put("stopped", services.size() - runningCount);
            response.put("missionCriticalRunning", missionServicesRunning);
            response.put("sagaAvailable", sagaRunning);
            response.put("missionPossible", sagaRunning && missionServicesRunning >= 3); // Saga + минимум 3 ключевых
            response.put("timestamp", System.currentTimeMillis());
            response.put("mission", "Демонстрация логистической ошибки и решения через Transaction Saga");

            if (!sagaRunning) {
                response.put("criticalWarning", "Transaction Saga Service не запущен! Миссия невозможна.");
                response.put("recommendation", "Запустите Saga: POST /api/v1/services/start/TransactionSaga");
            } else if (missionServicesRunning < 3) {
                response.put("warning", "Недостаточно ключевых сервисов для миссии");
                response.put("recommendation", "Используйте POST /api/v1/services/start-mission для запуска");
            } else {
                response.put("ready", "Система готова к выполнению миссии");
                response.put("nextStep", "POST /api/v1/demo/mission/complete - выполнить демонстрацию");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Ошибка получения статуса сервисов: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Не удалось получить статус сервисов");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/status/{serviceId}")
    @Operation(summary = "Получить статус конкретного сервиса")
    public ResponseEntity<ServiceStatusDTO> getServiceStatus(@PathVariable String serviceId) {
        logger.debug("🔍 ЗАПРОС: Получение статуса сервиса {}", serviceId);

        try {
            ServiceType serviceType = ServiceType.fromId(serviceId);
            ServiceStatusDTO status = serviceOrchestrator.getServiceStatus(serviceType);

            if (status != null) {
                return ResponseEntity.ok(status);
            }

            ServiceStatusDTO notFoundResponse = new ServiceStatusDTO();
            notFoundResponse.setErrorMessage("Статус сервиса не найден: " + serviceId);
            notFoundResponse.setServiceName(serviceId);
            return ResponseEntity.status(404).body(notFoundResponse);

        } catch (IllegalArgumentException e) {
            ServiceStatusDTO errorResponse = new ServiceStatusDTO();
            errorResponse.setErrorMessage("Неизвестный сервис: " + serviceId);
            errorResponse.setServiceName(serviceId);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "Получить список всех доступных сервисов")
    public ResponseEntity<Map<String, Object>> listAllServices() {
        logger.debug("📋 ЗАПРОС: Получение списка всех сервисов");

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();

        for (ServiceType type : ServiceType.values()) {
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("id", type.getId());
            serviceInfo.put("name", type.getDisplayName());
            serviceInfo.put("port", type.getDefaultPort());
            serviceInfo.put("description", type.getDisplayName());
            serviceInfo.put("missionCritical", isMissionCriticalService(type));
            serviceInfo.put("missionRole", getMissionRole(type));
            serviceInfo.put("directory", type.getDirectory());

            services.add(serviceInfo);
        }

        response.put("availableServices", services);
        response.put("count", services.size());
        response.put("missionCriticalCount", services.stream()
                .filter(s -> (Boolean) s.get("missionCritical"))
                .count());
        response.put("timestamp", System.currentTimeMillis());
        response.put("mission", "KEFIR Logistics - Демонстрация решения логистической ошибки");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/mission/readiness")
    @Operation(summary = "Проверить готовность системы для выполнения миссии")
    public ResponseEntity<Map<String, Object>> checkMissionReadiness() {
        logger.info("🎯 ЗАПРОС: Проверка готовности системы для миссии");

        try {
            Map<String, Object> readiness = serviceOrchestrator.checkMissionReadiness();
            return ResponseEntity.ok(readiness);

        } catch (Exception e) {
            logger.error("❌ Ошибка проверки готовности миссии: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Не удалось проверить готовность системы");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/stop-all")
    @Operation(summary = "Остановить все запущенные сервисы")
    public ResponseEntity<Map<String, Object>> stopAllServices() {
        logger.info("🛑 ЗАПРОС: Остановка всех запущенных сервисов");

        try {
            Map<String, Object> result = serviceOrchestrator.stopAllRunningServices();
            result.put("message", "Все сервисы остановлены");
            result.put("mission", "Система возвращена в исходное состояние");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Ошибка остановки всех сервисов: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Не удалось остановить все сервисы");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private boolean isMissionCriticalService(ServiceType serviceType) {
        return serviceType == ServiceType.SAGA_SERVICE ||
                serviceType == ServiceType.SKLAD_SERVICE ||
                serviceType == ServiceType.BACKET_SERVICE ||
                serviceType == ServiceType.COLLECTOR_SERVICE ||
                serviceType == ServiceType.OFFICE_SERVICE;
    }

    private String getMissionRole(ServiceType serviceType) {
        switch (serviceType) {
            case SAGA_SERVICE:
                return "Transaction Saga - решение ошибки транзакции (КРИТИЧЕСКИ ВАЖЕН)";
            case SKLAD_SERVICE:
                return "Warehouse Service - обнаружение отсутствующих товаров";
            case BACKET_SERVICE:
                return "Shopping Cart Service - начало транзакции клиентом";
            case COLLECTOR_SERVICE:
                return "Collector Service - обнаружение ошибки сборщиком";
            case OFFICE_SERVICE:
                return "Office Service - связь с клиентом при ошибке";
            case AUTH_SERVICE:
                return "Authentication Service - аутентификация пользователей";
            case USER_SERVICE:
                return "User Management Service - управление пользователями";
            case DELIVERY_SERVICE:
                return "Delivery Service - доставка товаров";
            case API_GATEWAY:
                return "API Gateway - точка входа в систему";
            default:
                return "Вспомогательный сервис";
        }
    }
}