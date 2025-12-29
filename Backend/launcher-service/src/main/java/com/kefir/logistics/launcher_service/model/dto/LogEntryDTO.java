package com.kefir.logistics.launcher_service.model.dto;

import java.time.LocalDateTime;

public class LogEntryDTO {

    private LocalDateTime timestamp;
    private String level;          // INFO, ERROR, WARN, DEBUG
    private String serviceName;
    private String thread;
    private String logger;
    private String message;
    private String stackTrace;     // Для ошибок

    // Конструкторы
    public LogEntryDTO() {}

    public LogEntryDTO(LocalDateTime timestamp, String level, String serviceName, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.serviceName = serviceName;
        this.message = message;
    }

    // Геттеры и сеттеры
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getThread() { return thread; }
    public void setThread(String thread) { this.thread = thread; }

    public String getLogger() { return logger; }
    public void setLogger(String logger) { this.logger = logger; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    // Builder
    public static LogEntryDTOBuilder builder() {
        return new LogEntryDTOBuilder();
    }

    public static class LogEntryDTOBuilder {
        private LocalDateTime timestamp;
        private String level;
        private String serviceName;
        private String thread;
        private String logger;
        private String message;
        private String stackTrace;

        public LogEntryDTOBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public LogEntryDTOBuilder level(String level) {
            this.level = level;
            return this;
        }

        public LogEntryDTOBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public LogEntryDTOBuilder thread(String thread) {
            this.thread = thread;
            return this;
        }

        public LogEntryDTOBuilder logger(String logger) {
            this.logger = logger;
            return this;
        }

        public LogEntryDTOBuilder message(String message) {
            this.message = message;
            return this;
        }

        public LogEntryDTOBuilder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public LogEntryDTO build() {
            LogEntryDTO dto = new LogEntryDTO();
            dto.setTimestamp(timestamp);
            dto.setLevel(level);
            dto.setServiceName(serviceName);
            dto.setThread(thread);
            dto.setLogger(logger);
            dto.setMessage(message);
            dto.setStackTrace(stackTrace);
            return dto;
        }
    }
}