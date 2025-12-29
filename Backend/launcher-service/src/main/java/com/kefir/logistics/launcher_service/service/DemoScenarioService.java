package com.kefir.logistics.launcher_service.service;

import com.kefir.logistics.launcher_service.model.dto.DemoScenarioDTO;
import com.kefir.logistics.launcher_service.model.enums.DemoScenarioType;
import com.kefir.logistics.launcher_service.model.enums.ErrorType;
import com.kefir.logistics.launcher_service.model.enums.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DemoScenarioService {

    private static final Logger logger = LoggerFactory.getLogger(DemoScenarioService.class);

    @Autowired
    private ServiceOrchestrator serviceOrchestrator;

    private final Map<String, DemoScenarioDTO> activeScenarios = new HashMap<>();

    /**
     * Главный демо-сценарий: Каскадные ошибки при отсутствии товаров
     */
    public DemoScenarioDTO runCascadeErrorsDemo() {
        logger.info("🚀 Запуск демо: КАСКАДНЫЕ ОШИБКИ");

        DemoScenarioDTO scenario = createCascadeErrorsScenario();
        activeScenarios.put(scenario.getScenarioType().name(), scenario);

        // Запускаем в отдельном потоке
        new Thread(() -> executeCascadeErrorsScenario(scenario)).start();

        return scenario;
    }

    private DemoScenarioDTO createCascadeErrorsScenario() {
        DemoScenarioDTO scenario = new DemoScenarioDTO();
        scenario.setScenarioType(DemoScenarioType.CASCADE_ERRORS);
        scenario.setTitle("Каскадные ошибки при отсутствии товаров");
        scenario.setDescription("Демонстрация реальной проблемы логистики: " +
                "один отсутствующий товар вызывает цепочку проблем, приводящую " +
                "к доставке только 2 из 6 товаров клиенту.");
        scenario.setStartTime(LocalDateTime.now());
        scenario.setRunning(true);

        // Шаги сценария
        List<String> steps = new ArrayList<>();
        steps.add("1️⃣ Подготовка: Создаем скрытые дефициты товаров");
        steps.add("2️⃣ Клиент создает заказ на 6 товаров");
        steps.add("3️⃣ Система показывает 'все товары в наличии' (ложь!)");
        steps.add("4️⃣ Сборщик обнаруживает отсутствие йогурта");
        steps.add("5️⃣ 15-минутное ожидание: офис связывается с клиентом");
        steps.add("6️⃣ КАТАСТРОФА: За это время другие клиенты забирают товары");
        steps.add("7️⃣ Результат: Клиент получает только 2 из 6 товаров");
        steps.add("8️⃣ Финансовые потери: 1,689 руб из 2,184 руб");
        scenario.setSteps(steps);

        // Симулируемые ошибки
        List<ErrorType> errors = Arrays.asList(
                ErrorType.PRODUCT_NOT_FOUND,
                ErrorType.LOW_STOCK,
                ErrorType.TRANSACTION_TIMEOUT
        );
        scenario.setSimulatedErrors(errors);

        // Тестовые данные
        Map<String, Object> testData = new HashMap<>();
        testData.put("orderId", "ORDER-" + System.currentTimeMillis());
        testData.put("clientId", "CLIENT-DEMO-001");
        testData.put("collectorId", "COLLECTOR-DEMO-001");
        testData.put("products", Arrays.asList("Молоко", "Хлеб", "Йогурт", "Яйца", "Сыр", "Масло"));
        testData.put("quantities", Arrays.asList(2, 1, 4, 10, 5, 3));
        scenario.setTestData(testData);

        return scenario;
    }

    private void executeCascadeErrorsScenario(DemoScenarioDTO scenario) {
        try {
            logger.info("=== НАЧАЛО ДЕМО: КАСКАДНЫЕ ОШИБКИ ===");

            // Шаг 1: Подготовка сервисов
            scenario.getSteps().set(0, "✅ " + scenario.getSteps().get(0));
            logger.info("Шаг 1: Подготавливаем сервисы...");

            // Запускаем необходимые сервисы
            serviceOrchestrator.startService(ServiceType.SKLAD_SERVICE);           // ← ИСПРАВЛЕНО
            serviceOrchestrator.startService(ServiceType.BACKET_SERVICE);          // ← уже правильно
            serviceOrchestrator.startService(ServiceType.COLLECTOR_SERVICE);       // ← ИСПРАВЛЕНО
            serviceOrchestrator.startService(ServiceType.OFFICE_SERVICE);

            Thread.sleep(5000);

            // Шаг 2: Создаем скрытые дефициты
            scenario.getSteps().set(1, "✅ " + scenario.getSteps().get(1));
            logger.info("Шаг 2: Создаем скрытые дефициты товаров...");
            logger.info("  - Йогурт: 0 шт (явный дефицит)");
            logger.info("  - Яйца: 1 из 10 (скрытый дефицит)");
            logger.info("  - Сыр: 2 из 5 (скрытый дефицит)");

            Thread.sleep(3000);

            // Шаг 3: Клиент создает заказ
            scenario.getSteps().set(2, "✅ " + scenario.getSteps().get(2));
            logger.info("Шаг 3: Клиент создает заказ на 6 товаров...");
            logger.info("  Система некорректно показывает: 'Все товары в наличии'");

            Thread.sleep(3000);

            // Шаг 4: Сборщик обнаруживает проблему
            scenario.getSteps().set(3, "✅ " + scenario.getSteps().get(3));
            logger.info("Шаг 4: Сборщик начинает сборку...");
            logger.info("  - Молоко ✓");
            logger.info("  - Хлеб ✓");
            logger.info("  - Йогурт ❌ НЕ НАЙДЕН!");

            // Симулируем отправку уведомления в офис
            logger.info("  📨 Уведомление отправлено в офис");

            Thread.sleep(3000);

            // Шаг 5: Ожидание и звонок клиенту
            scenario.getSteps().set(4, "✅ " + scenario.getSteps().get(4));
            logger.info("Шаг 5: 15-минутное ожидание...");
            logger.info("  📞 Офис звонит клиенту: 'Йогурта нет. Продолжить?'");
            logger.info("  👤 Клиент: 'Да, продолжайте без йогурта'");

            Thread.sleep(2000);

            // Шаг 6: Катастрофа - другие клиенты забирают товары
            scenario.getSteps().set(5, "🔥 " + scenario.getSteps().get(5));
            logger.info("Шаг 6: КАТАСТРОФА во время ожидания!");
            logger.info("  Другие клиенты забрали товары:");
            logger.info("  - Яйца: было 10, стало 3 (не хватает 7)");
            logger.info("  - Сыр: было 5, стало 1 (не хватает 4)");
            logger.info("  - Масло: было 3, стало 1 (не хватает 2)");

            Thread.sleep(3000);

            // Шаг 7: Плачевный результат
            scenario.getSteps().set(6, "💥 " + scenario.getSteps().get(6));
            logger.info("Шаг 7: ИТОГОВЫЙ РЕЗУЛЬТАТ:");
            logger.info("  ┌─────────────────────────────┐");
            logger.info("  │  ОЖИДАЛОСЬ: 6 товаров      │");
            logger.info("  │  ПОЛУЧЕНО: 2 товара        │");
            logger.info("  │  УСПЕШНОСТЬ: 33%           │");
            logger.info("  └─────────────────────────────┘");
            logger.info("  Доставлено: только Молоко и Хлеб");

            // Шаг 8: Финансовые потери
            scenario.getSteps().set(7, "💰 " + scenario.getSteps().get(7));
            Map<String, Object> results = new HashMap<>();
            results.put("totalOrderValue", 2184.87);
            results.put("deliveredValue", 335.48);
            results.put("losses", 1849.39);
            results.put("lossPercentage", 84.6);
            results.put("timeLost", "45+ минут");
            results.put("clientSatisfaction", "РАЗГНЕВАН");

            scenario.setResults(results);

            logger.info("Шаг 8: ФИНАНСОВЫЕ ПОТЕРИ:");
            logger.info("  - Стоимость заказа: 2,184.87 руб");
            logger.info("  - Доставлено на: 335.48 руб");
            logger.info("  - ПОТЕРИ: 1,849.39 руб (84.6%)");
            logger.info("  - Время: 45+ минут");
            logger.info("  - Клиент: РАЗГНЕВАН");

            // Завершение сценария
            scenario.setEndTime(LocalDateTime.now());
            scenario.setRunning(false);

            logger.info("=== ЗАВЕРШЕНИЕ ДЕМО ===");
            logger.info("📊 Демо успешно завершено за {} секунд",
                    java.time.Duration.between(scenario.getStartTime(), scenario.getEndTime()).getSeconds());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Демо прервано: {}", e.getMessage());
            scenario.setRunning(false);
        }
    }

    /**
     * Другие демо-сценарии
     */
    public DemoScenarioDTO runNormalProcessDemo() {
        DemoScenarioDTO scenario = new DemoScenarioDTO();
        scenario.setScenarioType(DemoScenarioType.NORMAL_PROCESS);
        scenario.setTitle("Нормальный процесс заказа");
        scenario.setDescription("Успешный заказ без ошибок");
        // ... реализация
        return scenario;
    }

    public DemoScenarioDTO runSingleMissingItemDemo() {
        DemoScenarioDTO scenario = new DemoScenarioDTO();
        scenario.setScenarioType(DemoScenarioType.SINGLE_MISSING_ITEM);
        scenario.setTitle("Один отсутствующий товар");
        scenario.setDescription("Клиент соглашается на заказ без одного товара");
        // ... реализация
        return scenario;
    }

    public List<DemoScenarioDTO> getAllScenarios() {
        return Arrays.asList(
                createCascadeErrorsScenario(),
                createScenario(DemoScenarioType.NORMAL_PROCESS),
                createScenario(DemoScenarioType.SINGLE_MISSING_ITEM),
                createScenario(DemoScenarioType.CLIENT_DEMANDS_ALL),
                createScenario(DemoScenarioType.NIGHTMARE_SCENARIO)
        );
    }

    private DemoScenarioDTO createScenario(DemoScenarioType type) {
        DemoScenarioDTO scenario = new DemoScenarioDTO();
        scenario.setScenarioType(type);
        scenario.setTitle(type.getDescription());
        scenario.setRunning(false);
        return scenario;
    }

    public DemoScenarioDTO getActiveScenario(String scenarioType) {
        return activeScenarios.get(scenarioType);
    }
}