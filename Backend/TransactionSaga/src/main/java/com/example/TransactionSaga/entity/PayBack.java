package com.example.TransactionSaga.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pay_back")
public class PayBack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cart_id", nullable = false)
    private Integer cartId;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "data_tc")
    private LocalDateTime dataTc;

    @Column(name = "status", nullable = false)
    private String status = "created";

    // Constructors
    public PayBack() {
        this.dataTc = LocalDateTime.now();
    }

    public PayBack(Long userId, Integer cartId, BigDecimal price, LocalDateTime createdDate) {
        this.userId = userId;
        this.cartId = cartId;
        this.price = price;
        this.createdDate = createdDate;
        this.dataTc = LocalDateTime.now();
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getCartId() { return cartId; }
    public void setCartId(Integer cartId) { this.cartId = cartId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getDataTc() { return dataTc; }
    public void setDataTc(LocalDateTime dataTc) { this.dataTc = dataTc; }
}