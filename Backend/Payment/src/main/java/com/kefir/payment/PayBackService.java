package com.kefir.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayBackService {

    private static final Logger log = LoggerFactory.getLogger(PayBackService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentService paymentService;

    private static final Long SYSTEM_USER_ID = -1L;

    @Transactional
    public Map<String, Object> processPayBackRecords() {
        Map<String, Object> result = new HashMap<>();
        int processed = 0;
        int errors = 0;

        try {
            String findSql = "SELECT id, user_id, price, cart_id FROM pay_back WHERE status = 'created'";
            List<Map<String, Object>> records = jdbcTemplate.queryForList(findSql);

            log.info("📊 Найдено {} записей для возврата", records.size());

            for (Map<String, Object> record : records) {
                try {
                    processSingleRecord(record);
                    processed++;
                } catch (Exception e) {
                    errors++;
                    log.error("❌ Ошибка при обработке записи {}: {}", record.get("id"), e.getMessage());
                }
            }

            result.put("success", true);
            result.put("processed", processed);
            result.put("errors", errors);
            result.put("total", records.size());
            result.put("message", String.format("Обработано %d из %d записей", processed, records.size()));

        } catch (Exception e) {
            log.error("❌ Критическая ошибка при обработке возвратов: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @Transactional
    public void processSingleRecord(Map<String, Object> record) {
        Long recordId = ((Number) record.get("id")).longValue();
        Long userId = ((Number) record.get("user_id")).longValue();
        BigDecimal amount = BigDecimal.valueOf(((Number) record.get("price")).doubleValue());
        Long cartId = ((Number) record.get("cart_id")).longValue();

        log.info("💰 Обработка возврата для пользователя {}: сумма {}", userId, amount);

        try {
            // Получаем номер заказа из таблицы orders по cart_id
            String orderNumber = null;
            try {
                String orderSql = "SELECT order_number FROM orders WHERE cart_id = ?";
                orderNumber = jdbcTemplate.queryForObject(orderSql, String.class, cartId);
                log.info("📦 Найден номер заказа: {} для корзины {}", orderNumber, cartId);
            } catch (Exception e) {
                log.warn("⚠️ Не удалось найти номер заказа для корзины {}, используем cartId", cartId);
                orderNumber = cartId.toString(); // запасной вариант
            }

            // Используем метод refund из PaymentService с номером заказа
            Map<String, Object> refundResult = paymentService.refund(userId, amount, orderNumber, "Возврат по pay_back");

            if (!"success".equals(refundResult.get("status"))) {
                throw new RuntimeException("Ошибка при возврате: " + refundResult.get("message"));
            }

            // Обновляем статус в pay_back на 'completed'
            String updateSql = "UPDATE pay_back SET status = 'completed', data_tc = NOW() WHERE id = ?";
            int updated = jdbcTemplate.update(updateSql, recordId);

            if (updated > 0) {
                log.info("✅ Возврат для пользователя {} успешно выполнен, запись {} обновлена", userId, recordId);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при возврате для пользователя {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> getPayBackStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            String countSql = "SELECT status, COUNT(*) FROM pay_back GROUP BY status";
            List<Map<String, Object>> counts = jdbcTemplate.queryForList(countSql);

            String totalSql = "SELECT COUNT(*) FROM pay_back";
            Long total = jdbcTemplate.queryForObject(totalSql, Long.class);

            status.put("success", true);
            status.put("total", total);
            status.put("byStatus", counts);

        } catch (Exception e) {
            status.put("success", false);
            status.put("error", e.getMessage());
        }

        return status;
    }
}