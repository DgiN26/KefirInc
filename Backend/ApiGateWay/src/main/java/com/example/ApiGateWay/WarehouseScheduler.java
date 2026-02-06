// WarehouseScheduler.java
package com.example.ApiGateWay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
public class WarehouseScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarehouseScheduler.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Интервалы для каждого склада (в миллисекундах)
    private static final long SKLADODIN_INTERVAL = 5 * 60 * 1000; // 5 минут
    private static final long SKLADDVA_INTERVAL = (5 * 60 + 10) * 1000; // 5 минут 10 секунд
    private static final long SKLADTRI_INTERVAL = (5 * 60 + 30) * 1000; // 5 минут 30 секунд

    /**
     * Фоновая задача для skladodin (каждые 5 минут)
     */
    @Scheduled(fixedDelay = SKLADODIN_INTERVAL)
    public void transferToSkladodin() {
        log.info("🔄 Запуск переноса товаров в skladodin");
        transferProductsToWarehouse("skladodin");
    }

    /**
     * Фоновая задача для skladdva (каждые 5 минут 10 секунд)
     */
    @Scheduled(fixedDelay = SKLADDVA_INTERVAL)
    public void transferToSkladdva() {
        log.info("🔄 Запуск переноса товаров в skladdva");
        transferProductsToWarehouse("skladdva");
    }

    /**
     * Фоновая задача для skladtri (каждые 5 минут 30 секунд)
     */
    @Scheduled(fixedDelay = SKLADTRI_INTERVAL)
    public void transferToSkladtri() {
        log.info("🔄 Запуск переноса товаров в skladtri");
        transferProductsToWarehouse("skladtri");
    }

    /**
     * Основной метод переноса товаров в указанный склад
     */
    @Transactional
    public void transferProductsToWarehouse(String warehouseTable) {
        try {
            // 1. Получаем ВСЕ товары из основного склада, где count > 0
            String getProductsSql = "SELECT id, name, price, count, akticul, category, " +
                    "description, supplier, created_at, updated_at " +
                    "FROM usersklad WHERE count > 0";

            List<Map<String, Object>> products = jdbcTemplate.queryForList(getProductsSql);

            if (products.isEmpty()) {
                log.info("📭 На основном складе нет товаров для переноса в {}", warehouseTable);
                return;
            }

            log.info("📦 Найдено {} товаров для переноса в {}", products.size(), warehouseTable);
            int transferredItems = 0;
            int skippedItems = 0;

            // 2. Для КАЖДОГО товара выполняем операцию переноса
            for (Map<String, Object> product : products) {
                Integer productId = (Integer) product.get("id");
                String productName = (String) product.get("name");

                // Безопасное уменьшение count на основном складе
                String updateMainSql = "UPDATE usersklad SET count = count - 1, " +
                        "updated_at = ? WHERE id = ? AND count > 0";

                int rowsUpdated = jdbcTemplate.update(updateMainSql,
                        LocalDateTime.now(), productId);

                if (rowsUpdated == 0) {
                    // count уже был 0 или ушел в минус в параллельной операции
                    log.debug("⚠️ Товар {} (ID: {}) пропущен (count <= 0)", productName, productId);
                    skippedItems++;
                    continue;
                }

                // 3. Проверяем существование товара в целевом складе
                String checkExistsSql = String.format(
                        "SELECT COUNT(*) FROM %s WHERE id = ?", warehouseTable);

                Integer exists = jdbcTemplate.queryForObject(checkExistsSql, Integer.class, productId);

                if (exists != null && exists > 0) {
                    // Товар уже есть в целевом складе - увеличиваем count
                    String updateWarehouseSql = String.format(
                            "UPDATE %s SET count = count + 1, updated_at = ? WHERE id = ?",
                            warehouseTable);

                    jdbcTemplate.update(updateWarehouseSql, LocalDateTime.now(), productId);
                    log.debug("➕ Увеличен count товара {} (ID: {}) в {}",
                            productName, productId, warehouseTable);
                } else {
                    // Товара нет в целевом складе - создаем новую запись
                    String insertSql = String.format(
                            "INSERT INTO %s (id, name, price, count, akticul, category, " +
                                    "description, supplier, created_at, updated_at) " +
                                    "VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?, ?)", warehouseTable);

                    jdbcTemplate.update(insertSql,
                            productId,
                            product.get("name"),
                            product.get("price"),
                            product.get("akticul"),
                            product.get("category"),
                            product.get("description"),
                            product.get("supplier"),
                            LocalDateTime.now(), // created_at
                            LocalDateTime.now()  // updated_at
                    );
                    log.debug("🆕 Создана запись товара {} (ID: {}) в {}",
                            productName, productId, warehouseTable);
                }

                transferredItems++;
                log.debug("✅ Товар {} (ID: {}) перенесен в {}",
                        productName, productId, warehouseTable);
            }

            log.info("📊 Итог переноса в {}: перенесено {}, пропущено {}",
                    warehouseTable, transferredItems, skippedItems);

        } catch (Exception e) {
            log.error("❌ Критическая ошибка при переносе в {}: {}",
                    warehouseTable, e.getMessage(), e);
            // Исключение вызовет откат транзакции благодаря @Transactional
            throw new RuntimeException("Ошибка переноса товаров в " + warehouseTable, e);
        }
    }

    /**
     * Эндпоинт для проверки статуса фоновых задач
     */
    public Map<String, Object> getSchedulerStatus() {
        Map<String, Object> status = new java.util.HashMap<>();

        status.put("service", "WarehouseScheduler");
        status.put("status", "ACTIVE");
        status.put("timestamp", LocalDateTime.now());

        // Получаем статистику по каждому складу
        String[] warehouses = {"skladodin", "skladdva", "skladtri"};
        List<Map<String, Object>> warehouseStats = new java.util.ArrayList<>();

        for (String warehouse : warehouses) {
            try {
                String countSql = String.format("SELECT COUNT(*) as total_items, " +
                        "COALESCE(SUM(count), 0) as total_quantity " +
                        "FROM %s", warehouse);

                Map<String, Object> stats = jdbcTemplate.queryForMap(countSql);
                Map<String, Object> warehouseInfo = new java.util.HashMap<>();
                warehouseInfo.put("name", warehouse);
                warehouseInfo.put("totalItems", stats.get("total_items"));
                warehouseInfo.put("totalQuantity", stats.get("total_quantity"));
                warehouseInfo.put("lastUpdate", LocalDateTime.now());

                warehouseStats.add(warehouseInfo);
            } catch (Exception e) {
                log.warn("Не удалось получить статистику для {}: {}", warehouse, e.getMessage());
            }
        }

        status.put("warehouses", warehouseStats);

        // Статистика основного склада
        try {
            String mainSql = "SELECT COUNT(*) as total_items, " +
                    "COALESCE(SUM(count), 0) as total_quantity " +
                    "FROM usersklad";
            Map<String, Object> mainStats = jdbcTemplate.queryForMap(mainSql);
            status.put("mainWarehouse", mainStats);
        } catch (Exception e) {
            log.warn("Не удалось получить статистику основного склада: {}", e.getMessage());
        }

        return status;
    }
}