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
import org.springframework.stereotype.Service;
// В начале ServiceOrchestrator.java добавить:
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ServiceOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ServiceOrchestrator.class);

    @Autowired
    private PowerShellHelper powerShellHelper;

    @Value("${app.services.baseDir:C:\\Users\\2oleg\\Downloads\\Persona 5 Royal (2022)\\KefirInc\\Backend}")
    private String backendBaseDir;

    @Value("${app.frontend.dir:C:\\Users\\2oleg\\Downloads\\Persona 5 Royal (2022)\\KefirInc\\kefir-react-app}")
    private String frontendDir;

    // Порты для освобождения и проверки
    private static final int[] ALL_KEFIR_PORTS = {8080, 8097, 8081, 8082, 8084, 8083, 8085, 3000};

    // Сервисы в порядке запуска (как в .bat файле)
    private static final ServiceConfig[] BACKEND_SERVICES = {
            new ServiceConfig("ApiGateway", 8080),
            new ServiceConfig("Auth", 8097),
            new ServiceConfig("User", 8081),
            new ServiceConfig("Sklad", 8082),
            new ServiceConfig("Collector", 8084),
            new ServiceConfig("Backet", 8083),
            new ServiceConfig("Office", 8085)
    };

    // Класс для конфигурации сервиса
    private static class ServiceConfig {
        String name;
        int port;
        String directory;

        ServiceConfig(String name, int port) {
            this.name = name;
            this.port = port;
        }

        String getFullPath(String baseDir) {
            return baseDir + "\\" + name;
        }
    }

    // Статус фронтенда
    private static class FrontendConfig {
        static final String NAME = "kefir-react-app";
        static final int PORT = 3000;
        static final String START_COMMAND = "npm start";
    }

    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @PostConstruct
    public void init() {
        logger.info("=== KEFIR SERVICE ORCHESTRATOR INITIALIZED ===");
        logger.info("Backend directory: {}", backendBaseDir);
        logger.info("Frontend directory: {}", frontendDir);
        logger.info("Managing {} backend services + 1 frontend", BACKEND_SERVICES.length);
    }

    /**
     * ГЛАВНЫЙ МЕТОД: Запуск всей системы KEFIR
     */
    public Map<String, Object> startCompleteSystem() {
        logger.info("🚀🚀🚀 ЗАПУСК ВСЕЙ СИСТЕМЫ KEFIR ===");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operation", "startCompleteSystem");
        result.put("timestamp", LocalDateTime.now());
        result.put("step1", "Освобождение портов");
        result.put("step2", "Запуск бекенд сервисов");
        result.put("step3", "Запуск фронтенда");
        result.put("step4", "Верификация");

        try {
            // ШАГ 1: АГРЕССИВНОЕ ОСВОБОЖДЕНИЕ ВСЕХ ПОРТОВ
            Map<String, Object> portRelease = aggressivelyReleaseAllPorts();
            result.put("portReleaseResult", portRelease);

            // Ждем после освобождения портов
            Thread.sleep(3000);

            // ШАГ 2: ЗАПУСК БЕКЕНД-СЕРВИСОВ
            Map<String, Object> backendStart = startAllBackendServices();
            result.put("backendStartResult", backendStart);

            // Даем время на запуск бекенда
            logger.info("⏳ Ожидание запуска бекенд-сервисов (15 секунд)...");
            Thread.sleep(15000);

            // ШАГ 3: ЗАПУСК ФРОНТЕНДА
            Map<String, Object> frontendStart = startFrontendApplication();
            result.put("frontendStartResult", frontendStart);

            // Пауза для запуска фронтенда
            logger.info("⏳ Ожидание запуска фронтенда (10 секунд)...");
            Thread.sleep(10000);

            // ШАГ 4: ВЕРИФИКАЦИЯ
            Map<String, Object> verification = verifySystemStartup();
            result.put("verificationResult", verification);

            // ФИНАЛЬНЫЙ СТАТУС
            boolean allServicesRunning = (boolean) verification.getOrDefault("allRunning", false);
            result.put("status", allServicesRunning ? "SYSTEM_STARTED_SUCCESSFULLY" : "SYSTEM_STARTED_PARTIALLY");
            result.put("success", allServicesRunning);
            result.put("message", allServicesRunning ?
                    "✅ Вся система KEFIR успешно запущена и готова к работе!" :
                    "⚠️ Система запущена частично. Проверьте логи проблемных сервисов.");

            // ЛОГИРОВАНИЕ ИТОГОВ
            logFinalResults(result);

        } catch (Exception e) {
            logger.error("❌ КРИТИЧЕСКАЯ ОШИБКА при запуске системы: {}", e.getMessage(), e);
            result.put("status", "STARTUP_FAILED");
            result.put("error", e.getMessage());
            result.put("success", false);
        }

        return result;
    }

    /**
     * ШАГ 1: Агрессивное освобождение всех портов
     */
    /**
     * ШАГ 1: Агрессивное освобождение всех портов
     */
    private Map<String, Object> aggressivelyReleaseAllPorts() {
        logger.info("🔴 ШАГ 1: АГРЕССИВНОЕ ОСВОБОЖДЕНИЕ {} ПОРТОВ", ALL_KEFIR_PORTS.length);

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> released = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        // Останавливаем все текущие процессы
        stopAllRunningProcesses();
        logger.info("Все текущие процессы остановлены");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Прервано ожидание после остановки процессов");
        }

        // Освобождаем каждый порт через PowerShell
        for (int port : ALL_KEFIR_PORTS) {
            String serviceName = getServiceNameByPort(port);

            try {
                logger.debug("Освобождаю порт {} ({})...", port, serviceName);

                // Пробуем освободить порт
                boolean success = powerShellHelper.releasePortWithPowerShell(port);

                if (success) {
                    String msg = String.format("✅ Порт %d (%s) освобожден", port, serviceName);
                    released.add(msg);
                    logger.info(msg);
                } else {
                    // Пробуем еще раз с большей агрессивностью
                    logger.warn("Порт {} не освободился с первого раза, пробую еще...", port);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    success = powerShellHelper.releasePortWithPowerShell(port);

                    if (success) {
                        String msg = String.format("⚠️ Порт %d (%s) освобожден со второй попытки", port, serviceName);
                        released.add(msg);
                        logger.info(msg);
                    } else {
                        String msg = String.format("❌ Порт %d (%s) не удалось освободить", port, serviceName);
                        failed.add(msg);
                        logger.error(msg);
                    }
                }

                try {
                    Thread.sleep(500); // Пауза между портами
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            } catch (Exception e) {
                String errorMsg = String.format("❌ Ошибка освобождения порта %d: %s", port, e.getMessage());
                failed.add(errorMsg);
                logger.error(errorMsg);
            }
        }

        result.put("releasedPorts", released);
        result.put("failedPorts", failed);
        result.put("totalAttempted", ALL_KEFIR_PORTS.length);
        result.put("successful", released.size());
        result.put("failed", failed.size());
        result.put("timestamp", LocalDateTime.now());

        logger.info("📊 Освобождение портов завершено: {}/{} успешно", released.size(), ALL_KEFIR_PORTS.length);

        return result;
    }

    /**
     * ШАГ 2: Запуск всех бекенд-сервисов через Maven
     */
    private Map<String, Object> startAllBackendServices() {
        logger.info("🟢 ШАГ 2: ЗАПУСК {} БЕКЕНД-СЕРВИСОВ", BACKEND_SERVICES.length);

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> serviceResults = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (ServiceConfig service : BACKEND_SERVICES) {
            Map<String, Object> serviceResult = new LinkedHashMap<>();
            serviceResult.put("service", service.name);
            serviceResult.put("port", service.port);

            try {
                String servicePath = service.getFullPath(backendBaseDir);
                serviceResult.put("directory", servicePath);

                // Проверка существования директории
                File dir = new File(servicePath);
                if (!dir.exists()) {
                    serviceResult.put("status", "FAILED");
                    serviceResult.put("error", "Директория не найдена");
                    failed++;
                    logger.error("❌ {}: директория не найдена - {}", service.name, servicePath);
                    serviceResults.add(serviceResult);
                    continue;
                }

                // Проверка наличия Maven wrapper
                File mvnw = new File(servicePath + "\\mvnw.cmd");
                if (!mvnw.exists()) {
                    serviceResult.put("status", "FAILED");
                    serviceResult.put("error", "mvnw.cmd не найден");
                    failed++;
                    logger.error("❌ {}: mvnw.cmd не найден", service.name);
                    serviceResults.add(serviceResult);
                    continue;
                }

                // Запуск сервиса через Maven (как в .bat файле)
                logger.info("🚀 Запуск {} (порт {})...", service.name, service.port);

                ProcessBuilder pb = new ProcessBuilder(
                        "cmd", "/c",
                        "cd", "/d", servicePath,
                        "&&",
                        "mvnw.cmd", "spring-boot:run",
                        "-Dspring-boot.run.profiles=local",
                        "-Dserver.port=" + service.port,
                        "-DskipTests"
                );

                pb.directory(dir);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                runningProcesses.put(service.name, process);

                // Запуск монитора вывода
                startOutputMonitor(service.name, process);

                serviceResult.put("status", "STARTED");
                serviceResult.put("pid", process.pid());
                serviceResult.put("command", "mvn spring-boot:run");
                successful++;

                logger.info("✅ {} запущен (PID: {}, порт: {})", service.name, process.pid(), service.port);

                // Пауза между запусками (как в .bat файле - 2 секунды)
                if (!service.name.equals(BACKEND_SERVICES[BACKEND_SERVICES.length - 1].name)) {
                    Thread.sleep(2000);
                }

            } catch (Exception e) {
                serviceResult.put("status", "FAILED");
                serviceResult.put("error", e.getMessage());
                failed++;
                logger.error("❌ Ошибка запуска {}: {}", service.name, e.getMessage());
            }

            serviceResults.add(serviceResult);
        }

        result.put("services", serviceResults);
        result.put("total", BACKEND_SERVICES.length);
        result.put("successful", successful);
        result.put("failed", failed);
        result.put("successRate", String.format("%.1f%%", (successful * 100.0 / BACKEND_SERVICES.length)));
        result.put("timestamp", LocalDateTime.now());

        logger.info("📊 Бекенд сервисов: {}/{} успешно запущено", successful, BACKEND_SERVICES.length);

        return result;
    }

    /**
     * ШАГ 3: Запуск фронтенд-приложения
     */
    private Map<String, Object> startFrontendApplication() {
        logger.info("🔵 ШАГ 3: ЗАПУСК ФРОНТЕНДА (React)");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", FrontendConfig.NAME);
        result.put("port", FrontendConfig.PORT);
        result.put("directory", frontendDir);
        result.put("command", FrontendConfig.START_COMMAND);

        try {
            // Проверка директории
            File dir = new File(frontendDir);
            if (!dir.exists()) {
                result.put("status", "FAILED");
                result.put("error", "Директория фронтенда не найдена: " + frontendDir);
                logger.error("❌ Фронтенд: директория не найдена");
                return result;
            }

            // Проверка package.json
            File packageJson = new File(frontendDir + "\\package.json");
            if (!packageJson.exists()) {
                result.put("status", "FAILED");
                result.put("error", "package.json не найден");
                logger.error("❌ Фронтенд: package.json не найден");
                return result;
            }

            // Дополнительная проверка порта 3000
            if (isPortOpen(FrontendConfig.PORT)) {
                logger.warn("⚠️ Порт {} занят, освобождаю...", FrontendConfig.PORT);
                powerShellHelper.releasePortWithPowerShell(FrontendConfig.PORT);
                Thread.sleep(3000);
            }

            // Запуск npm start
            logger.info("Запуск npm start в {}...", frontendDir);

            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c",
                    "cd", "/d", frontendDir,
                    "&&",
                    "npm", "start"
            );

            pb.directory(dir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            runningProcesses.put(FrontendConfig.NAME, process);

            // Запуск монитора вывода
            startOutputMonitor(FrontendConfig.NAME, process);

            result.put("status", "STARTED");
            result.put("pid", process.pid());
            result.put("success", true);

            logger.info("✅ Фронтенд запущен (PID: {}, порт: {})", process.pid(), FrontendConfig.PORT);

        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("success", false);
            logger.error("❌ Ошибка запуска фронтенда: {}", e.getMessage());
        }

        return result;
    }

    /**
     * ШАГ 4: Верификация запуска всей системы
     */
    private Map<String, Object> verifySystemStartup() {
        logger.info("📊 ШАГ 4: ВЕРИФИКАЦИЯ ЗАПУСКА СИСТЕМЫ");

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Boolean> portStatus = new LinkedHashMap<>();

        // Проверяем бекенд-сервисы
        for (ServiceConfig service : BACKEND_SERVICES) {
            boolean isOpen = isPortOpen(service.port);
            portStatus.put(service.name + " (порт " + service.port + ")", isOpen);

            if (isOpen) {
                logger.debug("✅ {}: порт {} открыт", service.name, service.port);
            } else {
                logger.warn("⚠️ {}: порт {} НЕ открыт", service.name, service.port);
            }
        }

        // Проверяем фронтенд
        boolean frontendOpen = isPortOpen(FrontendConfig.PORT);
        portStatus.put(FrontendConfig.NAME + " (порт " + FrontendConfig.PORT + ")", frontendOpen);

        // Подсчет результатов
        long openPorts = portStatus.values().stream().filter(v -> v).count();
        boolean allRunning = openPorts == portStatus.size();

        result.put("portStatus", portStatus);
        result.put("totalPorts", portStatus.size());
        result.put("openPorts", openPorts);
        result.put("closedPorts", portStatus.size() - openPorts);
        result.put("allRunning", allRunning);
        result.put("timestamp", LocalDateTime.now());

        // Рекомендации
        if (allRunning) {
            result.put("message", "🎉 ВСЯ СИСТЕМА KEFIR УСПЕШНО ЗАПУЩЕНА!");
            result.put("nextSteps", Arrays.asList(
                    "1. Откройте браузер: http://localhost:3000",
                    "2. Войдите в систему с демо-учетными данными",
                    "3. Протестируйте функционал"
            ));
        } else {
            List<String> recommendations = new ArrayList<>();
            recommendations.add("⚠️ Не все сервисы запущены");

            for (Map.Entry<String, Boolean> entry : portStatus.entrySet()) {
                if (!entry.getValue()) {
                    recommendations.add("❌ " + entry.getKey() + " не отвечает");
                }
            }

            recommendations.add("Проверьте логи проблемных сервисов");
            recommendations.add("Попробуйте перезапустить: POST /api/v1/services/start-complete");

            result.put("recommendations", recommendations);
        }

        logger.info("📊 Верификация: {}/{} портов открыто", openPorts, portStatus.size());

        return result;
    }

    /**
     * Мониторинг вывода процесса
     */
    private void startOutputMonitor(String serviceName, Process process) {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    // Важные сообщения логируем
                    if (line.contains("Started") && line.contains("seconds")) {
                        logger.info("🎉 {} успешно запущен за {}",
                                serviceName, extractStartupTime(line));
                    }

                    if (line.contains("ERROR") || line.contains("Failed to start")) {
                        logger.error("❌ {}: {}", serviceName, line);
                    }

                    if (line.contains("Tomcat started on port")) {
                        logger.info("🌐 {} запущен на порту", serviceName);
                    }

                    // Специальные логи для фронтенда
                    if (serviceName.equals(FrontendConfig.NAME)) {
                        if (line.contains("Compiled successfully") || line.contains("Local:")) {
                            logger.info("⚛️  Фронтенд: {}", line);
                        }
                        if (line.contains("Failed to compile")) {
                            logger.error("❌ Фронтенд ошибка компиляции: {}", line);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Ошибка чтения вывода {}: {}", serviceName, e.getMessage());
            }
        });
    }

    /**
     * Остановка всех запущенных процессов
     */
    private void stopAllRunningProcesses() {
        logger.info("🛑 Остановка всех запущенных процессов...");

        int stoppedCount = 0;
        for (Map.Entry<String, Process> entry : runningProcesses.entrySet()) {
            Process process = entry.getValue();
            if (process != null && process.isAlive()) {
                try {
                    process.destroy();
                    if (process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.debug("✅ Остановлен: {}", entry.getKey());
                        stoppedCount++;
                    } else {
                        process.destroyForcibly();
                        logger.warn("⚠️ Принудительно остановлен: {}", entry.getKey());
                        stoppedCount++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("❌ Ошибка остановки {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }

        runningProcesses.clear();
        logger.info("Остановлено процессов: {}", stoppedCount);
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 1500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String getServiceNameByPort(int port) {
        for (ServiceConfig service : BACKEND_SERVICES) {
            if (service.port == port) {
                return service.name;
            }
        }
        if (port == FrontendConfig.PORT) {
            return FrontendConfig.NAME;
        }
        return "Unknown (port " + port + ")";
    }

    private String extractStartupTime(String logLine) {
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
        return "неизвестное время";
    }

    private void logFinalResults(Map<String, Object> result) {
        logger.info("================================================");
        logger.info("🚀 ЗАПУСК ВСЕЙ СИСТЕМЫ KEFIR ЗАВЕРШЕН");

        Map<String, Object> backendResult = (Map<String, Object>) result.get("backendStartResult");
        Map<String, Object> frontendResult = (Map<String, Object>) result.get("frontendStartResult");
        Map<String, Object> verification = (Map<String, Object>) result.get("verificationResult");

        if (backendResult != null) {
            logger.info("📊 Бекенд: {}/{} сервисов",
                    backendResult.get("successful"), backendResult.get("total"));
        }

        if (frontendResult != null) {
            logger.info("📊 Фронтенд: {}",
                    frontendResult.get("success").equals(true) ? "✅ Запущен" : "❌ Не запущен");
        }

        if (verification != null) {
            logger.info("📊 Порты: {}/{} открыто",
                    verification.get("openPorts"), verification.get("totalPorts"));
        }

        logger.info("📈 Статус: {}", result.get("status"));
        logger.info("================================================");
    }

    // ============ PUBLIC API МЕТОДЫ ============

    /**
     * Остановить всю систему
     */
    public Map<String, Object> stopCompleteSystem() {
        logger.info("🛑 ОСТАНОВКА ВСЕЙ СИСТЕМЫ KEFIR");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operation", "stopCompleteSystem");
        result.put("timestamp", LocalDateTime.now());

        stopAllRunningProcesses();

        // Освобождаем порты
        try {
            for (int port : ALL_KEFIR_PORTS) {
                powerShellHelper.releasePortWithPowerShell(port);
                Thread.sleep(200);
            }
        } catch (Exception e) {
            logger.error("Ошибка освобождения портов: {}", e.getMessage());
        }

        result.put("status", "SYSTEM_STOPPED");
        result.put("message", "Вся система KEFIR остановлена");
        result.put("portsReleased", Arrays.toString(ALL_KEFIR_PORTS));

        logger.info("✅ Вся система KEFIR остановлена");

        return result;
    }

    /**
     * Получить статус системы
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Boolean> serviceStatus = new LinkedHashMap<>();

        // Проверяем бекенд
        for (ServiceConfig service : BACKEND_SERVICES) {
            boolean isRunning = isPortOpen(service.port);
            serviceStatus.put(service.name + ":" + service.port, isRunning);
        }

        // Проверяем фронтенд
        boolean frontendRunning = isPortOpen(FrontendConfig.PORT);
        serviceStatus.put(FrontendConfig.NAME + ":" + FrontendConfig.PORT, frontendRunning);

        // Подсчет
        long runningCount = serviceStatus.values().stream().filter(v -> v).count();
        boolean allRunning = runningCount == serviceStatus.size();

        result.put("services", serviceStatus);
        result.put("totalServices", serviceStatus.size());
        result.put("runningServices", runningCount);
        result.put("allRunning", allRunning);
        result.put("systemReady", allRunning);
        result.put("timestamp", LocalDateTime.now());

        if (allRunning) {
            result.put("status", "SYSTEM_RUNNING");
            result.put("message", "✅ Вся система KEFIR запущена и работает");
        } else {
            result.put("status", "SYSTEM_PARTIAL");
            result.put("message", "⚠️ Система работает частично");
        }

        return result;
    }
    // В ServiceOrchestrator.java добавляем:

    /**
     * Запуск отдельного сервиса (для обратной совместимости)
     */
    public ServiceStatusDTO startService(ServiceType serviceType) {
        logger.info("🚀 Запуск отдельного сервиса: {}", serviceType.getDisplayName());

        try {
            String serviceName = getServiceNameByType(serviceType);
            Integer port = getPortByServiceType(serviceType);

            if (serviceName == null || port == null) {
                return createErrorStatus(serviceType, "Сервис не поддерживается в новой системе");
            }

            // Освобождаем порт если занят
            if (isPortOpen(port)) {
                powerShellHelper.releasePortWithPowerShell(port);
                safeSleep(2000);
            }

            // Запускаем сервис
            String servicePath = backendBaseDir + "\\" + serviceName;
            Process process = startMavenService(serviceName, servicePath, port);

            if (process != null) {
                runningProcesses.put(serviceName, process);
                startOutputMonitor(serviceName, process);

                // Ждем запуска
                safeSleep(5000);

                ServiceStatusDTO status = ServiceStatusDTO.builder()
                        .serviceType(serviceType)
                        .state(isPortOpen(port) ? ServiceState.RUNNING : ServiceState.FAILED)
                        .serviceName(serviceType.getDisplayName())
                        .port(port)
                        .pid((int) process.pid())
                        .portOpen(isPortOpen(port))
                        .lastChecked(LocalDateTime.now())
                        .build();

                logger.info("✅ Сервис {} запущен на порту {}", serviceName, port);
                return status;
            }

            return createErrorStatus(serviceType, "Не удалось запустить процесс");

        } catch (Exception e) {
            logger.error("❌ Ошибка запуска сервиса {}: {}", serviceType, e.getMessage());
            return createErrorStatus(serviceType, e.getMessage());
        }
    }

    /**
     * Остановка отдельного сервиса
     */
    public ServiceStatusDTO stopService(ServiceType serviceType) {
        logger.info("🛑 Остановка сервиса: {}", serviceType.getDisplayName());

        try {
            String serviceName = getServiceNameByType(serviceType);

            if (serviceName == null) {
                return createErrorStatus(serviceType, "Сервис не найден");
            }

            Process process = runningProcesses.get(serviceName);
            if (process != null && process.isAlive()) {
                process.destroy();
                if (process.waitFor(5, TimeUnit.SECONDS)) {
                    runningProcesses.remove(serviceName);
                    logger.info("✅ Сервис {} остановлен", serviceName);
                } else {
                    process.destroyForcibly();
                    logger.warn("⚠️ Сервис {} принудительно остановлен", serviceName);
                }
            }

            // Освобождаем порт
            Integer port = getPortByServiceType(serviceType);
            if (port != null) {
                powerShellHelper.releasePortWithPowerShell(port);
            }

            return ServiceStatusDTO.builder()
                    .serviceType(serviceType)
                    .state(ServiceState.STOPPED)
                    .serviceName(serviceType.getDisplayName())
                    .lastChecked(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            logger.error("❌ Ошибка остановки сервиса {}: {}", serviceType, e.getMessage());
            return createErrorStatus(serviceType, e.getMessage());
        }
    }

    /**
     * Получение статуса отдельного сервиса
     */
    public ServiceStatusDTO getServiceStatus(ServiceType serviceType) {
        String serviceName = getServiceNameByType(serviceType);
        Integer port = getPortByServiceType(serviceType);

        if (serviceName == null || port == null) {
            return createErrorStatus(serviceType, "Сервис не найден");
        }

        boolean isRunning = isPortOpen(port);
        Process process = runningProcesses.get(serviceName);

        return ServiceStatusDTO.builder()
                .serviceType(serviceType)
                .state(isRunning ? ServiceState.RUNNING : ServiceState.STOPPED)
                .serviceName(serviceType.getDisplayName())
                .port(port)
                .portOpen(isRunning)
                .pid(process != null ? (int) process.pid() : null)
                .lastChecked(LocalDateTime.now())
                .build();
    }

    // Вспомогательные методы
    private String getServiceNameByType(ServiceType serviceType) {
        switch (serviceType) {
            case API_GATEWAY: return "ApiGateway";
            case AUTH_SERVICE: return "Auth";
            case USER_SERVICE: return "User";
            case SKLAD_SERVICE: return "Sklad";
            case COLLECTOR_SERVICE: return "Collector";
            case BACKET_SERVICE: return "Backet";
            case OFFICE_SERVICE: return "Office";
            default: return null;
        }
    }

    private Integer getPortByServiceType(ServiceType serviceType) {
        switch (serviceType) {
            case API_GATEWAY: return 8080;
            case AUTH_SERVICE: return 8097;
            case USER_SERVICE: return 8081;
            case SKLAD_SERVICE: return 8082;
            case COLLECTOR_SERVICE: return 8084;
            case BACKET_SERVICE: return 8083;
            case OFFICE_SERVICE: return 8085;
            default: return null;
        }
    }

    private Process startMavenService(String serviceName, String servicePath, int port) throws IOException {
        File dir = new File(servicePath);
        if (!dir.exists()) {
            logger.error("❌ Директория не найдена: {}", servicePath);
            return null;
        }

        ProcessBuilder pb = new ProcessBuilder(
                "cmd", "/c",
                "cd", "/d", servicePath,
                "&&",
                "mvnw.cmd", "spring-boot:run",
                "-Dspring-boot.run.profiles=local",
                "-Dserver.port=" + port,
                "-DskipTests"
        );

        pb.directory(dir);
        pb.redirectErrorStream(true);

        return pb.start();
    }

    private ServiceStatusDTO createErrorStatus(ServiceType serviceType, String error) {
        return ServiceStatusDTO.builder()
                .serviceType(serviceType)
                .state(ServiceState.FAILED)
                .serviceName(serviceType.getDisplayName())
                .errorMessage(error)
                .lastChecked(LocalDateTime.now())
                .build();
    }

    private void safeSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}