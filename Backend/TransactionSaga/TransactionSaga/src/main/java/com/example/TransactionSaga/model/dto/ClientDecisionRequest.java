package com.example.TransactionSaga.model.dto;

import java.util.List;

public class ClientDecisionRequest {
    private String decision;
    private String notes;
    private String clientId;
    private List<String> substituteProducts;
    private Boolean requestRefund;
    private String refundReason;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public List<String> getSubstituteProducts() { return substituteProducts; }
    public void setSubstituteProducts(List<String> substituteProducts) { this.substituteProducts = substituteProducts; }

    public Boolean getRequestRefund() { return requestRefund; }
    public void setRequestRefund(Boolean requestRefund) { this.requestRefund = requestRefund; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
}