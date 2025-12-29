package com.example.TransactionSaga.model.dto;

import java.util.List;

public class TransactionRequest {
    private String orderId;
    private String collectorId;
    private String clientId;
    private List<OrderItemRequest> items;
    private Integer timeoutMinutes;
    private String priority;
    private String notes;

    // Геттеры и сеттеры
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public Integer getTimeoutMinutes() { return timeoutMinutes; }
    public void setTimeoutMinutes(Integer timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public static class OrderItemRequest {
        private String productId;
        private String productName;
        private Integer quantity;
        private Double price;
        private String location;
        private String barcode;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
    }
}