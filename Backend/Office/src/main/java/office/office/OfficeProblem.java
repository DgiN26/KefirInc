package office.office;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "office_problems")
public class OfficeProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "collector_id")
    private String collectorId;

    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "problem_type")  // поле есть
    private String problemType;

    @Column(name = "status")
    private String status;

    @Column(name = "details")
    private String details;

    @Column(name = "client_email_sent")
    private Boolean clientEmailSent = false;

    @Column(name = "client_decision")
    private String clientDecision;

    @Column(name = "office_action")
    private String officeAction;

    @Column(name = "priority")
    private String priority;

    @Column(name = "assigned_to")
    private Integer assignedTo;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "client_responded_at")
    private LocalDateTime clientRespondedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Конструкторы
    public OfficeProblem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры (ВСЕ ДОЛЖНЫ БЫТЬ!)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }

    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }

    // ВАЖНО: Должен быть геттер и сеттер для problemType!
    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Boolean getClientEmailSent() { return clientEmailSent; }
    public void setClientEmailSent(Boolean clientEmailSent) { this.clientEmailSent = clientEmailSent; }

    public String getClientDecision() { return clientDecision; }
    public void setClientDecision(String clientDecision) { this.clientDecision = clientDecision; }

    public String getOfficeAction() { return officeAction; }
    public void setOfficeAction(String officeAction) { this.officeAction = officeAction; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Integer getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Integer assignedTo) { this.assignedTo = assignedTo; }

    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(LocalDateTime notifiedAt) { this.notifiedAt = notifiedAt; }

    public LocalDateTime getClientRespondedAt() { return clientRespondedAt; }
    public void setClientRespondedAt(LocalDateTime clientRespondedAt) { this.clientRespondedAt = clientRespondedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}