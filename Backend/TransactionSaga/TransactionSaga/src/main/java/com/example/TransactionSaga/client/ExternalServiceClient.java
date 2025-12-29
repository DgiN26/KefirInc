package com.example.TransactionSaga.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExternalServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceClient.class);

    public void validateOrder(String transactionId) {
        log.info("Validating order for transaction: {}", transactionId);
    }

    public void checkStock(String transactionId) {
        log.info("Checking stock for transaction: {}", transactionId);
    }

    public void notifyOffice(String transactionId) {
        log.info("Notifying office about transaction: {}", transactionId);
    }

    public void notifyCollector(String transactionId) {
        log.info("Notifying collector about transaction: {}", transactionId);
    }

    public void reserveItems(String transactionId) {
        log.info("Reserving items for transaction: {}", transactionId);
    }

    public void updateTransactionStatus(String transactionId, String status) {
        log.info("Updating transaction {} status to: {}", transactionId, status);
    }

    public void logAuditEvent(String transactionId, String eventType, String details) {
        log.info("Audit event: transaction={}, event={}, details={}",
                transactionId, eventType, details);
    }

    // Новые методы для компенсации
    public void cancelReservations(String transactionId) {
        log.info("Cancelling reservations for transaction: {}", transactionId);
    }

    public void notifyCollectorAboutCancellation(String transactionId) {
        log.info("Notifying collector about cancellation for transaction: {}", transactionId);
    }

    public void processRefund(String transactionId) {
        log.info("Processing refund for transaction: {}", transactionId);
    }
}