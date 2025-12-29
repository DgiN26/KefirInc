package com.example.collector;


import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collector_tasks")
public class CollectorTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", unique = true, nullable = false)
    private String taskId;

    @Column(name = "collector_id", nullable = false)
    private String collectorId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "status")
    private String status;

    @Column(name = "problem_type")
    private String problemType;

    @Column(name = "location")
    private String location;

    @Column(name = "comments")
    private String comments;

    @Column(name = "priority")
    private String priority;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "started_date")
    private LocalDateTime startedDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    // УДАЛИЛ поле transaction_status и связанные методы

    public CollectorTask() {}

    public CollectorTask(String taskId, String collectorId, String taskType, String status) {
        this.taskId = taskId;
        this.collectorId = collectorId;
        this.taskType = taskType;
        this.status = status;
        this.createdDate = LocalDateTime.now();
        this.priority = "MEDIUM";
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        if ("IN_PROGRESS".equals(status) && startedDate == null) {
            this.startedDate = LocalDateTime.now();
        } else if ("COMPLETED".equals(status) || "COMPLETED_WITH_RETURN".equals(status)) {
            this.completedDate = LocalDateTime.now();
        }
    }

    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getStartedDate() { return startedDate; }
    public void setStartedDate(LocalDateTime startedDate) { this.startedDate = startedDate; }

    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
}