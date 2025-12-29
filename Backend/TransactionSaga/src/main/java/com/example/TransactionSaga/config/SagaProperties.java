package com.example.TransactionSaga.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "saga")
public class SagaProperties {

    private Retry retry = new Retry();
    private Timeout timeout = new Timeout();
    private Cleanup cleanup = new Cleanup();
    private Batch batch = new Batch();

    public static class Retry {
        private int maxAttempts = 3;
        private int delayMinutes = 5;
        private int maxDelayMinutes = 60;

        // Геттеры и сеттеры
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public int getDelayMinutes() { return delayMinutes; }
        public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }

        public int getMaxDelayMinutes() { return maxDelayMinutes; }
        public void setMaxDelayMinutes(int maxDelayMinutes) { this.maxDelayMinutes = maxDelayMinutes; }
    }

    public static class Timeout {
        private int defaultMinutes = 1440; // 24 часа
        private int clientResponseMinutes = 120; // 2 часа

        // Геттеры и сеттеры
        public int getDefaultMinutes() { return defaultMinutes; }
        public void setDefaultMinutes(int defaultMinutes) { this.defaultMinutes = defaultMinutes; }

        public int getClientResponseMinutes() { return clientResponseMinutes; }
        public void setClientResponseMinutes(int clientResponseMinutes) { this.clientResponseMinutes = clientResponseMinutes; }
    }

    public static class Cleanup {
        private int completedTransactionsHours = 24;
        private int failedTransactionsHours = 72;

        // Геттеры и сеттеры
        public int getCompletedTransactionsHours() { return completedTransactionsHours; }
        public void setCompletedTransactionsHours(int completedTransactionsHours) { this.completedTransactionsHours = completedTransactionsHours; }

        public int getFailedTransactionsHours() { return failedTransactionsHours; }
        public void setFailedTransactionsHours(int failedTransactionsHours) { this.failedTransactionsHours = failedTransactionsHours; }
    }

    public static class Batch {
        private int size = 50;
        private long delayMs = 1000;

        // Геттеры и сеттеры
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public long getDelayMs() { return delayMs; }
        public void setDelayMs(long delayMs) { this.delayMs = delayMs; }
    }

    // Геттеры и сеттеры для главного класса
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }

    public Timeout getTimeout() { return timeout; }
    public void setTimeout(Timeout timeout) { this.timeout = timeout; }

    public Cleanup getCleanup() { return cleanup; }
    public void setCleanup(Cleanup cleanup) { this.cleanup = cleanup; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }
}