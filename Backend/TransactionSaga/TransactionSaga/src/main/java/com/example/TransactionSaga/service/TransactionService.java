package com.example.TransactionSaga.service;

import com.example.TransactionSaga.config.SagaProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    @Value("${saga.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${saga.timeout.default-minutes}")
    private int defaultTimeoutMinutes;

    @Value("${services.office.url}")
    private String officeServiceUrl;

    @Value("${services.collector.url}")
    private String collectorServiceUrl;

    private final SagaProperties sagaProperties;

    @Autowired
    public TransactionService(SagaProperties sagaProperties) {
        this.sagaProperties = sagaProperties;
    }

    public void someMethod() {
        // Использование значений из пропертис
        System.out.println("Max retry attempts: " + sagaProperties.getRetry().getMaxAttempts());
        System.out.println("Default timeout: " + sagaProperties.getTimeout().getDefaultMinutes());
        System.out.println("Office service URL: " + officeServiceUrl);

        // Использование @Value напрямую
        System.out.println("Max retry (via @Value): " + maxRetryAttempts);
    }
}