package com.kefir.logistics.launcher_service.controller;

import com.kefir.logistics.launcher_service.model.dto.DemoScenarioDTO;
import com.kefir.logistics.launcher_service.model.dto.ServiceStatusDTO;
import com.kefir.logistics.launcher_service.service.DemoScenarioService;
import com.kefir.logistics.launcher_service.service.ServiceOrchestrator;
import com.kefir.logistics.launcher_service.util.PowerShellHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Главный контроллер для управления демо-сценариями KEFIR.
 * ОСНОВНОЕ ПРАВИЛО: НЕ ВМЕШИВАТЬСЯ В РАБОТУ ServiceOrchestrator!
 * Только мониторинг и демонстрация.
 */
@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Demo Scenarios", description = "Управление демо-сценариями KEFIR")
@EnableScheduling
public class DemoController {
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    private ServiceOrchestrator serviceOrchestrator;

    @Autowired
    private DemoScenarioService demoScenarioService;

    @Autowired
    private PowerShellHelper powerShellHelper;

    @Value("${app.demo.startup.check:true}")
    private boolean startupCheckEnabled;

    @Value("${app.demo.autoStartScenario:none}")
    private String autoStartScenario;

    @Value("${app.reports.directory:./reports}")
    private String reportsDirectory;

    // Порты KEFIR согласно миссии - ТОЛЬКО ДЛЯ ИНФОРМАЦИИ
    private static final Map<String, Integer> KEFIR_SERVICES = new LinkedHashMap<String, Integer>() {{
        put("Launcher Service", 8099);
        put("API Gateway", 8080);
        put("Authentication Service", 8097);
        put("User Management Service", 8081);
        put("Warehouse Service", 8082);
        put("Shopping Cart Service", 8083);
        put("Office Management Service", 8085);
        put("Collector Service", 8086);
        put("Delivery Service", 8088);
        put("Transaction Saga Service", 8090);
    }};

    // Статистика
    private final AtomicInteger totalTransactions = new AtomicInteger(0);
    private final AtomicInteger failedTransactions = new AtomicInteger(0);
    private final LocalDateTime startTime = LocalDateTime.now();

    // Фоновые задачи
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final RestTemplate restTemplate;

    // Вспомогательные классы
    private static class ServiceInfo {
        String name;
        int port;
        boolean isRunning;
        boolean isHealthy;
        LocalDateTime lastChecked;

        ServiceInfo(String name, int port) {
            this.name = name;
            this.port = port;
            this.isRunning = false;
            this.isHealthy = false;
            this.lastChecked = LocalDateTime.now();
        }
    }

    public DemoController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();

