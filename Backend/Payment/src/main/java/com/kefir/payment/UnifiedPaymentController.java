package com.kefir.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class UnifiedPaymentController {

    private final PaymentService paymentService;

    public UnifiedPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-client-account")
    public ResponseEntity<Map<String, Object>> createClientAccount(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = Long.valueOf(request.get("user_id").toString());
            String role = request.get("role").toString();
            String cardNumber = request.containsKey("card_number") ?
                    request.get("card_number").toString() : null;

            if (!"client".equalsIgnoreCase(role)) {
                response.put("status", "error");
                response.put("message", "Account can only be created for clients");
                return ResponseEntity.badRequest().body(response);
            }

            PaymentAccount account = paymentService.createAccountForClient(userId, cardNumber);
            response.put("status", "success");
            response.put("message", "Payment account created");
            response.put("account_id", account.getId());
            response.put("user_id", account.getUserId());
            response.put("initial_balance", account.getCash());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/handle-role-change")
    public ResponseEntity<Map<String, Object>> handleRoleChange(@RequestBody Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();

        try {
            paymentService.handleUserRoleChange(userData);
            response.put("status", "success");
            response.put("message", "Payment account updated based on role change");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getBalanceResponse(userId));
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = Long.valueOf(request.get("user_id").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String orderId = request.containsKey("order_id") ?
                    request.get("order_id").toString() : null;

            PaymentAccount account = paymentService.deposit(userId, amount, orderId);
            response.put("status", "success");
            response.put("message", "Deposit successful");
            response.put("new_balance", account.getCash());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("user_id").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String orderId = request.containsKey("order_id") ?
                    request.get("order_id").toString() : null;

            // withdraw возвращает Map, а не PaymentAccount
            Map<String, Object> result = paymentService.withdraw(userId, amount, orderId);

            if ("error".equals(result.get("status"))) {
                return ResponseEntity.badRequest().body(result);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/account-exists/{userId}")
    public ResponseEntity<Map<String, Object>> accountExists(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.accountExistsResponse(userId));
    }

    @DeleteMapping("/delete-account/{userId}")
    public ResponseEntity<Map<String, Object>> deleteAccount(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.deleteAccountResponse(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(paymentService.health());
    }
}