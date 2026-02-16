package com.kefir.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 10, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "operation_type", nullable = false)
    private String operationType; // DEPOSIT, WITHDRAWAL, REFUND, ORDER_PAYMENT

    @Column(name = "status", nullable = false)
    private String status = "COMPLETED"; // PENDING, COMPLETED, FAILED

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "description")
    private String description;

    @Column(name = "system_id")
    private Long systemUserId;

    @Column(name = "reference_id")
    private String referenceId;

    public PaymentTransaction() {
        this.createdDate = LocalDateTime.now();
    }

    public PaymentTransaction(Long userId, BigDecimal amount, String operationType,
                              String order_id, String description) {
        this.userId = userId;
        this.amount = amount;
        this.operationType = operationType;
        this.orderId = order_id;
        this.description = description;
        this.createdDate = LocalDateTime.now();
        this.status = "COMPLETED";
    }



    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSystemAccountId() {
        return systemUserId;
    }
    public void setSystemAccountId(Long systemAccountId) {
        this.systemUserId = systemAccountId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
}