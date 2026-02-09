package com.example.TransactionSaga.scheduler;

import com.example.TransactionSaga.service.VozvratProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledPollingService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledPollingService.class);

    @Autowired
    private VozvratProcessorService vozvratProcessorService;

    @Scheduled(fixedDelay = 10000) // Каждые 10 секунд
    public void pollVozvratRecords() {
        try {
            logger.debug("Starting vozvrat polling...");
            vozvratProcessorService.processVozvratRecords();
        } catch (Exception e) {
            logger.error("Error in polling task", e);
        }
    }
}