package com.kefir.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class UnifiedPaymentController {

    @Autowired
    private PayBackService payBackService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentCartsRepository paymentCartsRepository;

    @Autowired
    private PayBackScheduler payBackScheduler;

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

            if (!"client".equalsIgnoreCase(role)) {
                response.put("status", "error");
                response.put("message", "Account can only be created for clients");
                return ResponseEntity.badRequest().body(response);
            }

            // Убрали cardNumber из вызова!
            PaymentAccount account = paymentService.createAccountForClient(userId);

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

    @PostMapping("/card-payment")
    public ResponseEntity<Map<String, Object>> cardPayment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer cardId = Integer.valueOf(request.get("card_id").toString());
            Long userId = Long.valueOf(request.get("user_id").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String orderId = request.get("order_id").toString();
            String inputCvv = request.get("cvv").toString();  // CVV из запроса

            // Получаем карту
            PaymentCarts card = paymentCartsRepository.findById(cardId)
                    .orElseThrow(() -> new RuntimeException("Card not found"));

            // ПРОВЕРКА CVV КОДА
            String realCvv = card.getCvv(); // Получаем CVV из БД

            if (!realCvv.equals(inputCvv)) {
                response.put("status", "error");
                response.put("message", "Invalid CVV code");
                return ResponseEntity.badRequest().body(response);
            }

            // Проверяем достаточно ли средств
            if (card.getBalans().compareTo(amount) < 0) {
                response.put("status", "error");
                response.put("message", "Insufficient funds on card");
                return ResponseEntity.badRequest().body(response);
            }

            // Получаем баланс ДО списания
            BigDecimal balanceBefore = card.getBalans();

            // Списываем с карты
            card.setBalans(card.getBalans().subtract(amount));
            paymentCartsRepository.save(card);

            // Зачисляем на системный счет
            PaymentAccount systemAccount = paymentRepository.findByUserId(-1L)
                    .orElseThrow(() -> new RuntimeException("System account not found"));
            systemAccount.setCash(systemAccount.getCash().add(amount));
            paymentRepository.save(systemAccount);

            // Создаем транзакцию
            paymentService.createTransaction(
                    userId,
                    amount.negate(),
                    "CARD_PAYMENT",
                    orderId,
                    "Card payment for order",
                    balanceBefore,
                    card.getBalans(),
                    -1L
            );

            response.put("status", "success");
            response.put("message", "Card payment successful");
            response.put("new_card_balance", card.getBalans());
            response.put("new_balance", systemAccount.getCash());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // Остальные методы без изменений...
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

    @GetMapping("/payback/process")
    public ResponseEntity<Map<String, Object>> processPayBack() {
        try {
            Map<String, Object> result = payBackService.processPayBackRecords();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/payback/status")
    public ResponseEntity<Map<String, Object>> getPayBackStatus() {
        try {
            Map<String, Object> status = payBackService.getPayBackStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/payback/scheduler-status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        try {
            Map<String, Object> status = payBackScheduler.getSchedulerStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/create-cart")
    public ResponseEntity<Map<String, Object>> createPaymentCart(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("user_id").toString());
            String cardNumber = request.get("card_number").toString();

            Map<String, Object> result = paymentService.createPaymentCart(userId, cardNumber);

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

    @GetMapping("/card-info/{userId}")
    public ResponseEntity<Map<String, Object>> getCardInfo(@PathVariable Long userId) {
        try {
            // Ищем карту в таблице payment_carts
            Optional<PaymentCarts> cartOpt = paymentCartsRepository.findByIdUsers(userId);

            Map<String, Object> response = new HashMap<>();

            if (cartOpt.isPresent()) {
                PaymentCarts cart = cartOpt.get();
                response.put("status", "success");
                response.put("user_id", userId);
                response.put("id", cart.getId());
                response.put("cardNumber", maskCardNumber(cart.getCartNumber()));
                response.put("balance", cart.getBalans());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "success");
                response.put("user_id", userId);
                response.put("id", null);
                response.put("cardNumber", null);
                response.put("balance", 0);
                response.put("message", "No card found");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // Вспомогательный метод для маскировки номера карты
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}