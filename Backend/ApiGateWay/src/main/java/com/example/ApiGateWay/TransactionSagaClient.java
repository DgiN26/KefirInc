package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@FeignClient(name = "transaction-saga", url = "${services.transaction-saga.url:http://localhost:8090}")
public interface TransactionSagaClient {

    // ==================== ТРАНЗАКЦИИ ====================

    @PostMapping("/api/transactions")
    Map<String, Object> createTransaction(@RequestBody Map<String, Object> transactionRequest);

    @GetMapping("/api/transactions/{transactionId}")
    Map<String, Object> getTransaction(@PathVariable String transactionId);

    @GetMapping("/api/transactions/collector/{collectorId}")
    List<Map<String, Object>> getCollectorTransactions(@PathVariable String collectorId);

    @GetMapping("/api/transactions/active")
    List<Map<String, Object>> getActiveTransactions();

    @GetMapping("/api/transactions/paused")
    List<Map<String, Object>> getPausedTransactions();

    // ==================== СКАНИРОВАНИЕ ТОВАРОВ ====================

    @PostMapping("/api/transactions/{transactionId}/scan")
    Map<String, Object> scanItem(@PathVariable String transactionId,
                                 @RequestBody Map<String, Object> scanRequest);

    // ==================== СООБЩЕНИЕ О ПРОБЛЕМЕ ====================

    @PostMapping("/api/transactions/{transactionId}/report-problem")
    Map<String, Object> reportProblem(@PathVariable String transactionId,
                                      @RequestBody Map<String, Object> problemRequest);

    // ==================== РЕШЕНИЕ КЛИЕНТА ====================

    @PostMapping("/api/transactions/{transactionId}/client-decision")
    Map<String, Object> processClientDecision(@PathVariable String transactionId,
                                              @RequestBody Map<String, Object> decisionRequest);

    // ==================== УПРАВЛЕНИЕ САГОЙ ====================

    @GetMapping("/api/saga/steps/{transactionId}")
    List<Map<String, Object>> getTransactionSteps(@PathVariable String transactionId);

    @PostMapping("/api/saga/steps/{stepId}/retry")
    Map<String, Object> retryStep(@PathVariable Long stepId);

    @GetMapping("/api/saga/health")
    Map<String, Object> checkSagaHealth();

    // ==================== КОМПЕНСАЦИИ ====================

    @PostMapping("/api/compensation/{transactionId}/initiate")
    Map<String, Object> initiateCompensation(@PathVariable String transactionId,
                                             @RequestParam String reason,
                                             @RequestParam(required = false) String details);

    @GetMapping("/api/compensation/history/{transactionId}")
    List<Map<String, Object>> getCompensationHistory(@PathVariable String transactionId);

    // ==================== СТАТУС ====================

    @PutMapping("/api/transactions/{transactionId}/status")
    Map<String, Object> updateTransactionStatus(@PathVariable String transactionId,
                                                @RequestBody Map<String, Object> statusRequest);

    // ==================== КОМПЛЕКСНЫЕ ОПЕРАЦИИ ====================

    @PostMapping("/api/transactions/complete-order")
    Map<String, Object> createCompleteOrderWithSaga(@RequestBody Map<String, Object> orderRequest);

    @GetMapping("/api/transactions/{transactionId}/full-info")
    Map<String, Object> getTransactionFullInfo(@PathVariable String transactionId);
}