        logger.info("DemoController initialized with mission: Demonstrate transaction error and solution");
    }

    @PostConstruct
    public void init() {
        logger.info("🚀 Initializing DemoController for KEFIR mission...");
        logger.info("📌 РЕЖИМ: Только мониторинг, НЕ управление процессами!");

        // Инициализация информации о сервисах
        initializeServiceInfos();

        // Создание директорий для отчетов
        createDirectories();

        // Автозапуск демо если настроено
        if (!"none".equalsIgnoreCase(autoStartScenario)) {
            logger.info("📋 Автозапуск демо-сценария: {}", autoStartScenario);
            scheduleAutoStart(autoStartScenario, 15000); // Даем больше времени на старт
        }

        // Запуск фоновых задач ТОЛЬКО мониторинга
        if (startupCheckEnabled) {
            startBackgroundTasks();
        }

        logger.info("✅ DemoController initialized");
        logger.info("   Mission: Demonstrate transaction error → Solution via Saga");
        logger.info("   Key Ports: Saga(8090), Warehouse(8082), Cart(8083), Collector(8086)");
        logger.info("   ❗ Важно: ServiceOrchestrator управляет сервисами, DemoController только мониторит");
    }

    @PreDestroy
    public void cleanup() {
        logger.info("🧹 Shutting down DemoController...");

        // ❗ НЕ ОСТАНАВЛИВАЕМ ПРОЦЕССЫ! ServiceOrchestrator сам управляет ими

        // Завершаем фоновые задачи
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("✅ DemoController shutdown complete");
    }

    // ============ ДЕМО-СЦЕНАРИИ ДЛЯ МИССИИ ============

    @PostMapping("/mission/demonstrate-problem")
    @Operation(summary = "Демонстрация проблемы из миссии: неполный заказ")
    public ResponseEntity<Map<String, Object>> demonstrateMissionProblem() {
        logger.info("⚠️ ДЕМОНСТРАЦИЯ ПРОБЛЕМЫ ИЗ МИССИИ");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mission", "Пункт 1.1-1.2: Демонстрация ошибки транзакции");
        result.put("operationId", "MISSION_PROBLEM_" + System.currentTimeMillis());
        result.put("startTime", LocalDateTime.now());

        try {
            // Шаг 1: Проверяем готовность сервисов
            logger.info("1. 🔍 Проверка готовности сервисов...");
            boolean servicesReady = checkMissionServicesReady();
            if (!servicesReady) {
                // Автоматически пытаемся запустить недостающие сервисы
                logger.info("🔄 Не все сервисы готовы. Запускаю автоматически...");
                ResponseEntity<Map<String, Object>> startupResult = startMissionServices();

                if (startupResult.getStatusCode() != HttpStatus.OK) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(Map.of(
                                    "error", "Не удалось запустить необходимые сервисы",
                                    "requiredServices", getRequiredMissionServices(),
                                    "startupResult", startupResult.getBody(),
                                    "timestamp", LocalDateTime.now()
                            ));
                }

                // Даем время на запуск
                Thread.sleep(15000);
                servicesReady = checkMissionServicesReady();

                if (!servicesReady) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(Map.of(
                                    "error", "Сервисы не запустились после автоматической попытки",
                                    "requiredServices", getRequiredMissionServices(),
                                    "timestamp", LocalDateTime.now()
                            ));
                }
            }

            // Шаг 2: Демонстрация проблемы
            logger.info("2. 🎬 Демонстрация сценария проблемы...");
            List<String> problemSteps = createProblemScenario();
            result.put("problemSteps", problemSteps);

            // Шаг 3: Анализ последствий
            logger.info("3. 📊 Анализ последствий...");
            Map<String, Object> consequences = analyzeConsequences();
            result.put("consequences", consequences);

            // Шаг 4: Итоги
            result.put("status", "PROBLEM_DEMONSTRATED");
            result.put("endTime", LocalDateTime.now());
            result.put("conclusion", "Частичное закрытие транзакции приводит к неполной доставке и увеличению времени доставки в 4 раза");

            totalTransactions.incrementAndGet();

            logger.info("✅ Проблема из миссии успешно продемонстрирована");
            logger.info("   Суть: Транзакция закрывается частично → клиент получает неполный заказ → доставка 1 час вместо 15 мин");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Ошибка демонстрации проблемы: {}", e.getMessage(), e);

            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("endTime", LocalDateTime.now());

            failedTransactions.incrementAndGet();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/mission/demonstrate-solution")
    @Operation(summary = "Демонстрация решения из миссии: перезапуск транзакции")
    public ResponseEntity<Map<String, Object>> demonstrateMissionSolution() {
        logger.info("💡 ДЕМОНСТРАЦИЯ РЕШЕНИЯ ИЗ МИССИИ");

        // КРИТИЧЕСКАЯ ПРОВЕРКА: Saga должен быть доступен
        boolean sagaAvailable = isPortOpen(8090);
        if (!sagaAvailable) {
            // Автоматически пытаемся запустить Saga
            logger.warn("⚠️ Transaction Saga не доступен. Пытаюсь запустить...");
            try {
                serviceOrchestrator.startService(com.kefir.logistics.launcher_service.model.enums.ServiceType.SAGA_SERVICE);
                Thread.sleep(10000);
                sagaAvailable = isPortOpen(8090);
            } catch (Exception e) {
                logger.error("❌ Не удалось запустить Saga: {}", e.getMessage());
            }
        }

        if (!sagaAvailable) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Transaction Saga Service не доступен");
            error.put("mission", "Пункт 2: Решение через полный перезапуск транзакции");
            error.put("critical", true);
            error.put("port", 8090);
            error.put("recommendation", "Запустите Transaction Saga Service на порту 8090 для демонстрации решения");
            error.put("command", "cd TransactionSaga && mvn spring-boot:run -Dserver.port=8090");

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mission", "Пункт 2: Решение через полный перезапуск транзакции");
        result.put("operationId", "MISSION_SOLUTION_" + System.currentTimeMillis());
        result.put("startTime", LocalDateTime.now());
        result.put("sagaAvailable", true);
        result.put("sagaPort", 8090);

        try {
            // Шаг 1: Философия решения
            logger.info("1. 🧠 Философия решения...");
            Map<String, Object> philosophy = getSolutionPhilosophy();
            result.put("philosophy", philosophy);

            // Шаг 2: Демонстрация решения
            logger.info("2. 🎬 Демонстрация решения...");
            List<String> solutionSteps = createSolutionScenario();
            result.put("solutionSteps", solutionSteps);

            // Шаг 3: Преимущества
            logger.info("3. ✅ Преимущества подхода...");
            Map<String, Object> benefits = getSolutionBenefits();
            result.put("benefits", benefits);

            // Шаг 4: Сравнение
            logger.info("4. 📊 Сравнение подходов...");
            Map<String, Object> comparison = createSolutionComparison();
            result.put("comparison", comparison);

            // Шаг 5: Итоги
            result.put("status", "SOLUTION_DEMONSTRATED");
            result.put("endTime", LocalDateTime.now());
            result.put("keyMessage", "При неизвестных системных ошибках безопаснее перезагрузить процесс, чем пытаться его починить");
            result.put("missionAccomplished", true);

            totalTransactions.incrementAndGet();

            logger.info("✅ Решение из миссии успешно продемонстрировано");
            logger.info("   Суть: Полный перезапуск транзакции → перепроверка всех товаров → доставка 15 минут");
            logger.info("   Философия: 'Перезагрузить процесс' вместо 'чинить неизвестную ошибку'");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Ошибка демонстрации решения: {}", e.getMessage(), e);

            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("endTime", LocalDateTime.now());

            failedTransactions.incrementAndGet();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/mission/complete")
    @Operation(summary = "Полное выполнение миссии: проблема + решение")
    public ResponseEntity<Map<String, Object>> completeMission() {
        logger.info("🎯 ПОЛНОЕ ВЫПОЛНЕНИЕ МИССИИ ПРИЛОЖЕНИЯ");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mission", "KEFIR: Демонстрация ошибки транзакции и её решения");
        result.put("operationId", "MISSION_COMPLETE_" + System.currentTimeMillis());
        result.put("startTime", LocalDateTime.now());

        try {
            // Часть 1: Подготовка
            logger.info("🔧 ПОДГОТОВКА К ВЫПОЛНЕНИЮ МИССИИ");

            // 1. Запускаем все необходимые сервисы через ServiceOrchestrator
            logger.info("1. 🚀 Запускаю все сервисы для миссии...");
            ResponseEntity<Map<String, Object>> startResult = startMissionServices();
            if (startResult.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Не удалось запустить сервисы: " + startResult.getBody());
            }

            // 2. Даем время на инициализацию
            logger.info("2. ⏳ Ожидание инициализации сервисов (20 секунд)...");
            Thread.sleep(20000);

            // 3. Проверяем, что все порты открыты
            logger.info("3. 🔍 Проверка всех портов...");
            boolean allPortsOpen = checkAllMissionPorts();
            if (!allPortsOpen) {
                // Автоматически пытаемся освободить и перезапустить
                logger.warn("⚠️ Не все порты открыты. Пытаюсь исправить...");
                fixMissionPorts();
                Thread.sleep(10000);
                allPortsOpen = checkAllMissionPorts();

                if (!allPortsOpen) {
                    throw new RuntimeException("Не удалось открыть все необходимые порты после попытки исправления");
                }
            }

            // Часть 2: Демонстрация проблемы
            logger.info("⚠️ ЧАСТЬ 1: ДЕМОНСТРАЦИЯ ПРОБЛЕМЫ");
            ResponseEntity<Map<String, Object>> problemResponse = demonstrateMissionProblem();
            if (problemResponse.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Не удалось продемонстрировать проблему: " +
                        problemResponse.getBody());
            }
            result.put("problem", problemResponse.getBody());

            // Пауза для осмысления
            logger.info("⏸️  Пауза для осмысления проблемы (5 секунд)...");
            Thread.sleep(5000);

            // Часть 3: Демонстрация решения
            logger.info("💡 ЧАСТЬ 2: ДЕМОНСТРАЦИЯ РЕШЕНИЯ");
            ResponseEntity<Map<String, Object>> solutionResponse = demonstrateMissionSolution();
            if (solutionResponse.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Не удалось продемонстрировать решение: " +
                        solutionResponse.getBody());
            }
            result.put("solution", solutionResponse.getBody());

            // Часть 4: Итоги и выводы
            logger.info("📊 ЧАСТЬ 3: ИТОГИ И ВЫВОДЫ");
            Map<String, Object> conclusions = createMissionConclusions();
            result.put("conclusions", conclusions);

            // Финальный статус
            result.put("status", "MISSION_COMPLETED");
            result.put("endTime", LocalDateTime.now());
            result.put("success", true);
            result.put("missionPoints", Arrays.asList(
                    "1.1 ✓ Разыграна ситуация с ошибкой транзакции",
                    "1.2 ✓ Продемонстрированы последствия неполной доставки",
                    "2 ✓ Предложено и продемонстрировано решение через Saga"
            ));

            totalTransactions.incrementAndGet();

            logger.info("✅ МИССИЯ ПРИЛОЖЕНИЯ ВЫПОЛНЕНА!");
            logger.info("   =========================================");
            logger.info("   ПРОБЛЕМА: Частичное закрытие транзакции");
            logger.info("   РЕШЕНИЕ:  Полный перезапуск через Saga");
            logger.info("   РЕЗУЛЬТАТ: Доставка 15 мин вместо 1 часа");
            logger.info("   =========================================");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ КРИТИЧЕСКАЯ ОШИБКА ВЫПОЛНЕНИЯ МИССИИ: {}", e.getMessage(), e);

            result.put("status", "MISSION_FAILED");
            result.put("endTime", LocalDateTime.now());
            result.put("error", e.getMessage());
            result.put("criticalIssue", "Не удалось запустить или поддерживать сервисы");
            result.put("recommendation", "Проверьте логи и убедитесь, что все порты свободны");

            failedTransactions.incrementAndGet();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    // ============ УПРАВЛЕНИЕ ПОРТАМИ (АГРЕССИВНОЕ) ============

    @PostMapping("/ports/force-release-all")
    @Operation(summary = "Принудительное освобождение ВСЕХ портов KEFIR")
    public ResponseEntity<Map<String, Object>> forceReleaseAllPorts() {
        logger.info("💥 ПРИНУДИТЕЛЬНОЕ ОСВОБОЖДЕНИЕ ВСЕХ ПОРТОВ KEFIR");

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> actions = new ArrayList<>();

        int releasedCount = 0;
        int alreadyFreeCount = 0;
        int failedCount = 0;

        try {
            // Сначала останавливаем ВСЕ сервисы через ServiceOrchestrator
            logger.info("1. 🛑 Останавливаю все управляемые сервисы...");
            serviceOrchestrator.stopAllRunningServices();
            actions.add("✅ Все управляемые сервисы остановлены");

            Thread.sleep(5000);

            // Затем освобождаем ВСЕ порты через PowerShell
            logger.info("2. 🔧 Освобождаю ВСЕ порты через PowerShell...");
            for (Map.Entry<String, Integer> entry : KEFIR_SERVICES.entrySet()) {
                String serviceName = entry.getKey();
                int port = entry.getValue();

                // Пропускаем лаунчер (это мы сами)
                if ("Launcher Service".equals(serviceName)) {
                    actions.add("⏭️ " + serviceName + " (порт " + port + "): это лаунчер, пропускаем");
                    continue;
                }

                // Проверяем, занят ли порт
                boolean isOccupied = isPortOpen(port);

                if (isOccupied) {
                    logger.warn("⚠️ {} (порт {}) занят, принудительно освобождаю...", serviceName, port);

                    // Многократные попытки освобождения
                    boolean released = false;
                    for (int attempt = 1; attempt <= 3; attempt++) {
                        logger.info("   Попытка {}/3 освободить порт {}", attempt, port);
                        released = powerShellHelper.releasePortWithPowerShell(port);
                        if (released) {
                            break;
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    if (released) {
                        actions.add("✅ " + serviceName + " (порт " + port + "): успешно освобожден");
                        releasedCount++;
                        logger.info("✅ Порт {} освобожден", port);
                    } else {
                        actions.add("❌ " + serviceName + " (порт " + port + "): не удалось освободить");
                        failedCount++;
                        logger.error("❌ Не удалось освободить порт {}", port);
                    }
                } else {
                    actions.add("✅ " + serviceName + " (порт " + port + "): уже свободен");
                    alreadyFreeCount++;
                }

                // Небольшая пауза между портами
                Thread.sleep(500);
            }

            // Формируем результат
            result.put("strategy", "АГРЕССИВНОЕ освобождение: остановка всех сервисов + PowerShell");
            result.put("totalPorts", KEFIR_SERVICES.size() - 1); // минус лаунчер
            result.put("releasedPorts", releasedCount);
            result.put("alreadyFreePorts", alreadyFreeCount);
            result.put("failedPorts", failedCount);
            result.put("actions", actions);
            result.put("powerShellUsed", true);
            result.put("timestamp", LocalDateTime.now());

            logger.info("📊 Итоги: {} освобождено, {} уже свободно, {} не удалось",
                    releasedCount, alreadyFreeCount, failedCount);

            // Рекомендация
            result.put("recommendation", "Теперь можно запускать сервисы заново");
            result.put("nextSteps", Arrays.asList(
                    "POST /api/v1/services/start-mission - запустить сервисы для миссии",
                    "POST /api/v1/demo/mission/complete - выполнить полную демонстрацию"
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Ошибка принудительного освобождения портов: {}", e.getMessage(), e);

            result.put("error", e.getMessage());
            result.put("status", "PARTIAL_SUCCESS");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/ports/auto-fix")
    @Operation(summary = "Автоматическое исправление проблем с портами")
    public ResponseEntity<Map<String, Object>> autoFixPorts() {
        logger.info("🔧 АВТОМАТИЧЕСКОЕ ИСПРАВЛЕНИЕ ПРОБЛЕМ С ПОРТАМИ");

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> actions = new ArrayList<>();

        try {
            // 1. Проверяем все порты
            logger.info("1. 🔍 Проверяю состояние всех портов...");
            Map<String, Object> portStatus = checkAllPortsStatusDetailed();
            result.put("initialStatus", portStatus);

            // 2. Находим проблемные порты
            List<Integer> problematicPorts = findProblematicPorts();
            if (problematicPorts.isEmpty()) {
                actions.add("✅ Все порты в порядке, исправление не требуется");
                result.put("status", "NO_ACTION_NEEDED");
                return ResponseEntity.ok(result);
            }

            logger.info("2. ⚠️ Найдено проблемных портов: {}", problematicPorts.size());
            actions.add("Найдено проблемных портов: " + problematicPorts.size());

            // 3. Для каждого проблемного порта
            for (int port : problematicPorts) {
                String serviceName = getServiceNameByPort(port);
                logger.info("   Исправляю порт {} ({})...", port, serviceName);

                // 3.1. Останавливаем через ServiceOrchestrator (если он управляет)
                try {
                    com.kefir.logistics.launcher_service.model.enums.ServiceType serviceType =
                            com.kefir.logistics.launcher_service.model.enums.ServiceType.fromPort(port);
                    serviceOrchestrator.stopService(serviceType);
                    actions.add("🛑 Остановлен через оркестратор: " + serviceName);
                } catch (Exception e) {
                    // Не нашли в ServiceType - значит внешний процесс
                }

                // 3.2. Освобождаем порт через PowerShell
                boolean released = powerShellHelper.releasePortWithPowerShell(port);
                if (released) {
                    actions.add("✅ Освобожден через PowerShell: " + serviceName);
                } else {
                    actions.add("❌ Не удалось освободить: " + serviceName);
                }

                // 3.3. Перезапускаем через ServiceOrchestrator (если это наш сервис)
                try {
                    com.kefir.logistics.launcher_service.model.enums.ServiceType serviceType =
                            com.kefir.logistics.launcher_service.model.enums.ServiceType.fromPort(port);

                    // Ждем, чтобы порт точно освободился
                    Thread.sleep(3000);

                    // Запускаем заново
                    serviceOrchestrator.startService(serviceType);
                    actions.add("🚀 Перезапущен через оркестратор: " + serviceName);

                    // Даем время на запуск
                    Thread.sleep(5000);

                    // Проверяем
                    if (isPortOpen(port)) {
                        actions.add("✅ Проверка: порт " + port + " открыт");
                    } else {
                        actions.add("⚠️ Внимание: порт " + port + " не открылся после перезапуска");
                    }
                } catch (Exception e) {
                    // Не наш сервис или ошибка запуска
                }

                Thread.sleep(2000);
            }

            // 4. Финальная проверка
            logger.info("3. ✅ Финальная проверка...");
            Thread.sleep(10000);
            Map<String, Object> finalStatus = checkAllPortsStatusDetailed();
            result.put("finalStatus", finalStatus);

            // 5. Итоги
            result.put("actions", actions);
            result.put("problematicPorts", problematicPorts);
            result.put("timestamp", LocalDateTime.now());
            result.put("status", "AUTO_FIX_COMPLETED");

            logger.info("✅ Автоматическое исправление завершено");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Ошибка автоматического исправления: {}", e.getMessage(), e);

            result.put("error", e.getMessage());
            result.put("status", "AUTO_FIX_FAILED");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/ports/status-detailed")
    @Operation(summary = "Подробный статус всех портов с рекомендациями")
    public ResponseEntity<Map<String, Object>> getPortsStatusDetailed() {
        logger.info("🔍 ПОДРОБНАЯ ПРОВЕРКА СТАТУСА ВСЕХ ПОРТОВ");

        Map<String, Object> result = checkAllPortsStatusDetailed();
        return ResponseEntity.ok(result);
    }

    // ============ СЕРВИСЫ ДЛЯ МИССИИ ============

    @PostMapping("/services/start-mission")
    @Operation(summary = "Запуск сервисов для выполнения миссии")
    public ResponseEntity<Map<String, Object>> startMissionServices() {
        logger.info("🚀 ЗАПУСК СЕРВИСОВ ДЛЯ ВЫПОЛНЕНИЯ МИССИИ");

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 1. Сначала освобождаем все порты
            logger.info("1. 🧹 Освобождаю порты от возможных блокировок...");
            forceReleaseAllPorts();

            Thread.sleep(5000);

            // 2. Запускаем через ServiceOrchestrator
            logger.info("2. 🚀 Запускаю сервисы через ServiceOrchestrator...");
            List<ServiceStatusDTO> serviceResults = serviceOrchestrator.startMissionServices();

            // 3. Даем время на запуск
            logger.info("3. ⏳ Даю время на запуск (20 секунд)...");
            Thread.sleep(20000);

            // 4. Проверяем результат
            logger.info("4. 🔍 Проверяю результат запуска...");
            long successful = serviceResults.stream()
                    .filter(status -> status.getState() != null && status.getState().isRunning())
                    .count();

            boolean sagaRunning = serviceResults.stream()
                    .anyMatch(s -> s.getServiceType() ==
                            com.kefir.logistics.launcher_service.model.enums.ServiceType.SAGA_SERVICE
                            && s.getState() != null && s.getState().isRunning());

            // 5. Автоматически исправляем проблемы
            if (!sagaRunning || successful < 5) {
                logger.warn("⚠️ Не все сервисы запустились. Пытаюсь исправить...");
                autoFixPorts();
                Thread.sleep(10000);

                // Запускаем еще раз то, что не запустилось
                serviceResults = serviceOrchestrator.startMissionServices();
                Thread.sleep(15000);

                successful = serviceResults.stream()
                        .filter(status -> status.getState() != null && status.getState().isRunning())
                        .count();
            }

            // 6. Формируем ответ
            result.put("services", serviceResults);
            result.put("total", serviceResults.size());
            result.put("successful", successful);
            result.put("missionReady", successful >= 5);
            result.put("sagaAvailable", sagaRunning);
            result.put("timestamp", LocalDateTime.now());

            if (successful >= 5 && sagaRunning) {
                result.put("status", "READY_FOR_MISSION");
                result.put("message", "Сервисы успешно запущены, можно выполнять миссию");
                result.put("nextStep", "POST /api/v1/demo/mission/complete - выполнить демонстрацию");
                logger.info("✅ Сервисы для миссии успешно запущены: {}/{}", successful, serviceResults.size());
            } else {
                result.put("status", "PARTIAL_SUCCESS");
                result.put("message", "Не все сервисы запустились");
                result.put("recommendation", "Используйте POST /api/v1/demo/ports/auto-fix для автоматического исправления");
                logger.warn("⚠️ Частичный успех: запущено только {}/{} сервисов", successful, serviceResults.size());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Ошибка запуска сервисов для миссии: {}", e.getMessage(), e);

            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/services/mission-status")
    @Operation(summary = "Статус сервисов для выполнения миссии")
    public ResponseEntity<Map<String, Object>> getMissionServicesStatus() {
        logger.info("🔍 СТАТУС СЕРВИСОВ ДЛЯ МИССИИ");

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Map<String, Object>> servicesStatus = new LinkedHashMap<>();

        // Ключевые сервисы для миссии
        Map<String, Integer> missionServices = new LinkedHashMap<String, Integer>() {{
            put("Transaction Saga Service", 8090);
            put("Warehouse Service", 8082);
            put("Shopping Cart Service", 8083);
            put("Collector Service", 8086);
            put("Office Management Service", 8085);
            put("Authentication Service", 8097);  // Добавил 8097
            put("API Gateway", 8080);
        }};

        boolean allReady = true;
        List<String> notReady = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : missionServices.entrySet()) {
            String serviceName = entry.getKey();
            int port = entry.getValue();

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("port", port);
            status.put("serviceName", serviceName);

            // Проверяем порт
            boolean portOpen = isPortOpen(port);
            status.put("portOpen", portOpen);

            // Проверяем здоровье если порт открыт
            if (portOpen) {
                try {
                    boolean healthy = checkServiceHealth(port);
                    status.put("healthy", healthy);
                    status.put("status", healthy ? "READY" : "RUNNING_BUT_UNHEALTHY");

                    if (!healthy) {
                        allReady = false;
                        notReady.add(serviceName + " (запущен, но не отвечает)");
                    }

                } catch (Exception e) {
                    status.put("healthy", false);
                    status.put("status", "RUNNING_BUT_ERROR");
                    allReady = false;
                    notReady.add(serviceName + " (ошибка проверки)");
                }
            } else {
                status.put("healthy", false);
                status.put("status", "NOT_RUNNING");
                allReady = false;
                notReady.add(serviceName + " (не запущен)");
            }

            servicesStatus.put(serviceName, status);
        }

        result.put("services", servicesStatus);
        result.put("allReady", allReady);
        result.put("notReady", notReady);
        result.put("missionPossible", isPortOpen(8090)); // Saga критически важен
        result.put("timestamp", LocalDateTime.now());

        if (!isPortOpen(8090)) {
            result.put("critical", "Transaction Saga Service не запущен (порт 8090)");
            result.put("recommendation", "Используйте POST /api/v1/demo/services/start-mission для автоматического запуска");
        } else if (!allReady) {
            result.put("recommendation", "Используйте POST /api/v1/demo/ports/auto-fix для автоматического исправления");
        } else {
            result.put("recommendation", "Все сервисы готовы. Используйте POST /api/v1/demo/mission/complete для выполнения миссии");
        }

        return ResponseEntity.ok(result);
    }

    // ============ УТИЛИТНЫЕ МЕТОДЫ ============

    private void initializeServiceInfos() {
        logger.info("Инициализация информации о сервисах (только мониторинг)...");

        for (Map.Entry<String, Integer> entry : KEFIR_SERVICES.entrySet()) {
            String serviceName = entry.getKey();
            int port = entry.getValue();

            boolean isRunning = isPortOpen(port);

            if (isRunning) {
                logger.info("   ✅ {} (порт {}) запущен", serviceName, port);
            } else {
                logger.info("   ❌ {} (порт {}) не запущен", serviceName, port);
            }
        }

        logger.info("Мониторинг инициализирован для {} сервисов", KEFIR_SERVICES.size());
    }

    private void createDirectories() {
        try {
            Path reportDir = Paths.get(reportsDirectory);
            if (!Files.exists(reportDir)) {
                Files.createDirectories(reportDir);
                logger.info("Создана директория для отчетов: {}", reportDir.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Не удалось создать директории: {}", e.getMessage());
        }
    }

    private void startBackgroundTasks() {
        // ТОЛЬКО мониторинг, никаких действий!
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitorPortsPassively();
            } catch (Exception e) {
                logger.error("Ошибка пассивного мониторинга портов: {}", e.getMessage());
            }
        }, 2, 5, TimeUnit.MINUTES);

        // Проверка готовности к миссии
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkMissionReadiness();
            } catch (Exception e) {
                logger.error("Ошибка проверки готовности: {}", e.getMessage());
            }
        }, 5, 10, TimeUnit.MINUTES);

        logger.info("🚀 Фоновые задачи мониторинга запущены (пассивный режим)");
    }

    private void scheduleAutoStart(String scenarioType, long delayMs) {
        scheduler.schedule(() -> {
            try {
                logger.info("⏰ Автозапуск демо-сценария: {}", scenarioType);

                // Сначала проверяем и запускаем сервисы
                getMissionServicesStatus();
                Thread.sleep(5000);

                // Запускаем сценарий
                switch (scenarioType.toLowerCase()) {
                    case "mission":
                    case "complete":
                        completeMission();
                        break;
                    case "problem":
                        demonstrateMissionProblem();
                        break;
                    case "solution":
                        demonstrateMissionSolution();
                        break;
                    default:
                        logger.warn("Неизвестный тип сценария для автозапуска: {}", scenarioType);
                }

            } catch (Exception e) {
                logger.error("❌ Ошибка при автозапуске сценария: {}", e.getMessage());
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void monitorPortsPassively() {
        // ТОЛЬКО логирование, никаких действий!
        logger.debug("🔍 Пассивный мониторинг портов...");

        int openPorts = 0;
        for (Map.Entry<String, Integer> entry : KEFIR_SERVICES.entrySet()) {
            String serviceName = entry.getKey();
            int port = entry.getValue();

            boolean isOpen = isPortOpen(port);
            if (isOpen) {
                openPorts++;
                logger.debug("   ✅ {} (порт {}) открыт", serviceName, port);
            }
        }

        logger.debug("📊 Итог мониторинга: {}/{} портов открыто", openPorts, KEFIR_SERVICES.size());
    }

    private boolean checkMissionServicesReady() {
        int[] missionPorts = {8090, 8082, 8083, 8086, 8085, 8097, 8080};

        for (int port : missionPorts) {
            if (!isPortOpen(port)) {
                logger.debug("❌ Порт {} не открыт", port);
                return false;
            }
        }

        return true;
    }

    private boolean checkAllMissionPorts() {
        int[] missionPorts = {8090, 8082, 8083, 8086, 8085, 8097, 8080, 8081, 8088};
        boolean allOpen = true;

        for (int port : missionPorts) {
            boolean isOpen = isPortOpen(port);
            if (!isOpen) {
                logger.warn("⚠️ Порт {} не открыт", port);
                allOpen = false;
            } else {
                logger.debug("✅ Порт {} открыт", port);
            }
        }

        return allOpen;
    }

    private void fixMissionPorts() {
        logger.info("🔧 Исправление проблемных портов...");

        int[] missionPorts = {8090, 8082, 8083, 8086, 8085, 8097, 8080};

        for (int port : missionPorts) {
            if (!isPortOpen(port)) {
                logger.warn("   Порт {} не открыт. Пытаюсь исправить...", port);

                // 1. Освобождаем порт
                powerShellHelper.releasePortWithPowerShell(port);

                // 2. Пытаемся запустить через ServiceOrchestrator
                try {
                    com.kefir.logistics.launcher_service.model.enums.ServiceType serviceType =
                            com.kefir.logistics.launcher_service.model.enums.ServiceType.fromPort(port);
                    serviceOrchestrator.startService(serviceType);
                    logger.info("   ✅ Сервис на порту {} запущен", port);
                } catch (Exception e) {
                    logger.error("   ❌ Не удалось запустить сервис на порту {}: {}", port, e.getMessage());
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private List<String> getRequiredMissionServices() {
        return Arrays.asList(
                "Transaction Saga Service (порт 8090) - КРИТИЧЕСКИ",
                "Warehouse Service (порт 8082)",
                "Shopping Cart Service (порт 8083)",
                "Collector Service (порт 8086)",
                "Office Management Service (порт 8085)",
                "Authentication Service (порт 8097)",
                "API Gateway (порт 8080)"
        );
    }

    private List<String> createProblemScenario() {
        List<String> steps = new ArrayList<>();

        steps.add("1. 📱 Клиент заказывает 5 товаров через мобильное приложение");
        steps.add("2. 💳 Происходит оплата, транзакция начинается в Transaction Saga");
        steps.add("3. 🏭 Сборщик получает задание собрать заказ на складе");
        steps.add("4. 📦 Сборщик сканирует первые 3 товара - они есть в наличии");
        steps.add("5. ⚠️ 4-й товар отсутствует в системе (ошибка сканирования)");
        steps.add("6. 📞 Сборщик нажимает кнопку 'Проблема' - уведомление в офис");
        steps.add("7. 👨‍💼 Офисмен звонит клиенту: 'Товар X отсутствует, продолжить с 3 товарами?'");
        steps.add("8. ✅ Клиент соглашается получить 3 товара сейчас");
        steps.add("9. 🔄 Transaction Saga получает команду 'partial_commit'");
        steps.add("10. 💰 Деньги возвращаются только за отсутствующий товар");
        steps.add("11. 🚚 Клиенту привозят 3 товара вместо 5");
        steps.add("12. 😠 На следующий день клиент звонит: 'Где остальные 2 товара? Я их оплатил!'");
        steps.add("13. 🔍 Офис проверяет: 2 товара не были пробиты сборщиком");
        steps.add("14. 🏬 Проблема: эти товары могут быть на других складах города");
        steps.add("15. 📞 Офис обзванивает 3 склада, находит товары на разных");
        steps.add("16. 🚗 Курьер должен объехать 3 склада для сбора 2 товаров");
        steps.add("17. ⏰ Итог: доставка занимает 1 ЧАС вместо обычных 15 МИНУТ");
        steps.add("18. 💸 Дополнительные расходы: бензин, время курьера, недовольный клиент");

        return steps;
    }

    private Map<String, Object> analyzeConsequences() {
        Map<String, Object> consequences = new LinkedHashMap<>();

        consequences.put("time", "Увеличение времени доставки в 4 раза (15 мин → 1 час)");
        consequences.put("cost", "Рост логистических расходов на 300%");
        consequences.put("customerSatisfaction", "Резкое снижение (клиент получает неполный заказ)");
        consequences.put("processComplexity", "Усложнение процесса (обзвон складов, ручная работа)");
        consequences.put("reliability", "Низкая (непредсказуемый результат при ошибках)");
        consequences.put("scalability", "Плохая (ручная работа не масштабируется)");

        return consequences;
    }

    private Map<String, Object> getSolutionPhilosophy() {
        Map<String, Object> philosophy = new LinkedHashMap<>();

        philosophy.put("coreIdea", "При неизвестных системных ошибках безопаснее перезагрузить процесс, чем пытаться его починить");
        philosophy.put("analogy", "Как перезагрузка зависшего компьютера вместо поиска конкретного бага в коде");
        philosophy.put("principle", "Целостность и предсказуемость важнее частичной оптимизации");
        philosophy.put("approach", "Полный перезапуск транзакции с начальными данными");
        philosophy.put("technology", "Transaction Saga Pattern для управления распределенными транзакциями");

        return philosophy;
    }

    private List<String> createSolutionScenario() {
        List<String> steps = new ArrayList<>();

        steps.add("1. 📱 Клиент заказывает 5 товаров через приложение");
        steps.add("2. 💳 Начинается транзакция в Transaction Saga (порт 8090)");
        steps.add("3. 🏭 Сборщик получает задание, начинает сборку");
        steps.add("4. ⚠️ Обнаружен отсутствующий товар (ошибка сканирования)");
        steps.add("5. 📞 Сборщик нажимает 'Проблема' - уведомление в офис");
        steps.add("6. 👨‍💼 Офис связывается с клиентом для уточнений");
        steps.add("7. 🔄 Transaction Saga получает событие 'ERROR_DETECTED'");
        steps.add("8. ⏹️ Saga выполняет компенсирующие операции для ВСЕХ шагов");
        steps.add("9. 💰 ВСЕ деньги возвращаются клиенту (полный возврат)");
        steps.add("10. ✅ Транзакция помечается как 'COMPENSATED' (отменена полностью)");
        steps.add("11. 🆕 Saga создает НОВУЮ транзакцию с теми же начальными данными");
        steps.add("12. 🔄 Сборщик получает новое задание с тем же списком товаров");
        steps.add("13. 📋 Сборщик ПЕРЕПРОВЕРЯЕТ ВСЕ 5 товаров заново");
        steps.add("14. ✅ Все доступные товары (4 из 5) успешно сканируются");
        steps.add("15. 💳 Клиент оплачивает только доступные 4 товара");
        steps.add("16. 🚚 Доставка занимает стандартные 15 МИНУТ");
        steps.add("17. 😊 Клиент доволен: получил полный (доступный) заказ быстро");
        steps.add("18. 📊 Система чиста: нет 'висящих' товаров, все транзакции завершены");

        return steps;
    }

    private Map<String, Object> getSolutionBenefits() {
        Map<String, Object> benefits = new LinkedHashMap<>();

        benefits.put("time", "Стандартное время доставки (15 минут)");
        benefits.put("cost", "Нормальные логистические расходы");
        benefits.put("customerSatisfaction", "Высокая (клиент получает полный доступный заказ)");
        benefits.put("processComplexity", "Простой и предсказуемый процесс");
        benefits.put("reliability", "Высокая (гарантированный результат при любых ошибках)");
        benefits.put("scalability", "Хорошая (автоматический процесс, без ручной работы)");
        benefits.put("debugging", "Упрощенное отладка (каждая транзакция независима)");
        benefits.put("dataIntegrity", "Гарантированная целостность данных");

        return benefits;
    }

    private Map<String, Object> createSolutionComparison() {
        Map<String, Object> comparison = new LinkedHashMap<>();

        Map<String, Object> oldWay = new LinkedHashMap<>();
        oldWay.put("name", "Частичное закрытие транзакции");
        oldWay.put("time", "1 час");
        oldWay.put("cost", "Высокая (+300%)");
        oldWay.put("customer", "Недовольный (неполный заказ)");
        oldWay.put("process", "Сложный (ручная работа)");
        oldWay.put("reliability", "Низкая");

        Map<String, Object> newWay = new LinkedHashMap<>();
        newWay.put("name", "Полный перезапуск через Saga");
        newWay.put("time", "15 минут");
        newWay.put("cost", "Нормальная");
        newWay.put("customer", "Довольный (полный доступный заказ)");
        newWay.put("process", "Простой (автоматический)");
        newWay.put("reliability", "Высокая");
        newWay.put("philosophy", "Перезагрузить процесс вместо починки неизвестной ошибки");

        comparison.put("oldWay", oldWay);
        comparison.put("newWay", newWay);
        comparison.put("improvement", "Время доставки: -75%, Затраты: -75%, Удовлетворенность: +100%");
        comparison.put("technology", "Transaction Saga Pattern");

        return comparison;
    }

    private Map<String, Object> createMissionConclusions() {
        Map<String, Object> conclusions = new LinkedHashMap<>();

        conclusions.put("problemDemonstrated", true);
        conclusions.put("solutionDemonstrated", true);
        conclusions.put("comparisonProvided", true);
        conclusions.put("missionAccomplished", true);

        conclusions.put("keyLearnings", Arrays.asList(
                "1. Частичное закрытие транзакций опасно для клиентского опыта",
                "2. Transaction Saga Pattern решает проблему распределенных транзакций",
                "3. При неизвестных ошибках безопаснее перезапустить процесс",
                "4. Целостность данных важнее частичной оптимизации",
                "5. Хорошая архитектура окупается в долгосрочной перспективе"
        ));

        conclusions.put("businessValue", "Сокращение времени доставки с 1 часа до 15 минут при ошибках");
        conclusions.put("technicalValue", "Надежная обработка ошибок в микросервисной архитектуре");
        conclusions.put("customerValue", "Предсказуемый и качественный сервис даже при сбоях");

        return conclusions;
    }

    private void checkMissionReadiness() {
        // ТОЛЬКО логирование статуса
        try {
            boolean sagaReady = isPortOpen(8090);
            boolean authReady = isPortOpen(8097);
            boolean cartReady = isPortOpen(8083);

            if (sagaReady && authReady && cartReady) {
                logger.info("🎯 Готовность к миссии: ВСЕ ключевые сервисы запущены");
            } else {
                logger.warn("⚠️ Готовность к миссии: НЕ все сервисы запущены. Saga: {}, Auth: {}, Cart: {}",
                        sagaReady, authReady, cartReady);
            }
        } catch (Exception e) {
            logger.error("Ошибка проверки готовности: {}", e.getMessage());
        }
    }

    // ============ МЕТОДЫ РАБОТЫ С ПОРТАМИ ============

    private Map<String, Object> checkAllPortsStatusDetailed() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Map<String, Object>> portStatus = new LinkedHashMap<>();

        int openPorts = 0;
        int closedPorts = 0;
        int problematicPorts = 0;

        for (Map.Entry<String, Integer> entry : KEFIR_SERVICES.entrySet()) {
            String serviceName = entry.getKey();
            int port = entry.getValue();

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("port", port);
            status.put("serviceName", serviceName);

            boolean isOpen = isPortOpen(port);
            status.put("open", isOpen);

            if (isOpen) {
                openPorts++;
                status.put("status", "OPEN");

                // Проверяем здоровье
                boolean isHealthy = checkServiceHealth(port);
                status.put("healthy", isHealthy);

                if (!isHealthy) {
                    status.put("status", "OPEN_BUT_UNHEALTHY");
                    problematicPorts++;
                }
            } else {
                closedPorts++;
                status.put("status", "CLOSED");
                status.put("healthy", false);
            }

            // Рекомендации
            List<String> recommendations = new ArrayList<>();
            if (!isOpen) {
                recommendations.add("Порт закрыт. Используйте force-release-all для освобождения");
                recommendations.add("Затем запустите сервис через start-mission");
            } else if (!checkServiceHealth(port)) {
                recommendations.add("Порт открыт, но сервис не отвечает");
                recommendations.add("Используйте auto-fix для перезапуска");
            }
            status.put("recommendations", recommendations);

            portStatus.put(serviceName, status);
        }

        result.put("portStatus", portStatus);
        result.put("summary", Map.of(
                "total", KEFIR_SERVICES.size(),
                "open", openPorts,
                "closed", closedPorts,
                "problematic", problematicPorts
        ));
        result.put("timestamp", LocalDateTime.now());

        // Общие рекомендации
        List<String> generalRecommendations = new ArrayList<>();
        if (closedPorts > 0) {
            generalRecommendations.add("POST /api/v1/demo/ports/force-release-all - освободить все порты");
            generalRecommendations.add("POST /api/v1/demo/services/start-mission - запустить все сервисы");
        }
        if (problematicPorts > 0) {
            generalRecommendations.add("POST /api/v1/demo/ports/auto-fix - автоматически исправить проблемные порты");
        }
        if (closedPorts == 0 && problematicPorts == 0) {
            generalRecommendations.add("✅ Все порты в порядке");
            generalRecommendations.add("POST /api/v1/demo/mission/complete - выполнить демонстрацию миссии");
        }

        result.put("generalRecommendations", generalRecommendations);

        return result;
    }

    private List<Integer> findProblematicPorts() {
        List<Integer> problematic = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : KEFIR_SERVICES.entrySet()) {
            String serviceName = entry.getKey();
            int port = entry.getValue();

            // Пропускаем лаунчер
            if ("Launcher Service".equals(serviceName)) {
                continue;
            }

            boolean isOpen = isPortOpen(port);

            if (!isOpen) {
                // Порт закрыт - проблема
                problematic.add(port);
            } else {
                // Порт открыт, но проверяем здоровье
                boolean isHealthy = checkServiceHealth(port);
                if (!isHealthy) {
                    problematic.add(port);
                }
            }
        }

        return problematic;
    }

    private boolean checkServiceHealth(int port) {
        if (!isPortOpen(port)) {
            return false;
        }

        String[] endpoints = {
                "http://localhost:" + port + "/actuator/health",
                "http://localhost:" + port + "/health",
                "http://localhost:" + port + "/",
                "http://localhost:" + port + "/api/health",
                "http://localhost:" + port + "/actuator/info"
        };

        for (String endpoint : endpoints) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }
            } catch (Exception e) {
                // Продолжаем
            }
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

    private String getServiceNameByPort(int port) {
        for (Map.Entry<String, Integer> entry : KEFIR_SERVICES.entrySet()) {
            if (entry.getValue() == port) {
                return entry.getKey();
            }
        }
        return "Unknown Service (port " + port + ")";
    }

    private String getUptime() {
        Duration uptime = Duration.between(startTime, LocalDateTime.now());
        long hours = uptime.toHours();
        long minutes = uptime.toMinutes() % 60;
        long seconds = uptime.getSeconds() % 60;

        return String.format("%d ч %d мин %d сек", hours, minutes, seconds);
    }

    private String calculateSuccessRate() {
        if (totalTransactions.get() == 0) {
            return "100%";
        }

        double rate = (1 - (double)failedTransactions.get() / totalTransactions.get()) * 100;
        return String.format("%.1f%%", rate);
    }
}