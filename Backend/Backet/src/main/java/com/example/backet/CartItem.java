package com.example.backet;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "cart_id", nullable = false)
    private int cartId;

    @Column(name = "product_id", nullable = false)
    private int productId; // ID из сервиса товаров

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "nalichie", nullable = false)
    private String nalichie = "unknown";

    // Конструкторы, геттеры, сеттеры
    public CartItem() {
    }

    public CartItem(int cartId, int productId, int quantity, double price) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.nalichie = "unknown";
    }

    public CartItem(int cartId, int productId, int quantity, double price, String nalichie ) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.nalichie = "unknown";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getNalichie() {
        return nalichie;
    }

    public void setNalichie(String nalichie) {
        this.nalichie = nalichie;
    }
}
