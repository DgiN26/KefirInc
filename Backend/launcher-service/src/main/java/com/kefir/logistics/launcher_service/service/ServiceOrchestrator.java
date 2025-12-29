package com.kefir.logistics.launcher_service.service;

import com.kefir.logistics.launcher_service.model.dto.ServiceStatusDTO;
import com.kefir.logistics.launcher_service.model.enums.ServiceState;
import com.kefir.logistics.launcher_service.model.enums.ServiceType;
import com.kefir.logistics.launcher_service.util.PowerShellHelper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ServiceOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ServiceOrchestrator.class);

    @Autowired
    private PowerShellHelper powerShellHelper;

    @Value("${app.autoStart:false}")
    private boolean autoStartEnabled;

    @Value("${app.startup.delay.ms:3000}")
    private int startupDelayMs;

    @Value("${app.services.baseDir:C:\\Users\\2oleg\\Downloads\\Telegram Desktop\\Kefir\\Backend}")
    private String baseDirectory;

    private final Map<ServiceType, Process> runningProcesses = new ConcurrentHashMap<>();
    private final Map<ServiceType, ServiceStatusDTO> serviceStatuses = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final RestTemplate restTemplate = new RestTemplate();

    // Конфигурация портов для миссии
    private static final Map<ServiceType, MissionConfig> MISSION_CONFIG = new HashMap<ServiceType, MissionConfig>() {{
        put(ServiceType.SAGA_SERVICE, new MissionConfig(true, 1, "Критический для решения"));
        put(ServiceType.SKLAD_SERVICE, new MissionConfig(true, 2, "Обнаружение отсутствующих товаров"));
        put(ServiceType.BACKET_SERVICE, new MissionConfig(true, 3, "Начало транзакции клиентом"));
        put(ServiceType.COLLECTOR_SERVICE, new MissionConfig(true, 4, "Обнаружение ошибки сборщиком"));
        put(ServiceType.OFFICE_SERVICE, new MissionConfig(true, 5, "Связь с клиентом"));
        put(ServiceType.DELIVERY_SERVICE, new MissionConfig(false, 6, "Доставка"));
        put(ServiceType.USER_SERVICE, new MissionConfig(false, 7, "Управление пользователями"));
        put(ServiceType.AUTH_SERVICE, new MissionConfig(true, 8, "Аутентификация"));
        put(ServiceType.API_GATEWAY, new MissionConfig(true, 9, "Точка входа"));
    }};

    private static class MissionConfig {
        boolean requiredForMission;
        int startupOrder;
        String missionRole;

        MissionConfig(boolean requiredForMission, int startupOrder, String missionRole) {
            this.requiredForMission = requiredForMission;
            this.startupOrder = startupOrder;
            this.missionRole = missionRole;
        }
    }

    @PostConstruct
    public void init() {
        logger.info("=== SERVICE ORCHESTRATOR INITIALIZATION ===");
        logger.info("Mission: Demonstrate transaction error and solution via Saga");

        initializeServiceStatuses();

        if (autoStartEnabled) {
            new Thread(() -> {
                try {
                    Thread.sleep(startupDelayMs);
                    logger.info("=== AUTO-STARTING MISSION SERVICES ===");
                    startMissionServices();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            logger.info("Автостарт отключен. Сервисы будут запускаться по требованию.");
        }
    }

    private void initializeServiceStatuses() {
        for (ServiceType type : ServiceType.values()) {
            int port = getServicePort(type);
            ServiceStatusDTO status = ServiceStatusDTO.builder()
                    .serviceType(type)
                    .state(ServiceState.STOPPED)
                    .healthUrl("http://localhost:" + port + "/actuator/health")
                    .logPath("./logs/" + type.getId() + ".log")
                    .lastChecked(LocalDateTime.now())
                    .missionRole(getMissionRole(type))
                    .requiredForMission(isRequiredForMission(type))
                    .port(port)
                    .serviceName(type.getDisplayName())
                    .portOpen(false)
                    .build();

            // Проверяем, не запущен ли уже сервис
            if (isPortOpen(port)) {
                // Проверяем, управляем ли мы этим процессом
                if (isPortManagedByUs(type, port)) {
                    status.setState(ServiceState.RUNNING);
                    status.setPortOpen(true);
                    logger.info("✅ Сервис {} (порт {}) уже запущен нами", type.getDisplayName(), port);
                } else {
                    // Порт занят внешним процессом
                    status.setState(ServiceState.EXTERNAL);
                    status.setPortOpen(true);
                    status.setManaged(false);
                    logger.warn("⚠️ Порт {} занят внешним процессом (сервис {})", port, type.getDisplayName());
                }
            }

            serviceStatuses.put(type, status);
        }
        logger.info("Инициализировано {} статусов сервисов", ServiceType.values().length);
    }

    // ============ ОСНОВНЫЕ МЕТОДЫ ============

    /**
     * Запускает все сервисы в оптимальном порядке (для обратной совместимости)
     */
    public List<ServiceStatusDTO> startAllServices() {
        logger.info("🚀 ЗАПУСК ВСЕХ СЕРВИСОВ KEFIR");

        // Сортируем сервисы по порядку запуска
        List<ServiceType> allServices = Arrays.stream(ServiceType.values())
                .sorted(Comparator.comparing(st -> {
                    MissionConfig config = MISSION_CONFIG.get(st);
                    return config != null ? config.startupOrder : 100;
                }))
                .collect(Collectors.toList());

        List<ServiceStatusDTO> results = new ArrayList<>();

        logger.info("1. 🧹 Подготовка портов...");
        releaseExternalPorts(allServices);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Запускаем сервисы
        for (ServiceType serviceType : allServices) {
            try {
                logger.info("🚀 Запуск {}...", serviceType.getDisplayName());
                ServiceStatusDTO result = startService(serviceType);
                results.add(result);

                // Пауза между запусками
                if (!serviceType.equals(allServices.get(allServices.size() - 1))) {
                    Thread.sleep(3000);
                }

            } catch (Exception e) {
                logger.error("❌ Не удалось запустить {}: {}", serviceType.getDisplayName(), e.getMessage());

                ServiceStatusDTO errorStatus = ServiceStatusDTO.builder()
                        .serviceType(serviceType)
                        .state(ServiceState.FAILED)
                        .errorMessage(e.getMessage())
                        .build();
                results.add(errorStatus);
            }
        }

        // Даем время на инициализацию
        try {
            logger.info("⏳ Ожидание инициализации всех сервисов (15 секунд)...");
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Статистика
        long successful = results.stream()
                .filter(s -> s.getState() != null && s.getState().isRunning())
                .count();

        logger.info("📊 Запуск всех сервисов завершен: {}/{} успешно", successful, allServices.size());
        logger.info("=========================================");
        logger.info("🚀 ЗАПУСК ЗАВЕРШЕН");
        logger.info("✅ Успешно: {}", successful);
        logger.info("❌ Неудачно: {}", allServices.size() - successful);
        logger.info("📊 Всего: {}", allServices.size());
        logger.info("=========================================");

        return results;
    }

    /**
     * Проверяет, запущены ли все сервисы для миссии
     */
    public boolean areMissionServicesRunning() {
        return serviceStatuses.values().stream()
                .filter(ServiceStatusDTO::isRequiredForMission)
                .allMatch(status -> status.getState() != null && status.getState().isRunning());
    }

    /**
     * Проверяет, запущен ли конкретный сервис
     */
    public boolean isServiceRunning(ServiceType serviceType) {
        ServiceStatusDTO status = serviceStatuses.get(serviceType);
        if (status == null || status.getState() == null) {
            return false;
        }
        return status.getState().isRunning();
    }

    /**
     * Проверяет здоровье сервиса
     */
    public boolean isServiceHealthy(ServiceType serviceType) {
        try {
            ServiceStatusDTO status = serviceStatuses.get(serviceType);
            if (status == null || status.getState() == null || !status.getState().isRunning()) {
                return false;
            }

            int port = getServicePort(serviceType);

            // 1. Базовая проверка порта
            if (!isPortOpen(port)) {
                return false;
            }

            // 2. Проверка различных эндпоинтов
            String[] endpoints = {
                    "http://localhost:" + port + "/actuator/health",
                    "http://localhost:" + port + "/health",
                    "http://localhost:" + port + "/",
                    "http://localhost:" + port + "/api/health"
            };

            for (String endpoint : endpoints) {
                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.debug("✅ Сервис {} здоров (endpoint: {})", serviceType.getDisplayName(), endpoint);
                        return true;
                    }
                } catch (Exception e) {
                    // Продолжаем проверять другие эндпоинты
                }
            }

            // 3. Если порт открыт, считаем здоровым (даже если эндпоинты не отвечают)
            logger.warn("Сервис {} запущен на порту {}, но эндпоинты не отвечают",
                    serviceType.getDisplayName(), port);
            return true;

        } catch (Exception e) {
            logger.warn("Ошибка проверки здоровья сервиса {}: {}", serviceType.getDisplayName(), e.getMessage());
            return false;
        }
    }

    /**
     * Запускает сервис с интеллектуальной проверкой
     */
    public ServiceStatusDTO startService(ServiceType serviceType) {
        String displayName = serviceType.getDisplayName();
        logger.info("🚀 ЗАПУСК СЕРВИСА: {} (миссия: {})", displayName, getMissionRole(serviceType));

        try {
            // Проверяем, не запущен ли уже сервис
            if (isServiceRunning(serviceType)) {
                if (isServiceHealthy(serviceType)) {
                    logger.info("✅ Сервис {} уже запущен и здоров", displayName);
                    return serviceStatuses.get(serviceType);
                } else {
                    logger.warn("⚠️ Сервис {} запущен, но не здоров. Перезапускаем...", displayName);
                    stopService(serviceType);
                    Thread.sleep(2000);
                }
            }

            updateServiceStatus(serviceType, ServiceState.STARTING, null);

            String directory = getServiceDirectory(serviceType);
            int port = getServicePort(serviceType);

            logger.info("📁 Директория: {}", directory);
            logger.info("🔌 Порт: {}", port);
            logger.info("🎯 Роль в миссии: {}", getMissionRole(serviceType));

            // Проверяем директорию
            File serviceDir = new File(directory);
            if (!serviceDir.exists()) {
                String errorMsg = "❌ Директория не найдена: " + directory;
                logger.error(errorMsg);
                updateServiceStatus(serviceType, ServiceState.FAILED, null, errorMsg);
                return serviceStatuses.get(serviceType);
            }

            // ОСОБЕННО ВАЖНО: Для Saga сервиса обязательно освобождаем порт
            if (serviceType == ServiceType.SAGA_SERVICE) {
                logger.info("🔧 Transaction Saga Service - критический сервис, проверяю порт 8090...");
                if (isPortOpen(port)) {
                    logger.warn("⚠️ Порт 8090 занят. Освобождаю для Saga...");
                    boolean released = powerShellHelper.releasePortWithPowerShell(port);
                    if (!released) {
                        String errorMsg = "Не удалось освободить порт 8090 для Transaction Saga";
                        updateServiceStatus(serviceType, ServiceState.FAILED, null, errorMsg);
                        logger.error(errorMsg);
                        return serviceStatuses.get(serviceType);
                    }
                    Thread.sleep(3000);
                }
            } else {
                // Для других сервисов проверяем, не занят ли порт нашим же процессом
                if (isPortOpen(port) && isPortManagedByUs(serviceType, port)) {
                    logger.info("✅ Порт {} уже используется нашим процессом", port);
                } else if (isPortOpen(port)) {
                    // Порт занят внешним процессом - освобождаем через PowerShell
                    logger.warn("⚠️ Порт {} занят внешним процессом, освобождаю...", port);
                    boolean released = powerShellHelper.releasePortWithPowerShell(port);
                    if (!released) {
                        logger.warn("❌ Не удалось освободить порт {}, пробую продолжить...", port);
                    }
                    Thread.sleep(2000);
                }
            }

            // Собираем команду для запуска
            List<String> command = buildStartCommand(serviceType, port, directory);
            logger.debug("💻 Команда запуска: {}", String.join(" ", command));

            // Запускаем процесс
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(serviceDir);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            runningProcesses.put(serviceType, process);

            // Читаем вывод
            startOutputReader(serviceType, process);

            // Проверяем успешность запуска
            checkServiceStartup(serviceType, process, port);

            return serviceStatuses.get(serviceType);

        } catch (Exception e) {
            logger.error("❌ Не удалось запустить сервис {}: {}", displayName, e.getMessage(), e);
            updateServiceStatus(serviceType, ServiceState.FAILED, null, e.getMessage());
            return serviceStatuses.get(serviceType);
        }
    }

    /**
     * Запускает все сервисы для выполнения миссии в правильном порядке
     */
    public List<ServiceStatusDTO> startMissionServices() {
        logger.info("🎯 ЗАПУСК ВСЕХ СЕРВИСОВ ДЛЯ МИССИИ KEFIR");

        // Получаем сервисы в порядке запуска для миссии
        List<ServiceType> missionServices = getMissionServicesInOrder();

        List<ServiceStatusDTO> results = new ArrayList<>();

        // Сначала освобождаем порты от внешних процессов
        logger.info("1. 🧹 Освобождаю порты от внешних процессов...");
        releaseExternalPortsForMission(missionServices);

        // Запускаем сервисы в порядке важности для миссии
        for (ServiceType serviceType : missionServices) {
            try {
                logger.info("🚀 Запуск {} ({})...",
                        serviceType.getDisplayName(), getMissionRole(serviceType));

                ServiceStatusDTO result = startService(serviceType);
                results.add(result);

                // Особенная пауза после запуска Saga
                if (serviceType == ServiceType.SAGA_SERVICE) {
                    logger.info("⏳ Даю дополнительное время для инициализации Saga...");
                    Thread.sleep(5000);
                } else {
                    Thread.sleep(3000);
                }

            } catch (Exception e) {
                logger.error("❌ Ошибка запуска {}: {}", serviceType.getDisplayName(), e.getMessage());

                ServiceStatusDTO errorStatus = ServiceStatusDTO.builder()
                        .serviceType(serviceType)
                        .state(ServiceState.FAILED)
                        .errorMessage(e.getMessage())
                        .missionRole(getMissionRole(serviceType))
                        .build();
                results.add(errorStatus);
            }
        }

        // Итоговая проверка
        logger.info("📊 ИТОГИ ЗАПУСКА СЕРВИСОВ ДЛЯ МИССИИ:");
        long successful = results.stream()
                .filter(s -> s.getState() != null && s.getState().isRunning())
                .count();

        logger.info("✅ Успешно: {}/{}", successful, missionServices.size());
        logger.info("❌ Неудачно: {}", missionServices.size() - successful);

        // Проверяем критически важные сервисы
        checkCriticalMissionServices();

        return results;
    }

    /**
     * Останавливает сервис
     */
    public ServiceStatusDTO stopService(ServiceType serviceType) {
        String displayName = serviceType.getDisplayName();
        logger.info("🛑 ОСТАНОВКА СЕРВИСА: {}", displayName);

        Process process = runningProcesses.get(serviceType);
        if (process != null && process.isAlive()) {
            try {
                // Мягкая остановка
                process.destroy();
                if (process.waitFor(10, TimeUnit.SECONDS)) {
                    runningProcesses.remove(serviceType);
                    updateServiceStatus(serviceType, ServiceState.STOPPED, null);
                    logger.info("✅ Сервис {} успешно остановлен", displayName);
                } else {
                    // Принудительная остановка
                    process.destroyForcibly();
                    updateServiceStatus(serviceType, ServiceState.FAILED, null, "Принудительно остановлен");
                    logger.warn("⚠️ Сервис {} принудительно остановлен", displayName);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("❌ Прервано при остановке сервиса {}", displayName);
            }
        } else {
            logger.info("Сервис {} не запущен или уже остановлен", displayName);
            updateServiceStatus(serviceType, ServiceState.STOPPED, null);
        }

        return serviceStatuses.get(serviceType);
    }

    /**
     * Перезапускает сервис
     */
    public ServiceStatusDTO restartService(ServiceType serviceType) {
        String displayName = serviceType.getDisplayName();
        logger.info("🔄 ПЕРЕЗАПУСК СЕРВИСА: {}", displayName);

        stopService(serviceType);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return startService(serviceType);
    }

    /**
     * Останавливает все запущенные сервисы
     */
    public Map<String, Object> stopAllRunningServices() {
        logger.info("🛑 ОСТАНОВКА ВСЕХ ЗАПУЩЕННЫХ СЕРВИСОВ");

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> stoppedServices = new ArrayList<>();

        runningProcesses.forEach((serviceType, process) -> {
            if (process != null && process.isAlive()) {
                try {
                    String displayName = serviceType.getDisplayName();
                    process.destroy();

                    if (process.waitFor(5, TimeUnit.SECONDS)) {
                        updateServiceStatus(serviceType, ServiceState.STOPPED, null);
                        stoppedServices.add(displayName);
                        logger.info("✅ Остановлен сервис: {}", displayName);
                    } else {
                        process.destroyForcibly();
                        updateServiceStatus(serviceType, ServiceState.FAILED, null, "Принудительно остановлен");
                        logger.warn("⚠️ Принудительно остановлен сервис: {}", displayName);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("❌ Ошибка при остановке сервиса: {}", e.getMessage());
                }
            }
        });

        runningProcesses.clear();
        result.put("stoppedServices", stoppedServices);
        result.put("count", stoppedServices.size());
        result.put("timestamp", LocalDateTime.now());

        return result;
    }

    // ============ МЕТОДЫ ДЛЯ МИССИИ ============

    /**
     * Получает статусы сервисов, необходимых для миссии
     */
    public List<ServiceStatusDTO> getMissionServiceStatuses() {
        return serviceStatuses.values().stream()
                .filter(ServiceStatusDTO::isRequiredForMission)
                .sorted(Comparator.comparing(s -> MISSION_CONFIG.get(s.getServiceType()).startupOrder))
                .collect(Collectors.toList());
    }

    /**
     * Проверяет готовность системы для выполнения миссии
     */
    public Map<String, Object> checkMissionReadiness() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> serviceChecks = new ArrayList<>();

        boolean allReady = true;
        boolean sagaAvailable = false;

        for (ServiceType serviceType : ServiceType.values()) {
            if (!isRequiredForMission(serviceType)) {
                continue;
            }

            Map<String, Object> check = new LinkedHashMap<>();
            check.put("service", serviceType.getDisplayName());
            check.put("port", getServicePort(serviceType));
            check.put("missionRole", getMissionRole(serviceType));
            check.put("required", true);

            boolean isRunning = isServiceRunning(serviceType);
            boolean isHealthy = isServiceHealthy(serviceType);

            check.put("running", isRunning);
            check.put("healthy", isHealthy);
            check.put("status", isRunning && isHealthy ? "READY" : "NOT_READY");

            if (!isRunning || !isHealthy) {
                allReady = false;
                check.put("issue", isRunning ? "Запущен, но не здоров" : "Не запущен");
            }

            if (serviceType == ServiceType.SAGA_SERVICE) {
                sagaAvailable = isRunning && isHealthy;
                check.put("critical", true);
            }

            serviceChecks.add(check);
        }

        result.put("serviceChecks", serviceChecks);
        result.put("allReady", allReady);
        result.put("sagaAvailable", sagaAvailable);
        result.put("missionPossible", sagaAvailable); // Миссия возможна только если Saga доступен
        result.put("timestamp", LocalDateTime.now());

        if (!sagaAvailable) {
            result.put("criticalMessage", "Transaction Saga Service не доступен. Миссия невозможна!");
            result.put("recommendation", "Запустите Transaction Saga Service на порту 8090");
        } else if (!allReady) {
            result.put("recommendation", "Используйте startMissionServices() для запуска недостающих сервисов");
        } else {
            result.put("recommendation", "Все системы готовы. Можно выполнять миссию.");
        }

        return result;
    }

    /**
     * Интеллектуальный запуск сервисов для демо-сценария
     */
    public List<ServiceStatusDTO> startServicesForDemo(List<ServiceType> servicesToStart) {
        logger.info("🎬 Интеллектуальный запуск сервисов для демо: {} сервисов", servicesToStart.size());

        List<ServiceStatusDTO> results = new ArrayList<>();

        // Сначала освобождаем порты от внешних процессов
        releaseExternalPorts(servicesToStart);

        // Запускаем сервисы
        for (ServiceType serviceType : servicesToStart) {
            try {
                ServiceStatusDTO result = startService(serviceType);
                results.add(result);

                Thread.sleep(2000);

            } catch (Exception e) {
                logger.error("❌ Не удалось запустить {}: {}", serviceType.getDisplayName(), e.getMessage());

                ServiceStatusDTO errorStatus = ServiceStatusDTO.builder()
                        .serviceType(serviceType)
                        .state(ServiceState.FAILED)
                        .errorMessage(e.getMessage())
                        .build();
                results.add(errorStatus);
            }
        }

        return results;
    }

    /**
     * Принудительный запуск сервисов для демо (без проверки здоровья)
     */
    public List<ServiceStatusDTO> forceStartServicesForDemo(List<ServiceType> servicesToStart) {
        logger.info("⚡ ПРИНУДИТЕЛЬНЫЙ запуск сервисов для демо");

        List<ServiceStatusDTO> results = new ArrayList<>();

        // Принудительно освобождаем порты
        for (ServiceType serviceType : servicesToStart) {
            int port = getServicePort(serviceType);
            powerShellHelper.releasePortWithPowerShell(port);
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Запускаем сервисы
        for (ServiceType serviceType : servicesToStart) {
            try {
                ServiceStatusDTO result = startService(serviceType);
                results.add(result);

                Thread.sleep(3000);

            } catch (Exception e) {
                logger.error("❌ Не удалось принудительно запустить {}: {}", serviceType.getDisplayName(), e.getMessage());

                ServiceStatusDTO errorStatus = ServiceStatusDTO.builder()
                        .serviceType(serviceType)
                        .state(ServiceState.FAILED)
                        .errorMessage(e.getMessage())
                        .build();
                results.add(errorStatus);
            }
        }

        return results;
    }

    // ============ МЕТОДЫ ДЛЯ РАБОТЫ С ПОРТАМИ ============

    /**
     * Освобождает порты от внешних процессов для миссии
     */
    public Map<String, Object> releaseExternalPortsForMission(List<ServiceType> missionServices) {
        logger.info("🔧 Освобождение портов от внешних процессов для миссии");

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> releasedPorts = new ArrayList<>();

        for (ServiceType serviceType : missionServices) {
            int port = getServicePort(serviceType);
            String serviceName = serviceType.getDisplayName();

            // Проверяем, управляем ли мы этим портом
            if (isPortManagedByUs(serviceType, port)) {
                logger.debug("Порт {} ({}) управляется нами, пропускаем", port, serviceName);
                continue;
            }

            // Проверяем, занят ли порт
            if (isPortOpen(port)) {
                logger.info("⚠️ Порт {} ({}) занят внешним процессом, освобождаю...", port, serviceName);

                boolean released = powerShellHelper.releasePortWithPowerShell(port);
                if (released) {
                    releasedPorts.add(port + " (" + serviceName + ")");
                    logger.info("✅ Порт {} освобожден", port);
                } else {
                    logger.warn("❌ Не удалось освободить порт {}", port);
                }
            }
        }

        result.put("releasedPorts", releasedPorts);
        result.put("count", releasedPorts.size());
        result.put("method", "PowerShell");
        result.put("timestamp", LocalDateTime.now());

        return result;
    }

    /**
     * Проверяет статус всех портов KEFIR
     */
    public Map<String, Object> checkAllPortsStatus() {
        logger.info("🔍 Проверка статуса всех портов KEFIR");

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Map<String, Object>> portStatus = new LinkedHashMap<>();

        int managed = 0;
        int external = 0;
        int free = 0;

        for (ServiceType serviceType : ServiceType.values()) {
            int port = getServicePort(serviceType);
            String serviceName = serviceType.getDisplayName();

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("port", port);
            status.put("service", serviceName);
            status.put("missionRole", getMissionRole(serviceType));

            boolean isManaged = isPortManagedByUs(serviceType, port);
            boolean isOccupied = isPortOpen(port);

            status.put("occupied", isOccupied);
            status.put("managed", isManaged);

            if (isManaged) {
                status.put("status", "MANAGED");
                status.put("action", "Не трогать (управляется оркестратором)");
                managed++;
            } else if (isOccupied) {
                status.put("status", "EXTERNAL");
                status.put("action", "Можно освободить");
                external++;
            } else {
                status.put("status", "FREE");
                status.put("action", "Свободен для запуска");
                free++;
            }

            portStatus.put(serviceName, status);
        }

        result.put("portStatus", portStatus);
        result.put("summary", Map.of(
                "total", ServiceType.values().length,
                "managed", managed,
                "external", external,
                "free", free
        ));
        result.put("timestamp", LocalDateTime.now());

        return result;
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private String getServiceDirectory(ServiceType serviceType) {
        Map<ServiceType, String> directories = new HashMap<ServiceType, String>() {{
            put(ServiceType.AUTH_SERVICE, baseDirectory + "\\Auth");
            put(ServiceType.USER_SERVICE, baseDirectory + "\\User");
            put(ServiceType.SKLAD_SERVICE, baseDirectory + "\\Sklad");
            put(ServiceType.BACKET_SERVICE, baseDirectory + "\\Backet");
            put(ServiceType.OFFICE_SERVICE, baseDirectory + "\\Office");
            put(ServiceType.COLLECTOR_SERVICE, baseDirectory + "\\Collector");
            put(ServiceType.DELIVERY_SERVICE, baseDirectory + "\\Delivery");
            put(ServiceType.SAGA_SERVICE, baseDirectory + "\\TransactionSaga");
            put(ServiceType.API_GATEWAY, baseDirectory + "\\ApiGateWay");
        }};

        return directories.getOrDefault(serviceType, baseDirectory);
    }

    private int getServicePort(ServiceType serviceType) {
        return serviceType.getDefaultPort();
    }

    private String getMissionRole(ServiceType serviceType) {
        MissionConfig config = MISSION_CONFIG.get(serviceType);
        return config != null ? config.missionRole : "Вспомогательный сервис";
    }

    private boolean isRequiredForMission(ServiceType serviceType) {
        MissionConfig config = MISSION_CONFIG.get(serviceType);
        return config != null && config.requiredForMission;
    }

    private List<ServiceType> getMissionServicesInOrder() {
        return Arrays.stream(ServiceType.values())
                .filter(this::isRequiredForMission)
                .sorted(Comparator.comparing(st -> MISSION_CONFIG.get(st).startupOrder))
                .collect(Collectors.toList());
    }

    private List<String> buildStartCommand(ServiceType serviceType, int port, String directory) {
        List<String> command = new ArrayList<>();
        command.add("cmd");
        command.add("/c");
        command.add("cd");
        command.add("/d");
        command.add(directory);
        command.add("&&");
        command.add("echo");
        command.add("=== Starting " + serviceType.getDisplayName() + " for KEFIR Mission ===");
        command.add("&&");
        command.add("echo");
        command.add("Mission Role: " + getMissionRole(serviceType));
        command.add("&&");
        command.add("mvn");
        command.add("spring-boot:run");
        command.add("-Dserver.port=" + port);
        command.add("-DskipTests");

        return command;
    }

    private void startOutputReader(ServiceType serviceType, Process process) {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("[{}] {}", serviceType.getId(), line);

                    if (line.contains("Started") && line.contains("seconds")) {
                        logger.info("🎉 {} успешно запущен за {} секунд",
                                serviceType.getDisplayName(),
                                extractStartupTime(line));
                    }

                    if (line.contains("ERROR") || line.contains("Failed to start")) {
                        logger.warn("⚠️ Ошибка в {}: {}", serviceType.getDisplayName(), line);
                    }

                    // Особый лог для Saga
                    if (serviceType == ServiceType.SAGA_SERVICE && line.contains("transaction")) {
                        logger.info("🔗 Saga: {}", line);
                    }
                }
            } catch (IOException e) {
                logger.error("Ошибка чтения вывода процесса {}: {}", serviceType.getDisplayName(), e.getMessage());
            }
        });
    }

    private void checkServiceStartup(ServiceType serviceType, Process process, int port) {
        executorService.submit(() -> {
            try {
                // Даем время на запуск (дольше для Saga)
                int waitTime = (serviceType == ServiceType.SAGA_SERVICE) ? 20000 : 15000;
                Thread.sleep(waitTime);

                if (process.isAlive()) {
                    if (isPortOpen(port)) {
                        updateServiceStatus(serviceType, ServiceState.RUNNING, process.pid());
                        logger.info("✅ {} успешно запущен на порту {}",
                                serviceType.getDisplayName(), port);

                        // Запускаем мониторинг здоровья для важных сервисов
                        if (isRequiredForMission(serviceType)) {
                            startHealthMonitoring(serviceType);
                        }
                    } else {
                        String errorMsg = "Порт " + port + " не открыт после " + (waitTime/1000) + " секунд";
                        updateServiceStatus(serviceType, ServiceState.FAILED, null, errorMsg);
                        logger.error("❌ {} запущен, но порт {} не открыт",
                                serviceType.getDisplayName(), port);
                    }
                } else {
                    updateServiceStatus(serviceType, ServiceState.FAILED, null, "Процесс завершился");
                    logger.error("❌ Процесс {} завершился", serviceType.getDisplayName());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void updateServiceStatus(ServiceType serviceType, ServiceState state,
                                     Long pid, String... errorMessage) {
        ServiceStatusDTO status = serviceStatuses.get(serviceType);
        if (status != null) {
            status.setState(state);
            if (pid != null) {
                status.setPid(pid.intValue());
            }
            if (errorMessage.length > 0) {
                status.setErrorMessage(errorMessage[0]);
            }
            if (state == ServiceState.RUNNING) {
                status.setStartedAt(LocalDateTime.now());
                status.setPortOpen(true);
            }
            if (state == ServiceState.EXTERNAL) {
                status.setManaged(false);
            }
            status.setLastChecked(LocalDateTime.now());
        }
    }

    private void startHealthMonitoring(ServiceType serviceType) {
        executorService.submit(() -> {
            int port = getServicePort(serviceType);
            String displayName = serviceType.getDisplayName();

            while (runningProcesses.containsKey(serviceType)) {
                try {
                    Thread.sleep(30000); // Проверка каждые 30 секунд

                    if (!isServiceHealthy(serviceType)) {
                        logger.warn("⚠️ Сервис {} (порт {}) не здоров, перезапускаем...",
                                displayName, port);
                        restartService(serviceType);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void checkCriticalMissionServices() {
        // Проверяем Saga в первую очередь
        if (!isServiceRunning(ServiceType.SAGA_SERVICE) || !isServiceHealthy(ServiceType.SAGA_SERVICE)) {
            logger.error("❌ КРИТИЧЕСКАЯ ОШИБКА: Transaction Saga Service не доступен!");
            logger.error("   Без Saga выполнение миссии невозможно!");
            logger.error("   Порт: 8090, Директория: {}\\TransactionSaga", baseDirectory);
        }

        // Проверяем другие ключевые сервисы
        ServiceType[] criticalServices = {
                ServiceType.SKLAD_SERVICE,
                ServiceType.BACKET_SERVICE,
                ServiceType.COLLECTOR_SERVICE,
                ServiceType.OFFICE_SERVICE
        };

        for (ServiceType service : criticalServices) {
            if (!isServiceRunning(service)) {
                logger.warn("⚠️ Ключевой сервис {} не запущен", service.getDisplayName());
            }
        }
    }

    private void releaseExternalPorts(List<ServiceType> services) {
        for (ServiceType serviceType : services) {
            int port = getServicePort(serviceType);

            if (isPortOpen(port) && !isPortManagedByUs(serviceType, port)) {
                logger.info("Освобождаю порт {} от внешнего процесса...", port);
                powerShellHelper.releasePortWithPowerShell(port);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private boolean isPortManagedByUs(ServiceType serviceType, int port) {
        // Проверяем, есть ли у нас процесс на этом порту
        Process ourProcess = runningProcesses.get(serviceType);
        if (ourProcess != null && ourProcess.isAlive()) {
            return true;
        }

        // Дополнительная проверка через PowerShell
        String pid = powerShellHelper.getProcessIdOnPortPowerShell(port);
        if (pid != null) {
            // Пока считаем, что если порт открыт и у нас есть статус RUNNING, то это наш
            ServiceStatusDTO status = serviceStatuses.get(serviceType);
            return status != null && status.getState() == ServiceState.RUNNING;
        }

        return false;
    }

    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String extractStartupTime(String logLine) {
        // Извлекаем время запуска из строки "Started Application in 5.234 seconds"
        try {
            if (logLine.contains("Started") && logLine.contains("seconds")) {
                String[] parts = logLine.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("in") && i + 1 < parts.length) {
                        return parts[i + 1];
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
        return "unknown";
    }

    /**
     * Получает статус всех сервисов
     */
    public Map<ServiceType, ServiceStatusDTO> getAllServiceStatuses() {
        return new HashMap<>(serviceStatuses);
    }

    /**
     * Получает статус конкретного сервиса
     */
    public ServiceStatusDTO getServiceStatus(ServiceType serviceType) {
        return serviceStatuses.get(serviceType);
    }

    /**
     * Получает количество активных сессий
     */
    public long getActiveSessionsCount() {
        return runningProcesses.values().stream()
                .filter(process -> process != null && process.isAlive())
                .count();
    }


            }


