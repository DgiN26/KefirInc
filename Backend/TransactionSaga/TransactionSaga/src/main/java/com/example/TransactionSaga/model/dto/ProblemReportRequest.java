package com.example.TransactionSaga.model.dto;

public class ProblemReportRequest {
    private String problemType;
    private String productId;
    private Integer quantity;
    private String description;
    private String location;
    private String collectorNotes;
    private String imageUrl;

    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCollectorNotes() { return collectorNotes; }
    public void setCollectorNotes(String collectorNotes) { this.collectorNotes = collectorNotes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}