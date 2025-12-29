package com.example.TransactionSaga.service;

import com.example.TransactionSaga.client.ExternalServiceClient;
import com.example.TransactionSaga.model.entity.CompensationLogEntity;
import com.example.TransactionSaga.model.entity.TransactionEntity;
import com.example.TransactionSaga.model.enums.TransactionStatus;
import com.example.TransactionSaga.repository.CompensationLogRepository;
import com.example.TransactionSaga.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompensationService {

    private static final Logger log = LoggerFactory.getLogger(CompensationService.class);

    private final CompensationLogRepository compensationLogRepository;
    private final TransactionRepository transactionRepository;
    private final ExternalServiceClient externalServiceClient;

    @Autowired
    public CompensationService(
            CompensationLogRepository compensationLogRepository,
            TransactionRepository transactionRepository,
            ExternalServiceClient externalServiceClient) {
        this.compensationLogRepository = compensationLogRepository;
        this.transactionRepository = transactionRepository;
        this.externalServiceClient = externalServiceClient;
    }

    @Transactional
    public void initiateCompensation(String transactionId, String reason, String details) {
        TransactionEntity transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        CompensationLogEntity compensation = new CompensationLogEntity();
        compensation.setTransactionId(transactionId);
        compensation.setCompensationType("FULL");
        compensation.setReason(reason);
        compensation.setStatus("PENDING");
        compensation.setCompensationData("{\"details\": \"" + details + "\"}");

        compensationLogRepository.save(compensation);

        transaction.setStatus(TransactionStatus.COMPENSATING);
        transactionRepository.save(transaction);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processPendingCompensations() {
        List<CompensationLogEntity> pendingCompensations =
                compensationLogRepository.findByStatus("PENDING");

        for (CompensationLogEntity compensation : pendingCompensations) {
            try {
                executeCompensation(compensation);
            } catch (Exception e) {
                log.error("Compensation failed for {}: {}",
                        compensation.getId(), e.getMessage());
                compensation.setStatus("FAILED");
                compensation.setErrorMessage(e.getMessage());
                compensationLogRepository.save(compensation);
            }
        }
    }

    private void executeCompensation(CompensationLogEntity compensation) {
        compensation.setStatus("IN_PROGRESS");
        compensation.setStartedAt(LocalDateTime.now());
        compensationLogRepository.save(compensation);

        externalServiceClient.cancelReservations(compensation.getTransactionId());
        externalServiceClient.notifyCollectorAboutCancellation(compensation.getTransactionId());
        externalServiceClient.processRefund(compensation.getTransactionId());

        compensation.setStatus("COMPLETED");
        compensation.setCompletedAt(LocalDateTime.now());
        compensationLogRepository.save(compensation);

        TransactionEntity transaction = transactionRepository
                .findById(compensation.getTransactionId()).orElse(null);
        if (transaction != null) {
            transaction.setStatus(TransactionStatus.CANCELLED);
            transactionRepository.save(transaction);
        }
    }

    public List<CompensationLogEntity> getCompensationHistory(String transactionId) {
        return compensationLogRepository.findByTransactionId(transactionId);
    }
} // Закрывающая скобка была пропущена