package com.kefir.logistics.launcher_service.model.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_status_history")
public class ServiceStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "pid")
    private Integer pid;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Конструкторы
    public ServiceStatusEntity() {
        this.createdAt = LocalDateTime.now();
    }

    public ServiceStatusEntity(String serviceId, String serviceName, String status) {
        this();
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.status = status;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPid() { return pid; }
    public void setPid(Integer pid) { this.pid = pid; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(LocalDateTime stoppedAt) { this.stoppedAt = stoppedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}