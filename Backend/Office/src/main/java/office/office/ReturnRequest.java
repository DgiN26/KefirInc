package office.office;

public class ReturnRequest {
    private String requestId;
    private String collectorId;
    private String clientId;
    private String productId;
    private String reason;
    private Integer quantity;
    private String officeId;

    public ReturnRequest() {}

    public ReturnRequest(String requestId, String collectorId, String clientId, String productId,
                         String reason, Integer quantity, String officeId) {
        this.requestId = requestId;
        this.collectorId = collectorId;
        this.clientId = clientId;
        this.productId = productId;
        this.reason = reason;
        this.quantity = quantity;
        this.officeId = officeId;
    }

    // Геттеры и сеттеры
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCollectorId() { return collectorId; }
    public void setCollectorId(String collectorId) { this.collectorId = collectorId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getOfficeId() { return officeId; }
    public void setOfficeId(String officeId) { this.officeId = officeId; }
}