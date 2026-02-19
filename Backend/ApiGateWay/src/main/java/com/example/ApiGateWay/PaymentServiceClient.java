package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "payment-service", url = "http://localhost:8099")
public interface PaymentServiceClient {

    // ========== УПРАВЛЕНИЕ СЧЕТАМИ ==========

    @PostMapping("/api/payments/create-client-account")
    ResponseEntity<Map<String, Object>> createClientAccount(@RequestBody Map<String, Object> request);

    @PostMapping("/api/payments/create-account")
    ResponseEntity<Map<String, Object>> createAccount(@RequestBody Map<String, Object> request);

    @PostMapping("/api/payments/handle-role-change")
    ResponseEntity<Map<String, Object>> handleRoleChange(@RequestBody Map<String, Object> userData);

    // ========== ОПЕРАЦИИ СО СРЕДСТВАМИ ==========

    @PostMapping("/api/payments/deposit")
    ResponseEntity<Map<String, Object>> deposit(@RequestBody Map<String, Object> request);

    @PostMapping("/api/payments/withdraw")
    ResponseEntity<Map<String, Object>> withdraw(@RequestBody Map<String, Object> request);

    @PostMapping("/api/payments/refund")
    ResponseEntity<Map<String, Object>> refund(@RequestBody Map<String, Object> request);

    // ========== ЛОГИСТИЧЕСКИЕ ОПЕРАЦИИ ==========

    @PostMapping("/api/payments/pay-order")
    ResponseEntity<Map<String, Object>> payForOrder(@RequestBody Map<String, Object> request);

    @PostMapping("/api/payments/return-order-payment")
    ResponseEntity<Map<String, Object>> returnOrderPayment(@RequestBody Map<String, Object> request);

    @PostMapping("/api/payments/add-bonus")
    ResponseEntity<Map<String, Object>> addBonus(@RequestBody Map<String, Object> request);

    // ========== ИНФОРМАЦИОННЫЕ ЗАПРОСЫ ==========

    @GetMapping("/api/payments/balance/{userId}")
    ResponseEntity<Map<String, Object>> getBalance(@PathVariable("userId") Long userId);

    @GetMapping("/api/payments/transactions/{userId}")
    ResponseEntity<Map<String, Object>> getTransactionHistory(@PathVariable("userId") Long userId);

    @GetMapping("/api/payments/order-transactions/{orderId}")
    ResponseEntity<Map<String, Object>> getOrderTransactions(@PathVariable("orderId") Long orderId);

    @GetMapping("/api/payments/account-exists/{userId}")
    ResponseEntity<Map<String, Object>> accountExists(@PathVariable("userId") Long userId);

    // ========== УПРАВЛЕНИЕ СЧЕТАМИ ==========

    @DeleteMapping("/api/payments/delete-account/{userId}")
    ResponseEntity<Map<String, Object>> deleteAccount(@PathVariable("userId") Long userId);

    // ========== HEALTH CHECK ==========

    @GetMapping("/api/payments/health")
    ResponseEntity<Map<String, Object>> PaymentsHealth();

    @PostMapping("/api/payments/transfer")
    ResponseEntity<Map<String, Object>> transfer(@RequestBody Map<String, Object> request);

    @GetMapping("/api/payments/payback/process")
    ResponseEntity<Map<String, Object>> processPayBack();

    @PostMapping("/api/payments/create-cart")
    ResponseEntity<Map<String, Object>> createPaymentCart(@RequestBody Map<String, Object> request);

    @GetMapping("/api/payments/card-info/{userId}")
    ResponseEntity<Map<String, Object>> getCardInfo(@PathVariable("userId") Long userId);

    @GetMapping("/api/payments/payback/status")
    ResponseEntity<Map<String, Object>> getPayBackStatus();

    @GetMapping("/api/payments/payback/scheduler-status")
    ResponseEntity<Map<String, Object>> getPayBackSchedulerStatus();

    @PostMapping("/api/payments/card-payment")
    ResponseEntity<Map<String, Object>> cardPayment(@RequestBody Map<String, Object> request);
}