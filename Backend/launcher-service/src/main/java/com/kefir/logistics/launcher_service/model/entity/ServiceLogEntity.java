package com.kefir.logistics.launcher_service.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_logs")
@TableGenerator(name = "log_gen", table = "id_gen", pkColumnName = "gen_name",
        valueColumnName = "gen_value", pkColumnValue = "log_gen", initialValue = 1, allocationSize = 100)
public class ServiceLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "log_gen")
    private Long id;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "log_level", nullable = false)
    private String logLevel; // INFO, ERROR, WARN, DEBUG

    @Column(name = "message", length = 4000, nullable = false)
    private String message;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "thread_name")
    private String threadName;

    @Column(name = "logger_name")
    private String loggerName;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    // Конструкторы
    public ServiceLogEntity() {
        this.timestamp = LocalDateTime.now();
    }

    public ServiceLogEntity(String serviceId, String logLevel, String message) {
        this();
        this.serviceId = serviceId;
        this.logLevel = logLevel;
        this.message = message;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }

    public String getLoggerName() { return loggerName; }
    public void setLoggerName(String loggerName) { this.loggerName = loggerName; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
}