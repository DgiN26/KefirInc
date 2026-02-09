package com.example.TransactionSaga.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LoggingService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);

    public void logInfo(String message) {
        logger.info(message);
    }

    public void logInfo(String message, Map<String, Object> details) {
        if (details != null) {
            logger.info("{} - Details: {}", message, details);
        } else {
            logger.info(message);
        }
    }

    public void logError(String message, Exception e) {
        logger.error(message, e);
    }

    public void logError(String message, Exception e, Map<String, Object> details) {
        if (details != null) {
            logger.error("{} - Details: {}", message, details, e);
        } else {
            logger.error(message, e);
        }
    }
}