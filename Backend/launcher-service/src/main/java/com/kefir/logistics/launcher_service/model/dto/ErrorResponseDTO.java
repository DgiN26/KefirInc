package com.kefir.logistics.launcher_service.model.dto;

import java.time.LocalDateTime;

public class ErrorResponseDTO {

    private String errorCode;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String serviceName;

    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseDTO(String errorCode, String message, String serviceName) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.serviceName = serviceName;
    }

    public ErrorResponseDTO(String errorCode, String message, String details, String serviceName) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.serviceName = serviceName;
    }

    // Геттеры и сеттеры
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
}