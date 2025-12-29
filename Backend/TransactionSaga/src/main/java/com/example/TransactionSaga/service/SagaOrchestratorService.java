package com.example.TransactionSaga.service;

import com.example.TransactionSaga.client.ExternalServiceClient;
import com.example.TransactionSaga.model.entity.SagaStepEntity;
import com.example.TransactionSaga.model.entity.TransactionEntity;
import com.example.TransactionSaga.model.enums.TransactionStatus;
import com.example.TransactionSaga.repository.SagaStepRepository;
import com.example.TransactionSaga.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SagaOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestratorService.class);

    private final SagaStepRepository sagaStepRepository;
    private final TransactionRepository transactionRepository;
    private final ExternalServiceClient externalServiceClient;
    private final CompensationService compensationService;

    @Autowired
    public SagaOrchestratorService(
            SagaStepRepository sagaStepRepository,
            TransactionRepository transactionRepository,
            ExternalServiceClient externalServiceClient,
            CompensationService compensationService) {
        this.sagaStepRepository = sagaStepRepository;
        this.transactionRepository = transactionRepository;
        this.externalServiceClient = externalServiceClient;
        this.compensationService = compensationService;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processPendingSteps() {
        List<SagaStepEntity> pendingSteps = sagaStepRepository.findByStatus("PENDING");

        if (pendingSteps.isEmpty()) {
            log.debug("No pending steps to process");
            return;
        }

        log.info("Processing {} pending saga steps", pendingSteps.size());

        for (SagaStepEntity step : pendingSteps) {
            try {
                if (checkDependencies(step)) {
                    executeStep(step);
                } else {
                    log.debug("Step {} waiting for dependencies", step.getId());
                }
            } catch (Exception e) {
                log.error("Failed to execute step {}: {}", step.getId(), e.getMessage());
                handleStepFailure(step, e.getMessage());
            }
        }
    }

    private void executeStep(SagaStepEntity step) {
        log.info("Executing step {} for transaction {}",
                step.getId(), step.getTransactionId());

        step.setStatus("IN_PROGRESS");
        step.setStartedAt(LocalDateTime.now());
        sagaStepRepository.save(step);

        try {
            String stepType = step.getStepType();

            if (stepType == null) {
                log.error("Step type is null for step {}", step.getId());
                step.setStatus("FAILED");
                step.setErrorMessage("Step type is null");
                sagaStepRepository.save(step);
                return;
            }

            switch (stepType.toUpperCase()) {
                case "VALIDATE_ORDER":
                    externalServiceClient.validateOrder(step.getTransactionId());
                    break;
                case "CHECK_STOCK":
                    externalServiceClient.checkStock(step.getTransactionId());
                    break;
                case "NOTIFY_OFFICE":
                    externalServiceClient.notifyOffice(step.getTransactionId());
                    break;
                case "NOTIFY_COLLECTOR":
                    externalServiceClient.notifyCollector(step.getTransactionId());
                    break;
                case "RESERVE_ITEMS":
                    externalServiceClient.reserveItems(step.getTransactionId());
                    break;
                case "CREATE_DELIVERY":
                    createDelivery(step);
                    break;
                case "PROCESS_PAYMENT":
                    processPayment(step);
                    break;
                case "SEND_CONFIRMATION":
                    sendConfirmation(step);
                    break;
                default:
                    log.warn("Unknown step type: {}", stepType);
                    step.markAsSkipped();
                    sagaStepRepository.save(step);
                    return;
            }

            step.setStatus("COMPLETED");
            step.setCompletedAt(LocalDateTime.now());
            sagaStepRepository.save(step);

            log.info("Step {} completed successfully", step.getId());

            checkTransactionCompletion(step.getTransactionId());

        } catch (Exception e) {
            log.error("Step execution failed: {}", e.getMessage(), e);
            step.setStatus("FAILED");
            step.setErrorMessage(e.getMessage());
            sagaStepRepository.save(step);
            throw new RuntimeException("Step execution failed: " + e.getMessage(), e);
        }
    }

    private void createDelivery(SagaStepEntity step) {
        log.info("Creating delivery for transaction: {}", step.getTransactionId());
        externalServiceClient.updateTransactionStatus(step.getTransactionId(), "DELIVERY_CREATED");
    }

    private void processPayment(SagaStepEntity step) {
        log.info("Processing payment for transaction: {}", step.getTransactionId());
        externalServiceClient.updateTransactionStatus(step.getTransactionId(), "PAYMENT_PROCESSED");
    }

    private void sendConfirmation(SagaStepEntity step) {
        log.info("Sending confirmation for transaction: {}", step.getTransactionId());
        externalServiceClient.updateTransactionStatus(step.getTransactionId(), "CONFIRMATION_SENT");
    }

    private boolean checkDependencies(SagaStepEntity step) {
        Long dependsOnStepId = step.getDependsOnStepId();
        if (dependsOnStepId == null) {
            return true;
        }

        Optional<SagaStepEntity> dependency = sagaStepRepository.findById(dependsOnStepId);
        if (dependency.isPresent()) {
            SagaStepEntity depStep = dependency.get();
            String status = depStep.getStatus();
            return "COMPLETED".equals(status) || "SKIPPED".equals(status);
        }

        return true;
    }

    private void checkTransactionCompletion(String transactionId) {
        List<SagaStepEntity> allSteps = sagaStepRepository.findByTransactionId(transactionId);

        boolean allCompleted = allSteps.stream()
                .allMatch(step -> {
                    String status = step.getStatus();
                    return "COMPLETED".equals(status) || "SKIPPED".equals(status);
                });

        if (allCompleted) {
            Optional<TransactionEntity> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isPresent()) {
                TransactionEntity transaction = transactionOpt.get();
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);

                log.info("Transaction {} completed successfully", transactionId);
                externalServiceClient.logAuditEvent(transactionId, "TRANSACTION_COMPLETED",
                        "All saga steps completed successfully");
            }
        }
    }

    private void handleStepFailure(SagaStepEntity step, String error) {
        log.error("Handling failure for step {}: {}", step.getId(), error);

        step.setStatus("FAILED");
        step.setErrorMessage(error);
        sagaStepRepository.save(step);

        if (step.canRetry()) {
            step.incrementRetryCount();
            log.info("Step {} scheduled for retry (attempt {}/{})",
                    step.getId(), step.getRetryCount(), step.getMaxRetries());
        } else {
            compensationService.initiateCompensation(
                    step.getTransactionId(),
                    "STEP_FAILED: " + step.getStepType(),
                    error
            );

            Optional<TransactionEntity> transactionOpt = transactionRepository.findById(step.getTransactionId());
            transactionOpt.ifPresent(transaction -> {
                transaction.setStatus(TransactionStatus.COMPENSATING);
                transactionRepository.save(transaction);
            });

            log.error("Step {} failed permanently. Compensation initiated.", step.getId());
            externalServiceClient.logAuditEvent(step.getTransactionId(), "STEP_FAILED",
                    "Step " + step.getId() + " failed: " + error);
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryFailedSteps() {
        List<SagaStepEntity> failedSteps = sagaStepRepository.findByStatus("FAILED");

        for (SagaStepEntity step : failedSteps) {
            if (step.shouldRetryNow()) {
                log.info("Retrying failed step {} for transaction {}",
                        step.getId(), step.getTransactionId());
                step.setStatus("PENDING");
                step.incrementRetryCount();
                sagaStepRepository.save(step);
            }
        }
    }

    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void cleanupCompletedTransactions() {
        List<TransactionEntity> completedTransactions =
                transactionRepository.findByStatus(TransactionStatus.COMPLETED);

        for (TransactionEntity transaction : completedTransactions) {
            LocalDateTime completedAt = transaction.getCompletedAt();
            if (completedAt != null &&
                    completedAt.plusHours(24).isBefore(LocalDateTime.now())) {
                log.info("Archiving old completed transaction: {}", transaction.getId());
            }
        }
    }

    public void triggerCompensation(String transactionId, String reason, String details) {
        log.warn("Manual compensation triggered for {}: {} - {}", transactionId, reason, details);
        compensationService.initiateCompensation(transactionId, reason, details);
    }

    public List<SagaStepEntity> getTransactionSteps(String transactionId) {
        return sagaStepRepository.findByTransactionId(transactionId).stream()
                .sorted(Comparator.comparingInt(SagaStepEntity::getStepOrder))
                .collect(Collectors.toList());
    }

    public SagaStepEntity getStepStatus(Long stepId) {
        return sagaStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepId));
    }

    public void manuallySkipStep(Long stepId) {
        SagaStepEntity step = sagaStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepId));

        String status = step.getStatus();
        if ("PENDING".equals(status) || "FAILED".equals(status)) {
            step.markAsSkipped();
            sagaStepRepository.save(step);
            log.info("Step {} manually skipped", stepId);
        } else {
            throw new RuntimeException("Cannot skip step in status: " + status);
        }
    }

    public void resetStep(Long stepId) {
        SagaStepEntity step = sagaStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepId));

        step.setStatus("PENDING");
        step.setRetryCount(0);
        step.setNextRetryTime(null);
        step.setErrorMessage(null);
        sagaStepRepository.save(step);

        log.info("Step {} reset to PENDING", stepId);
    }
}