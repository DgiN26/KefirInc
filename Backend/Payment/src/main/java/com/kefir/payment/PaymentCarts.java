package com.kefir.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "payment_carts")
public class PaymentCarts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_users", nullable = false)
    private Long idUsers;

    @Column(name = "cart_number", length = 16, nullable = false, unique = true)
    private String cartNumber;

    @Column(name = "balans", precision = 10, scale = 2, nullable = false)
    private BigDecimal balans;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "CVV/CVC", precision = 3, nullable = false)
    private String cvv;

    // Конструкторы
    public PaymentCarts() {
    }

    public PaymentCarts(Long idUsers, String cartNumber) {
        this.idUsers = idUsers;
        this.cartNumber = cartNumber;
        this.balans = generateRandomBalance();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.cvv = generateRandomCVV();
    }

    // Генерация случайного баланса от 1000 до 10000
    private BigDecimal generateRandomBalance() {
        Random random = new Random();
        double min = 1000.0;
        double max = 10000.0;
        double randomValue = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(randomValue).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private String generateRandomCVV(){
        int randomValue = ThreadLocalRandom.current().nextInt(1, 1000);
        return String.format("%03d",randomValue);
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public Long getIdUsers() {
        return idUsers;
    }
    public void setIdUsers(Long idUsers) {
        this.idUsers = idUsers;
    }

    public String getCartNumber() {
        return cartNumber;
    }
    public void setCartNumber(String cartNumber) {
        this.cartNumber = cartNumber;
    }

    public BigDecimal getBalans() {
        return balans;
    }
    public void setBalans(BigDecimal balans) {
        this.balans = balans;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCvv() {
        return cvv;
    }
    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
}