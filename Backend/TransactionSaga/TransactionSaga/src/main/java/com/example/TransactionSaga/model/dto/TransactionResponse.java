package com.example.TransactionSaga.model.dto;

import com.example.TransactionSaga.model.enums.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
    private String transactionId;
    private String orderId;
    private String collectorId;
    private String clientId;
    private TransactionStatus status;
    private String statusDescription;
    private Map<String, Integer> scannedItems;
    private Map<String, Integer> missingItems;
    private Map<String, Object> metadata;
    private String pauseReason;
}