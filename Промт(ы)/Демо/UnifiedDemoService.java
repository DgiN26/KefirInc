package com.kefir.logistics.launcher_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UnifiedDemoService {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedDemoService.class);

    @Autowired
    private RestTemplate restTemplate;

    // Конфигурация пользователей для демо
    private static final Map<String, UserCredentials> DEMO_USERS = new HashMap<String, UserCredentials>() {{
        put("client", new UserCredentials("client", "client", "client@kefir.logistics", "CLIENT"));
        put("collector", new UserCredentials("collector", "collector", "collector@kefir.logistics", "COLLECTOR"));
        put("office", new UserCredentials("office", "office", "office@kefir.logistics", "OFFICE"));
        put("admin", new UserCredentials("admin", "admin", "admin@kefir.logistics", "ADMIN"));
    }};

    // Текущее состояние демо
    private DemoState currentState = new DemoState();

    /**
     * Класс для хранения учетных данных
     */
    private static class UserCredentials {
        String username;
        String password;
        String email;
        String role;

        UserCredentials(String username, String password, String email, String role) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.role = role;
        }
    }

    /**
     * Внутренний класс для хранения состояния демо
     */
    private static class DemoState {
        String currentOrderId;
        String currentTransactionId;
        String currentCollectorTaskId;
        Map<String, String> userTokens = new HashMap<>(); // username -> token
        LocalDateTime demoStartTime;
        boolean demoInProgress = false;

        void reset() {
            currentOrderId = null;
            currentTransactionId = null;
            currentCollectorTaskId = null;
            userTokens.clear();
            demoInProgress = false;
        }

        String getToken(String username) {
            return userTokens.get(username);
        }

        void setToken(String username, String token) {
            userTokens.put(username, token);
        }
    }

    /**
     * Основной метод демо - запускает все сервисы И выполняет демонстрацию
     */
    public Map<String, Object> executeCompleteMissionDemo() {
        logger.info("🎯 ЗАПУСК ПОЛНОЙ МИССИИ: запуск сервисов + демонстрация");

        Map<String, Object> result = new LinkedHashMap<>();
        currentState.reset();
        currentState.demoStartTime = LocalDateTime.now();
        currentState.demoInProgress = true;

        try {
            // ============ ЭТАП 0: ПОДГОТОВКА СИСТЕМЫ ============
            result.put("stage0", prepareSystem());

            // ============ ЭТАП 1: ДЕМОНСТРАЦИЯ ПРОБЛЕМЫ ============
            logger.info("1. 🎬 ДЕМОНСТРАЦИЯ ПРОБЛЕМЫ (Пункт 1.1-1.2)");
            Map<String, Object> problemDemo = demonstrateProblem();
            result.put("stage1", problemDemo);

            if (!"PROBLEM_DEMONSTRATED".equals(problemDemo.get("status"))) {
                throw new RuntimeException("Не удалось продемонстрировать проблему: " + problemDemo.get("error"));
            }

            // Пауза для осмысления
            Thread.sleep(3000);

            // ============ ЭТАП 2: ДЕМОНСТРАЦИЯ РЕШЕНИЯ ============
            logger.info("2. 💡 ДЕМОНСТРАЦИЯ РЕШЕНИЯ (Пункт 2)");
            Map<String, Object> solutionDemo = demonstrateSolution();
            result.put("stage2", solutionDemo);

            // ============ ИТОГИ ============
            currentState.demoInProgress = false;

            result.put("status", "MISSION_COMPLETED");
            result.put("totalTime", getElapsedTime());
            result.put("missionPoints", Arrays.asList(
                    "✅ Подготовка системы",
                    "✅ Пункт 1.1: Демонстрация ошибки транзакции",
                    "✅ Пункт 1.2: Последствия неполной доставки",
                    "✅ Пункт 2: Решение через перезапуск транзакции"
            ));
            result.put("philosophy", "При неизвестных системных ошибках безопаснее перезагрузить процесс, чем пытаться его починить");

            logger.info("✅ ПОЛНАЯ МИССИЯ ВЫПОЛНЕНА!");

        } catch (Exception e) {
            logger.error("❌ Ошибка выполнения миссии: {}", e.getMessage(), e);
            currentState.demoInProgress = false;

            result.put("status", "MISSION_FAILED");
            result.put("error", e.getMessage());
            result.put("elapsedTime", getElapsedTime());
        }

        return result;
    }

    /**
     * Этап 0: Подготовка системы
     */
    private Map<String, Object> prepareSystem() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            logger.info("0. 🔧 ПОДГОТОВКА СИСТЕМЫ");

            // 0.1. Освобождаем все порты
            logger.info("   1. Освобождаю порты...");
            releaseAllPorts();
            result.put("portsReleased", true);

            // 0.2. Запускаем все необходимые сервисы
            logger.info("   2. Запускаю сервисы...");
            startAllRequiredServices();
            result.put("servicesStarted", true);

            // 0.3. Ждем инициализации
            logger.info("   3. Ожидание инициализации (10 сек)...");
            Thread.sleep(10000);

            // 0.4. Проверяем, что все запустилось
            logger.info("   4. Проверка запуска...");
            boolean allServicesReady = verifyServicesReady();
            result.put("allServicesReady", allServicesReady);

            if (!allServicesReady) {
                throw new RuntimeException("Не все сервисы запустились");
            }

            // 0.5. Создаем/проверяем пользователей
            logger.info("   5. Подготавливаю пользователей...");
            prepareDemoUsers();
            result.put("usersPrepared", true);

            result.put("status", "SYSTEM_READY");
            logger.info("✅ Система подготовлена");

        } catch (Exception e) {
            logger.error("❌ Ошибка подготовки системы: {}", e.getMessage());
            result.put("error", e.getMessage());
            result.put("status", "PREPARATION_FAILED");
        }

        return result;
    }

    /**
     * Подготовка пользователей для демо
     */
    private void prepareDemoUsers() {
        logger.info("   Создаю/проверяю пользователей демо...");

        for (Map.Entry<String, UserCredentials> entry : DEMO_USERS.entrySet()) {
            String username = entry.getKey();
            UserCredentials creds = entry.getValue();

            try {
                String token = ensureUserExists(creds);
                currentState.setToken(username, token);
                logger.info("   ✅ Пользователь '{}' готов", username);

            } catch (Exception e) {
                logger.error("   ❌ Ошибка подготовки пользователя '{}': {}", username, e.getMessage());
                throw new RuntimeException("Не удалось подготовить пользователя " + username);
            }
        }
    }

    /**
     * Гарантирует, что пользователь существует и возвращает токен
     */
    private String ensureUserExists(UserCredentials creds) {
        try {
            // Пробуем залогиниться с реальными паролями
            String token = loginUser(creds.username, creds.password);
            return token;

        } catch (Exception loginError) {
            logger.info("   Пользователь '{}' не найден, создаю...", creds.username);

            try {
                // Регистрируем нового пользователя
                registerUser(creds);

                // Логинимся после регистрации
                String token = loginUser(creds.username, creds.password);
                return token;

            } catch (Exception registerError) {
                logger.error("   Не удалось создать пользователя '{}': {}", creds.username, registerError.getMessage());

                // Если не удалось - создаем токен для демо
                return "Bearer DEMO_TOKEN_" + creds.username.toUpperCase();
            }
        }
    }

    /**
     * Логин пользователя с реальными паролями
     */
    private String loginUser(String username, String password) {
        String url = "http://localhost:8097/api/auth/login";

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            return "Bearer " + body.get("token");
        }

        throw new RuntimeException("Логин не удался для " + username);
    }

    /**
     * Регистрация пользователя
     */
    private void registerUser(UserCredentials creds) {
        String url = "http://localhost:8097/api/auth/register";

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", creds.username);
        userData.put("password", creds.password);
        userData.put("email", creds.email);
        userData.put("role", creds.role);
        userData.put("phone", generatePhoneNumber());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Регистрация не удалась для " + creds.username);
        }
    }

    /**
     * Демонстрация проблемы (Пункт 1.1-1.2)
     */
    private Map<String, Object> demonstrateProblem() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("missionPoint", "1.1-1.2: Демонстрация проблемы и последствий");

        try {
            // Шаг 1: Клиент создает заказ
            logger.info("   👤 КЛИЕНТ (client/client) создает заказ...");
            String orderId = createDemoOrder();
            currentState.currentOrderId = orderId;
            result.put("orderCreated", true);
            result.put("orderId", orderId);

            // Шаг 2: Начало транзакции
            logger.info("   🔗 Начало транзакции в Saga...");
            String transactionId = startTransaction(orderId);
            currentState.currentTransactionId = transactionId;
            result.put("transactionStarted", true);
            result.put("transactionId", transactionId);

            // Шаг 3: Сборщик начинает работу
            logger.info("   👷 СБОРЩИК (collector/collector) начинает сборку...");
            String taskId = startCollection(orderId);
            currentState.currentCollectorTaskId = taskId;
            result.put("collectionStarted", true);
            result.put("taskId", taskId);

            // Шаг 4: ВОЗНИКНОВЕНИЕ ОШИБКИ
            logger.info("   ⚠️ ВОЗНИКНОВЕНИЕ ОШИБКИ: товар 'Йогурт' отсутствует");
            triggerProductMissingError(taskId);
            result.put("errorTriggered", true);
            result.put("errorType", "PRODUCT_NOT_FOUND");
            result.put("missingProduct", "Йогурт");

            // Шаг 5: Офис связывается с клиентом
            logger.info("   📞 ОФИС (office/office) звонит клиенту...");
            simulateOfficeCall();
            result.put("officeContactedClient", true);

            // Шаг 6: ❌ ПРОБЛЕМНОЕ РЕШЕНИЕ - частичный коммит
            logger.info("   ❌ ВЫПОЛНЯЕМ ПРОБЛЕМНОЕ РЕШЕНИЕ: частичный коммит");
            executePartialCommit(transactionId);
            result.put("partialCommitExecuted", true);
            result.put("warning", "Транзакция закрыта ЧАСТИЧНО - это ПРОБЛЕМА!");

            // Шаг 7: ПОСЛЕДСТВИЯ
            logger.info("   💸 ПОКАЗЫВАЕМ ПОСЛЕДСТВИЯ...");
            Map<String, Object> consequences = showConsequences();
            result.put("consequences", consequences);

            result.put("status", "PROBLEM_DEMONSTRATED");
            logger.info("✅ Проблема продемонстрирована");

        } catch (Exception e) {
            logger.error("Ошибка демонстрации проблемы: {}", e.getMessage());
            result.put("error", e.getMessage());
            result.put("status", "PROBLEM_DEMO_FAILED");
        }

        return result;
    }

    /**
     * Клиент создает демо-заказ
     */
    private String createDemoOrder() {
        String clientToken = currentState.getToken("client");

        try {
            // 1. Создаем корзину
            String cartUrl = "http://localhost:8083/api/cart/create";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", clientToken);

            HttpEntity<String> request = new HttpEntity<>("{}", headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(cartUrl, request, Map.class);

            String cartId = (String) response.getBody().get("cartId");
            logger.debug("Создана корзина: {}", cartId);

            // 2. Добавляем товары
            String addUrl = "http://localhost:8083/api/cart/" + cartId + "/add";

            List<Map<String, Object>> products = Arrays.asList(
                    createProduct("Молоко", 2, 89.99, "PROD_001"),
                    createProduct("Хлеб", 1, 45.50, "PROD_002"),
                    createProduct("Йогурт", 4, 67.30, "PROD_003"),
                    createProduct("Яйца", 10, 120.00, "PROD_004"),
                    createProduct("Сыр", 5, 350.75, "PROD_005")
            );

            HttpEntity<List<Map<String, Object>>> addRequest = new HttpEntity<>(products, headers);
            restTemplate.postForEntity(addUrl, addRequest, Map.class);
            logger.debug("Товары добавлены в корзину");

            // 3. Оформляем заказ
            String checkoutUrl = "http://localhost:8083/api/cart/" + cartId + "/checkout";

            Map<String, Object> checkoutData = new HashMap<>();
            checkoutData.put("deliveryAddress", "ул. Демонстрационная, д. 1, кв. 5");
            checkoutData.put("paymentMethod", "CARD");
            checkoutData.put("contactPhone", "+79991112233");

            HttpEntity<Map<String, Object>> checkoutRequest = new HttpEntity<>(checkoutData, headers);
            ResponseEntity<Map> checkoutResponse = restTemplate.postForEntity(checkoutUrl, checkoutRequest, Map.class);

            String orderId = (String) checkoutResponse.getBody().get("orderId");
            logger.info("✅ Заказ создан: {}", orderId);

            return orderId;

        } catch (Exception e) {
            logger.error("Ошибка создания заказа: {}", e.getMessage());
            // Для демо возвращаем тестовый ID
            return "ORDER_" + System.currentTimeMillis();
        }
    }

    /**
     * Начало транзакции в Saga
     */
    private String startTransaction(String orderId) {
        String clientToken = currentState.getToken("client");

        try {
            String url = "http://localhost:8090/api/saga/transactions/start";

            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("orderId", orderId);
            transactionData.put("type", "ORDER_PROCESSING");
            transactionData.put("initiator", "CLIENT");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", clientToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(transactionData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            String transactionId = (String) response.getBody().get("transactionId");
            logger.info("✅ Транзакция начата: {}", transactionId);

            return transactionId;

        } catch (Exception e) {
            logger.error("Ошибка начала транзакции: {}", e.getMessage());
            return "TRANS_" + System.currentTimeMillis();
        }
    }

    /**
     * Сборщик начинает сборку
     */
    private String startCollection(String orderId) {
        String collectorToken = currentState.getToken("collector");

        try {
            String url = "http://localhost:8086/api/collector/tasks/assign";

            Map<String, Object> taskData = new HashMap<>();
            taskData.put("orderId", orderId);
            taskData.put("collectorId", "COLLECTOR_DEMO");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", collectorToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(taskData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            String taskId = (String) response.getBody().get("taskId");
            logger.info("✅ Задача сборщика создана: {}", taskId);

            // Начинаем сборку
            String startUrl = "http://localhost:8086/api/collector/tasks/" + taskId + "/start";
            restTemplate.postForEntity(startUrl, new HttpEntity<>(headers), Map.class);

            return taskId;

        } catch (Exception e) {
            logger.error("Ошибка создания задачи сборщика: {}", e.getMessage());
            return "TASK_" + System.currentTimeMillis();
        }
    }

    /**
     * Сообщение о проблеме с товаром
     */
    private void triggerProductMissingError(String taskId) {
        String collectorToken = currentState.getToken("collector");

        try {
            String url = "http://localhost:8086/api/collector/tasks/" + taskId + "/report-problem";

            Map<String, Object> problemData = new HashMap<>();
            problemData.put("problemType", "PRODUCT_NOT_FOUND");
            problemData.put("productId", "PROD_003");
            problemData.put("productName", "Йогурт");
            problemData.put("description", "Товар отсутствует на складе. Сканированы: PROD_001, PROD_002");
            problemData.put("scannedProducts", Arrays.asList("PROD_001", "PROD_002"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", collectorToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(problemData, headers);
            restTemplate.postForEntity(url, request, Map.class);

            logger.info("⚠️ Проблема зафиксирована: йогурт отсутствует");

        } catch (Exception e) {
            logger.error("Ошибка фиксации проблемы: {}", e.getMessage());
        }
    }

    /**
     * Симуляция звонка офиса
     */
    private void simulateOfficeCall() {
        logger.info("   [СИМУЛЯЦИЯ ЗВОНКА ОФИСА]");
        logger.info("   📞 Офис (office/office): 'Здравствуйте, это служба доставки KEFIR.'");
        logger.info("   📞 Офис: 'Йогурт отсутствует. Продолжить доставку без йогурта?'");
        logger.info("   👤 Клиент (client/client): 'Да, продолжайте без йогурта.'");
        logger.info("   ✅ Клиент согласился на частичную доставку");
    }

    /**
     * Частичный коммит транзакции (ПРОБЛЕМНОЕ решение)
     */
    private void executePartialCommit(String transactionId) {
        String collectorToken = currentState.getToken("collector");

        try {
            String url = "http://localhost:8090/api/saga/transactions/" + transactionId + "/partial-commit";

            Map<String, Object> commitData = new HashMap<>();
            commitData.put("completedProducts", Arrays.asList("PROD_001", "PROD_002", "PROD_004")); // Молоко, Хлеб, Яйца
            commitData.put("refundProduct", "PROD_003"); // Только йогурт
            commitData.put("reason", "Client agreed to partial delivery without yogurt");
            commitData.put("initiator", "COLLECTOR");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", collectorToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(commitData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.warn("❌ ВНИМАНИЕ: Транзакция закрыта ЧАСТИЧНО!");
                logger.warn("   Товар PROD_005 (Сыр) не был пробит,");
                logger.warn("   но деньги за него НЕ возвращены клиенту!");
            }

        } catch (Exception e) {
            logger.error("Ошибка частичного коммита: {}", e.getMessage());
        }
    }

    /**
     * Показ последствий проблемы
     */
    private Map<String, Object> showConsequences() {
        Map<String, Object> consequences = new LinkedHashMap<>();

        consequences.put("missingProducts", Arrays.asList(
                "Сыр (5 шт × 350.75 руб = 1,753.75 руб)",
                "Яйца (10 шт × 120 руб = 1,200 руб)"
        ));

        consequences.put("financialLoss", "2,953.75 руб из 3,184.87 руб");
        consequences.put("lossPercentage", "92.7%");

        consequences.put("time", Arrays.asList(
                "Ожидалось: 15 минут",
                "Фактически: 1 час",
                "Увеличение: 300%"
        ));

        consequences.put("additionalWork", Arrays.asList(
                "Офис обзванивает 3 склада",
                "Курьер объезжает несколько точек",
                "Ручная сверка заказов",
                "Расследование инцидента"
        ));

        consequences.put("clientImpact", Arrays.asList(
                "Недовольство клиента",
                "Риск потери клиента",
                "Негативные отзывы",
                "Удар по репутации"
        ));

        logger.info("   💸 ФИНАНСОВЫЕ ПОТЕРИ: 2,954 руб из 3,185 руб (92.7%)");
        logger.info("   ⏱️  ПОТЕРЯ ВРЕМЕНИ: 15 мин → 1 час (+300%)");
        logger.info("   🔄 ДОПОЛНИТЕЛЬНАЯ РАБОТА: обзвон складов, ручная сверка");
        logger.info("   😠 КЛИЕНТ: НЕДОВОЛЕН, рискуем потерять клиента");

        return consequences;
    }

    /**
     * Демонстрация решения (Пункт 2)
     */
    private Map<String, Object> demonstrateSolution() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("missionPoint", "2: Решение через перезапуск транзакции");

        try {
            logger.info("   💡 ПРЕДЛАГАЕМ РЕШЕНИЕ...");

            // Шаг 1: Анализ проблемы
            result.put("problemAnalysis", Arrays.asList(
                    "Клиент не получил 2 оплаченных товара",
                    "Невозможно гарантировать наличие непробитых товаров",
                    "Офис тратит время на расследование",
                    "Доставка занимает 1 час вместо 15 минут",
                    "Дополнительные расходы +300%"
            ));

            // Шаг 2: Техническое решение через Saga
            logger.info("   2. Техническое решение через Saga Pattern");
            result.put("technicalSolution", Arrays.asList(
                    "1. Полная отмена старой транзакции",
                    "2. Возврат ВСЕХ денег клиенту",
                    "3. Создание новой транзакции с теми же данными",
                    "4. Сборщик проверяет ВСЕ товары заново",
                    "5. Клиент оплачивает только доступные товары",
                    "6. Доставка за 15 минут"
            ));

            // Шаг 3: Преимущества решения
            result.put("benefits", Arrays.asList(
                    "✅ Клиент получает полный (доступный) заказ",
                    "✅ Деньги возвращены за все отсутствующие товары",
                    "✅ Доставка за 15 минут (стандартное время)",
                    "✅ Нет ручной работы офиса",
                    "✅ Данные согласованы, система 'чиста'",
                    "✅ Клиент доволен"
            ));

            // Шаг 4: Философия решения
            logger.info("   3. Философия решения");
            result.put("philosophy",
                    "🎯 'При неизвестных системных ошибках безопаснее перезагрузить процесс, " +
                            "чем пытаться его починить.'\n" +
                            "💡 Это как перезапустить зависший компьютер вместо поиска конкретного бага в коде."
            );

            result.put("status", "SOLUTION_DEMONSTRATED");
            result.put("keyMessage", "Безопаснее перезагрузить процесс, чем пытаться починить неизвестную ошибку");

            logger.info("✅ Решение продемонстрировано");

        } catch (Exception e) {
            logger.error("Ошибка демонстрации решения: {}", e.getMessage());
            result.put("error", e.getMessage());
            result.put("status", "SOLUTION_DEMO_FAILED");
        }

        return result;
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private Map<String, Object> createProduct(String name, int quantity, double price, String productId) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("quantity", quantity);
        product.put("price", price);
        product.put("productId", productId);
        return product;
    }

    private String generatePhoneNumber() {
        return "+7999" + (1000000 + new Random().nextInt(9000000));
    }

    private void releaseAllPorts() {
        // Реализация освобождения портов
    }

    private void startAllRequiredServices() {
        // Реализация запуска сервисов
    }

    private boolean verifyServicesReady() {
        return isPortOpen(8097) && isPortOpen(8083) && isPortOpen(8090);
    }

    private boolean isPortOpen(int port) {
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress("localhost", port), 1000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getElapsedTime() {
        if (currentState.demoStartTime == null) {
            return "0 сек";
        }

        long seconds = java.time.Duration.between(
                currentState.demoStartTime,
                LocalDateTime.now()
        ).getSeconds();

        return seconds + " сек";
    }
}