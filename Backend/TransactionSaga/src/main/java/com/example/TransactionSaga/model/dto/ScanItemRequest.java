package com.example.TransactionSaga.model.dto;

public class ScanItemRequest {
    private String productId;
    private String barcode;
    private Integer quantity;
    private String location;
    private Boolean forceScan;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Boolean getForceScan() { return forceScan; }
    public void setForceScan(Boolean forceScan) { this.forceScan = forceScan; }
}