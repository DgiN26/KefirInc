package com.kefir.logistics.launcher_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class DemoAutoStarter implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(DemoAutoStarter.class);
    private static boolean alreadyStarted = false;

    @Autowired
    private ServiceOrchestrator serviceOrchestrator;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!alreadyStarted) {
            alreadyStarted = true;

            logger.info("=== АВТОСТАРТ СИСТЕМЫ KEFIR LOGISTICS ===");

            // Запуск в отдельном потоке с задержкой
            new Thread(() -> {
                try {
                    // Ждем 5 секунд после старта лаунчера
                    Thread.sleep(5000);

                    logger.info("🚀 Запуск сервисов для миссии...");
                    // Используем новый метод для миссии вместо startAllServices()
                    serviceOrchestrator.startMissionServices();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Автостарт прерван");
                } catch (Exception e) {
                    logger.error("Ошибка автостарта: {}", e.getMessage(), e);
                }
            }).start();
        }
    }
}