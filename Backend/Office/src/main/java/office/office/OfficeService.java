package office.office;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class OfficeService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OfficeProblemRepository officeProblemRepository;

    // ==================== МЕТОДЫ ДЛЯ РАБОТЫ С ТАБЛИЦАМИ ====================

    /**
     * Получить товар из таблицы usersklad
     */
    public Map<String, Object> getProductInfo(Integer productId) {
        try {
            String sql = "SELECT * FROM usersklad WHERE id = ?";
            return jdbcTemplate.queryForMap(sql, productId);
        } catch (Exception e) {
            throw new RuntimeException("Товар не найден: " + productId);
        }
    }

    /**
     * Получить заказ из таблицы carts
     */
    public Map<String, Object> getOrderInfo(Integer orderId) {
        try {
            String sql = "SELECT * FROM carts WHERE id = ?";
            return jdbcTemplate.queryForMap(sql, orderId);
        } catch (Exception e) {
            throw new RuntimeException("Заказ не найден: " + orderId);
        }
    }

    /**
     * Получить пользователя из таблицы users по ID заказа
     */
    public Map<String, Object> getUserInfoByOrder(Integer orderId) {
        try {
            // Получаем client_id из заказа
            String clientIdSql = "SELECT client_id FROM carts WHERE id = ?";
            Integer clientId = jdbcTemplate.queryForObject(clientIdSql, Integer.class, orderId);

            if (clientId == null) {
                throw new RuntimeException("Заказ не найден или не имеет client_id");
            }

            // Получаем пользователя (без пароля)
            String userSql = "SELECT id, username, firstname, email, city, status, role FROM users WHERE id = ?";
            return jdbcTemplate.queryForMap(userSql, clientId);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка получения пользователя: " + e.getMessage());
        }
    }

    /**
     * Получить полную информацию (товар + заказ + пользователь)
     */
    public Map<String, Object> getFullInfo(Integer orderId, Integer productId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("product", getProductInfo(productId));
            result.put("order", getOrderInfo(orderId));
            result.put("user", getUserInfoByOrder(orderId));
            result.put("timestamp", LocalDateTime.now());
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка получения полной информации: " + e.getMessage());
        }
    }

    /**
     * Поиск товаров по названию или артикулу
     */
    public List<Map<String, Object>> searchProducts(String query) {
        try {
            String sql = "SELECT * FROM usersklad WHERE LOWER(name) LIKE LOWER(?) OR akticul LIKE ? LIMIT 20";
            String searchPattern = "%" + query + "%";
            return jdbcTemplate.queryForList(sql, searchPattern, searchPattern);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Получить историю заказов пользователя
     */
    public List<Map<String, Object>> getUserOrderHistory(Integer userId) {
        try {
            String sql = "SELECT * FROM carts WHERE client_id = ? ORDER BY created_date DESC LIMIT 10";
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ==================== МЕТОДЫ ДЛЯ РАБОТЫ С ПРОБЛЕМАМИ ====================

    /**
     * Создать новую проблему (с details)
     */
    public OfficeProblem createProblem(Integer orderId, Integer productId, String collectorId,
                                       String problemType, String details) {
        try {
            // Получаем client_id из заказа
            String clientIdSql = "SELECT client_id FROM carts WHERE id = ?";
            Integer clientId = jdbcTemplate.queryForObject(clientIdSql, Integer.class, orderId);

            // Создаем новую проблему
            OfficeProblem problem = new OfficeProblem();
            problem.setOrderId(orderId);
            problem.setProductId(productId);
            problem.setCollectorId(collectorId);
            problem.setClientId(clientId);

            // Устанавливаем тип проблемы
            String finalProblemType = problemType != null ? problemType : "MISSING_PRODUCT";
            problem.setProblemType(finalProblemType);

            problem.setStatus("PENDING");

            // Устанавливаем детали
            String finalDetails = details != null ? details : "Сборщик " + collectorId + " сообщил о проблеме: " + finalProblemType;
            problem.setDetails(finalDetails);

            problem.setPriority("HIGH");
            problem.setClientEmailSent(false);

            return officeProblemRepository.save(problem);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания проблемы: " + e.getMessage());
        }
    }

    /**
     * Создать проблему (без details - для совместимости)
     */
    public OfficeProblem createProblem(Integer orderId, Integer productId, String collectorId,
                                       String problemType) {
        return createProblem(orderId, productId, collectorId, problemType, null);
    }

    /**
     * Получить проблему по ID
     */
    public OfficeProblem getProblemById(Long problemId) {
        return officeProblemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Проблема не найдена: " + problemId));
    }

    /**
     * Получить все активные проблемы
     */
    public List<OfficeProblem> getActiveProblems() {
        return officeProblemRepository.findActiveProblems();
    }

    /**
     * Получить проблемы по статусу
     */
    public List<OfficeProblem> getProblemsByStatus(String status) {
        return officeProblemRepository.findByStatus(status);
    }

    /**
     * Получить проблемы по ID заказа
     */
    public List<OfficeProblem> getProblemsByOrder(Integer orderId) {
        return officeProblemRepository.findByOrderId(orderId);
    }

    /**
     * Получить проблемы по ID клиента
     */
    public List<OfficeProblem> getProblemsByClient(Integer clientId) {
        return officeProblemRepository.findByClientId(clientId);
    }

    /**
     * Получить проблемы по ID сборщика
     */
    public List<OfficeProblem> getProblemsByCollector(String collectorId) {
        return officeProblemRepository.findByCollectorId(collectorId);
    }

    /**
     * Отправить уведомление клиенту о проблеме
     */
    public OfficeProblem notifyClient(Long problemId, String customMessage) {
        try {
            OfficeProblem problem = getProblemById(problemId);

            // Получаем информацию о клиенте
            Map<String, Object> userInfo = getUserInfoByOrder(problem.getOrderId());
            String clientEmail = (String) userInfo.get("email");
            String clientName = (String) userInfo.get("firstname");

            // Получаем информацию о товаре
            Map<String, Object> productInfo = getProductInfo(problem.getProductId());
            String productName = (String) productInfo.get("name");

            // Формируем сообщение
            String message = customMessage != null ? customMessage :
                    String.format("Уважаемый(ая) %s!\n\n" +
                                    "В вашем заказе #%d возникла проблема с товаром:\n" +
                                    "Название: %s\n" +
                                    "Тип проблемы: %s\n" +
                                    "Детали: %s\n\n" +
                                    "Пожалуйста, свяжитесь с нашим офисом для решения вопроса.\n\n" +
                                    "С уважением,\nКоманда KEFIR Logistics",
                            clientName, problem.getOrderId(), productName,
                            problem.getProblemType(), problem.getDetails());

            // Имитация отправки email
            System.out.println("\n" + "=".repeat(50));
            System.out.println("📧 ОТПРАВКА EMAIL КЛИЕНТУ");
            System.out.println("Кому: " + clientEmail);
            System.out.println("Тема: Проблема с заказом #" + problem.getOrderId());
            System.out.println("Сообщение:\n" + message);
            System.out.println("=".repeat(50) + "\n");

            // Обновляем статус проблемы
            problem.setClientEmailSent(true);
            problem.setNotifiedAt(LocalDateTime.now());
            problem.setStatus("CLIENT_NOTIFIED");

            return officeProblemRepository.save(problem);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка отправки уведомления: " + e.getMessage());
        }
    }

    /**
     * Обновить решение клиента
     */
    public OfficeProblem updateClientDecision(Long problemId, String decision, String comments) {
        try {
            OfficeProblem problem = getProblemById(problemId);

            problem.setClientDecision(decision);
            problem.setClientRespondedAt(LocalDateTime.now());
            problem.setStatus("CLIENT_DECIDED");

            if (comments != null && !comments.trim().isEmpty()) {
                problem.setDetails(problem.getDetails() + "\nКомментарий клиента: " + comments);
            }

            return officeProblemRepository.save(problem);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка обновления решения клиента: " + e.getMessage());
        }
    }

    /**
     * Решить проблему
     */
    public OfficeProblem resolveProblem(Long problemId, String officeAction, String solution) {
        try {
            OfficeProblem problem = getProblemById(problemId);

            problem.setOfficeAction(officeAction);
            problem.setStatus("RESOLVED");
            problem.setResolvedAt(LocalDateTime.now());

            if (solution != null && !solution.trim().isEmpty()) {
                problem.setDetails(problem.getDetails() + "\nРешение: " + solution);
            }

            return officeProblemRepository.save(problem);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка решения проблемы: " + e.getMessage());
        }
    }

    /**
     * Назначить проблему оператору
     */
    public OfficeProblem assignProblem(Long problemId, Integer operatorId) {
        try {
            OfficeProblem problem = getProblemById(problemId);
            problem.setAssignedTo(operatorId);
            return officeProblemRepository.save(problem);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка назначения проблемы: " + e.getMessage());
        }
    }

    /**
     * Обновить статус проблемы
     */
    public OfficeProblem updateProblemStatus(Long problemId, String status) {
        try {
            OfficeProblem problem = getProblemById(problemId);
            problem.setStatus(status);
            return officeProblemRepository.save(problem);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка обновления статуса: " + e.getMessage());
        }
    }

    /**
     * Удалить проблему (только если не решена)
     */
    public void deleteProblem(Long problemId) {
        try {
            OfficeProblem problem = getProblemById(problemId);

            if ("RESOLVED".equals(problem.getStatus()) ||
                    "COMPLETED".equals(problem.getStatus())) {
                throw new RuntimeException("Нельзя удалить решенную или завершенную проблему");
            }

            officeProblemRepository.delete(problem);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка удаления проблемы: " + e.getMessage());
        }
    }

    // ==================== МЕТОДЫ ДЛЯ ДАШБОРДА И СТАТИСТИКИ ====================

    /**
     * Получить статистику для дашборда
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Активные проблемы
            Long activeProblems = officeProblemRepository.countByStatus("PENDING");
            stats.put("activeProblems", activeProblems != null ? activeProblems : 0);

            // Уведомленные клиенты
            Long notifiedClients = officeProblemRepository.countByStatus("CLIENT_NOTIFIED");
            stats.put("notifiedClients", notifiedClients != null ? notifiedClients : 0);

            // Ожидают решения клиента
            Long waitingClient = officeProblemRepository.countByStatus("WAITING_CLIENT");
            stats.put("waitingClient", waitingClient != null ? waitingClient : 0);

            // Решено сегодня
            Long resolvedToday = officeProblemRepository.countTodayByStatus("RESOLVED");
            stats.put("resolvedToday", resolvedToday != null ? resolvedToday : 0);

            // Всего заказов сегодня
            String todayOrdersSql = "SELECT COUNT(*) FROM carts WHERE DATE(created_date) = CURRENT_DATE";
            Integer todayOrders = jdbcTemplate.queryForObject(todayOrdersSql, Integer.class);
            stats.put("todayOrders", todayOrders != null ? todayOrders : 0);

            // Активные пользователи
            String activeUsersSql = "SELECT COUNT(*) FROM users WHERE status = 'active'";
            Integer activeUsers = jdbcTemplate.queryForObject(activeUsersSql, Integer.class);
            stats.put("activeUsers", activeUsers != null ? activeUsers : 0);

            // Товаров на складе
            String productsSql = "SELECT COUNT(*) FROM usersklad WHERE count > 0";
            Integer totalProducts = jdbcTemplate.queryForObject(productsSql, Integer.class);
            stats.put("totalProducts", totalProducts != null ? totalProducts : 0);

        } catch (Exception e) {
            // Заглушка если что-то пошло не так
            stats.put("activeProblems", 8);
            stats.put("notifiedClients", 3);
            stats.put("waitingClient", 2);
            stats.put("resolvedToday", 5);
            stats.put("todayOrders", 12);
            stats.put("activeUsers", 42);
            stats.put("totalProducts", 156);
        }

        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }

    /**
     * Получить проблемы для дашборда
     */
    public List<OfficeProblem> getProblemsForDashboard(int limit) {
        try {
            List<OfficeProblem> problems = officeProblemRepository.findProblemsForDashboard();
            return problems.size() > limit ? problems.subList(0, limit) : problems;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Получить необработанные проблемы (старые более 1 часа)
     */
    public List<OfficeProblem> getUnprocessedProblems() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(1);
            return officeProblemRepository.findUnprocessedProblems(threshold);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Получить статистику по проблемам
     */
    public Map<String, Long> getProblemStatistics() {
        Map<String, Long> stats = new HashMap<>();

        try {
            // Простая статистика через базовые методы
            stats.put("PENDING", officeProblemRepository.countByStatus("PENDING"));
            stats.put("CLIENT_NOTIFIED", officeProblemRepository.countByStatus("CLIENT_NOTIFIED"));
            stats.put("WAITING_CLIENT", officeProblemRepository.countByStatus("WAITING_CLIENT"));
            stats.put("CLIENT_DECIDED", officeProblemRepository.countByStatus("CLIENT_DECIDED"));
            stats.put("RESOLVED", officeProblemRepository.countByStatus("RESOLVED"));
            stats.put("CANCELLED", officeProblemRepository.countByStatus("CANCELLED"));
            stats.put("TOTAL", officeProblemRepository.count());

        } catch (Exception e) {
            // Заглушка
            stats.put("PENDING", 5L);
            stats.put("CLIENT_NOTIFIED", 3L);
            stats.put("RESOLVED", 8L);
            stats.put("TOTAL", 16L);
        }

        return stats;
    }

    /**
     * Получить последние проблемы
     */
    public List<OfficeProblem> getRecentProblems(int count) {
        try {
            List<OfficeProblem> problems = officeProblemRepository.findTop10ByOrderByCreatedAtDesc();
            return problems.size() > count ? problems.subList(0, count) : problems;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Проверить наличие активных проблем для заказа
     */
    public boolean hasActiveProblems(Integer orderId) {
        try {
            List<OfficeProblem> problems = officeProblemRepository.findByOrderId(orderId);
            return problems.stream()
                    .anyMatch(p -> !"RESOLVED".equals(p.getStatus()) &&
                            !"CANCELLED".equals(p.getStatus()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверить товар на других складах (имитация)
     */
    public List<Map<String, Object>> checkOtherWarehouses(Integer productId) {
        List<Map<String, Object>> warehouses = new ArrayList<>();
        Random random = new Random();

        // Имитация проверки 5 складов
        for (int i = 1; i <= 5; i++) {
            // 40% chance что товар есть на складе
            if (random.nextInt(100) < 40) {
                Map<String, Object> warehouse = new HashMap<>();
                warehouse.put("warehouseId", i);
                warehouse.put("warehouseName", "Склад #" + i);
                warehouse.put("quantity", random.nextInt(10) + 1);
                warehouse.put("distance", (i * 3) + " км");
                warehouse.put("estimatedTime", (i * 15) + " минут");
                warehouses.add(warehouse);
            }
        }

        return warehouses;
    }

    /**
     * Симуляция: сборщик обнаружил проблему
     */
    public Map<String, Object> simulateCollectorProblem(String collectorId) {
        try {
            // Генерируем тестовые данные
            Random random = new Random();
            Integer orderId = random.nextInt(100) + 1;
            Integer productId = random.nextInt(50) + 1;
            String[] problemTypes = {"MISSING_PRODUCT", "DAMAGED_PRODUCT", "WRONG_PRODUCT", "QUALITY_ISSUE"};
            String problemType = problemTypes[random.nextInt(problemTypes.length)];

            // Создаем проблему
            OfficeProblem problem = createProblem(orderId, productId, collectorId, problemType,
                    "Тестовая симуляция проблемы от сборщика " + collectorId);

            // Получаем информацию о товаре
            Map<String, Object> productInfo = getProductInfo(productId);
            String productName = (String) productInfo.get("name");

            // Получаем информацию о клиенте
            Map<String, Object> userInfo = getUserInfoByOrder(orderId);
            String clientName = (String) userInfo.get("firstname");
            String clientEmail = (String) userInfo.get("email");

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("simulation", true);
            result.put("problem", problem);
            result.put("collectorId", collectorId);
            result.put("orderId", orderId);
            result.put("productId", productId);
            result.put("productName", productName);
            result.put("clientName", clientName);
            result.put("clientEmail", clientEmail);
            result.put("message", "Проблема успешно смоделирована");
            result.put("nextStep", "notify_client");
            result.put("timestamp", LocalDateTime.now());

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка симуляции: " + e.getMessage());
        }
    }

    // ==================== ПРОВЕРКА ЗДОРОВЬЯ ====================

    /**
     * Проверка здоровья сервиса
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Простая проверка подключения к БД
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("database", "UP");

            // Проверка репозитория
            long problemCount = officeProblemRepository.count();
            health.put("repository", "UP");
            health.put("problemCount", problemCount);

            health.put("status", "UP");
            health.put("service", "office-service");
            health.put("port", 8086);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }

        health.put("timestamp", LocalDateTime.now());
        return health;
    }

    // ==================== МЕТОДЫ ДЛЯ ОБРАБОТКИ ВОЗВРАТОВ ====================

    /**
     * Обработать возврат от сборщика
     */
    public Map<String, Object> processReturnFromCollector(Map<String, Object> returnRequest) {
        try {
            String collectorId = (String) returnRequest.get("collectorId");
            Integer orderId = (Integer) returnRequest.get("orderId");
            Integer productId = (Integer) returnRequest.get("productId");
            Integer quantity = (Integer) returnRequest.get("quantity");
            String reason = (String) returnRequest.get("reason");

            // Создаем запись о проблеме для возврата
            OfficeProblem problem = createProblem(orderId, productId, collectorId,
                    "OTHER", "Возврат от сборщика: " + reason);

            // Обновляем статус
            problem.setStatus("RESOLVED");
            problem.setOfficeAction("PROCESS_REFUND");
            problem.setResolvedAt(LocalDateTime.now());
            officeProblemRepository.save(problem);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("problemId", problem.getId());
            result.put("collectorId", collectorId);
            result.put("orderId", orderId);
            result.put("productId", productId);
            result.put("quantity", quantity);
            result.put("refundProcessed", true);
            result.put("message", "Возврат обработан, средства возвращены клиенту");
            result.put("timestamp", LocalDateTime.now());

            // Имитация возврата средств
            System.out.println("💰 Возврат средств для заказа #" + orderId +
                    ", товар #" + productId + ", количество: " + quantity);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка обработки возврата: " + e.getMessage());
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Проверить подключение к таблицам
     */
    public Map<String, Boolean> checkTableConnections() {
        Map<String, Boolean> connections = new HashMap<>();

        String[] tables = {"users", "carts", "usersklad", "office_problems"};

        for (String table : tables) {
            try {
                String sql = "SELECT 1 FROM " + table + " LIMIT 1";
                jdbcTemplate.queryForObject(sql, Integer.class);
                connections.put(table, true);
            } catch (Exception e) {
                connections.put(table, false);
            }
        }

        return connections;
    }

    /**
     * Получить количество проблем по типам
     */
    public Map<String, Long> getProblemTypesCount() {
        Map<String, Long> typeCounts = new HashMap<>();

        try {
            // Получаем все проблемы
            List<OfficeProblem> problems = officeProblemRepository.findAll();

            // Группируем по типу
            for (OfficeProblem problem : problems) {
                String type = problem.getProblemType();
                typeCounts.put(type, typeCounts.getOrDefault(type, 0L) + 1);
            }

        } catch (Exception e) {
            // Заглушка
            typeCounts.put("MISSING_PRODUCT", 5L);
            typeCounts.put("DAMAGED_PRODUCT", 3L);
            typeCounts.put("OTHER", 2L);
        }

        return typeCounts;
    }
}