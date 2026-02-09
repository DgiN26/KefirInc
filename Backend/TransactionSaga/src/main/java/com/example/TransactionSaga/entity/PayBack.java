package com.example.TransactionSaga.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pay_back")
public class PayBack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "cart_id", nullable = false)
    private Integer cartId;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "data_tc")
    private LocalDateTime dataTc;

    // Constructors
    public PayBack() {
        this.dataTc = LocalDateTime.now();
    }

    public PayBack(Integer userId, Integer cartId, Double price, LocalDateTime createdDate) {
        this.userId = userId;
        this.cartId = cartId;
        this.price = price;
        this.createdDate = createdDate;
        this.dataTc = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getCartId() { return cartId; }
    public void setCartId(Integer cartId) { this.cartId = cartId; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getDataTc() { return dataTc; }
    public void setDataTc(LocalDateTime dataTc) { this.dataTc = dataTc; }
}