package com.kefir.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PayBackScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayBackScheduler.class);

    @Autowired
    private PayBackService payBackService;

    private Map<String, Object> schedulerStatus = new HashMap<>();
    private long lastRunTime = 0;
    private long totalProcessed = 0;
    private long totalErrors = 0;

    @Scheduled(fixedDelay = 15000) // Каждые 15 секунд
    public void scheduledPayBackProcessing() {
        log.info("🔄 Запуск автоматической обработки возвратов (каждые 15 сек)");

        try {
            Map<String, Object> result = payBackService.processPayBackRecords();
            lastRunTime = System.currentTimeMillis();

            if (result.containsKey("processed")) {
                totalProcessed += (int) result.get("processed");
            }
            if (result.containsKey("errors")) {
                totalErrors += (int) result.get("errors");
            }

            schedulerStatus = result;
            schedulerStatus.put("lastRunTime", lastRunTime);
            schedulerStatus.put("totalProcessed", totalProcessed);
            schedulerStatus.put("totalErrors", totalErrors);

            log.info("✅ Обработка возвратов завершена: {}", result);

        } catch (Exception e) {
            log.error("❌ Ошибка при обработке возвратов: {}", e.getMessage(), e);
            schedulerStatus.put("error", e.getMessage());
            schedulerStatus.put("lastRunTime", lastRunTime);
            totalErrors++;
        }
    }

    public Map<String, Object> getSchedulerStatus() {
        Map<String, Object> status = new HashMap<>(schedulerStatus);
        status.put("currentTime", System.currentTimeMillis());
        return status;
    }
}