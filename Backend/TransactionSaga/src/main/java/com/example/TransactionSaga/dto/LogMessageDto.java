package com.example.TransactionSaga.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class LogMessageDto {
    private LocalDateTime timestamp;
    private String level;
    private String message;
    private Map<String, Object> details;
    private String microservice = "transaction-saga";

    // Constructors
    public LogMessageDto() {
        this.timestamp = LocalDateTime.now();
    }

    public LogMessageDto(String level, String message) {
        this();
        this.level = level;
        this.message = message;
    }

    public LogMessageDto(String level, String message, Map<String, Object> details) {
        this(level, message);
        this.details = details;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public String getMicroservice() { return microservice; }
    public void setMicroservice(String microservice) { this.microservice = microservice; }
}