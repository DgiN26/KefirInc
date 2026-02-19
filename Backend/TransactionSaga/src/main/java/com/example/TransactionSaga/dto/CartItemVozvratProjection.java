package com.example.TransactionSaga.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CartItemVozvratProjection {
    Long getId();              // cart_items.id
    Integer getCartId();       // cart_items.cart_id
    BigDecimal getPrice();         // cart_items.price
    Long getClientId();     // carts.client_id (это user_id)
    LocalDateTime getCreatedDate();// carts.created_date
    String getStatus();
}