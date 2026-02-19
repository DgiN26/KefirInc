package com.kefir.payment;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class PaymentService {

    @Autowired
    private PaymentCartsRepository paymentCartsRepository;

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final Random random = new Random();

    // ID системного счета
    private static final Long SYSTEM_USER_ID = -1L;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentTransactionRepository transactionRepository) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public PaymentAccount createAccountForClient(Long userId) {  // Без cardNumber!
        if (paymentRepository.existsByUserId(userId)) {
            throw new RuntimeException("Account already exists for user: " + userId);
        }

        double min = 100.0;
        double max = 10000.0;
        double randomCash = min + (max - min) * random.nextDouble();
        BigDecimal cash = BigDecimal.valueOf(randomCash)
                .setScale(2, RoundingMode.HALF_UP);

        PaymentAccount account = new PaymentAccount(userId, cash);  // Конструктор без cardNumber
        account = paymentRepository.save(account);

        createTransaction(userId, cash, "ACCOUNT_CREATION", null,
                "Initial account funding", BigDecimal.ZERO, cash, null);

        return account;
    }

    @Transactional
    public PaymentAccount deposit(Long userId, BigDecimal amount, String orderId) {
        PaymentAccount account = updateCash(userId, amount);

        createTransaction(userId, amount, "DEPOSIT", orderId,
                orderId != null ? "Пополнение счета" : "Пополнение счета",
                account.getCash().subtract(amount), account.getCash(), null);

        return account;
    }

    @Transactional
    public Map<String, Object> withdraw(Long userId, BigDecimal amount, String orderId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Ищем системный счет по user_id = -1
            PaymentAccount systemAccount = paymentRepository.findByUserId(SYSTEM_USER_ID)
                    .orElseThrow(() -> new RuntimeException("Системный счет не найден"));

            PaymentAccount userAccount = paymentRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Счет пользователя не найден"));

            if (userAccount.getCash().compareTo(amount) < 0) {
                response.put("status", "error");
                response.put("message", "Недостаточно средств");
                return response;
            }

            userAccount.setCash(userAccount.getCash().subtract(amount));
            paymentRepository.save(userAccount);

            systemAccount.setCash(systemAccount.getCash().add(amount));
            paymentRepository.save(systemAccount);

            // 2. В транзакции сохраняем user_id системного счета (который -1)
            createTransaction(userId, amount.negate(), "WITHDRAWAL", orderId,
                    "Оплата заказа",
                    userAccount.getCash().add(amount), userAccount.getCash(),
                    SYSTEM_USER_ID);  // ← передаем -1, а не id счета

            response.put("status", "success");
            response.put("message", "Оплата выполнена");
            response.put("new_balance", userAccount.getCash());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @Transactional
    public Map<String, Object> refund(Long userId, BigDecimal amount, String orderId, String reason) {
        Map<String, Object> response = new HashMap<>();

        try {
            PaymentAccount systemAccount = paymentRepository.findByUserId(SYSTEM_USER_ID)
                    .orElseThrow(() -> new RuntimeException("Системный счет не найден"));

            PaymentAccount userAccount = paymentRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Счет пользователя не найден"));

            if (systemAccount.getCash().compareTo(amount) < 0) {
                response.put("status", "error");
                response.put("message", "Недостаточно средств на системном счете");
                return response;
            }

            // 1. Списываем с системного счета
            systemAccount.setCash(systemAccount.getCash().subtract(amount));
            paymentRepository.save(systemAccount);

            // 2. Возвращаем пользователю
            userAccount.setCash(userAccount.getCash().add(amount));
            paymentRepository.save(userAccount);

            createTransaction(userId, amount, "REFUND", orderId,
                    "Возврат средств",
                    userAccount.getCash().subtract(amount), userAccount.getCash(),
                    systemAccount.getId());

            response.put("status", "success");
            response.put("message", "Возврат выполнен");
            response.put("new_balance", userAccount.getCash());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    private PaymentAccount updateCash(Long userId, BigDecimal amount) {
        PaymentAccount account = paymentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Account not found for user: " + userId));

        BigDecimal newCash = account.getCash().add(amount);
        if (newCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient funds. Current balance: " + account.getCash());
        }

        account.setCash(newCash.setScale(2, RoundingMode.HALF_UP));
        return paymentRepository.save(account);
    }

    public void createTransaction(Long userId, BigDecimal amount, String operationType,
                                   String orderId, String description,
                                   BigDecimal balanceBefore, BigDecimal balanceAfter,
                                   Long systemUserId) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setOperationType(operationType);
        transaction.setOrderId(orderId);
        transaction.setDescription(description);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setCreatedDate(LocalDateTime.now());
        transaction.setStatus("COMPLETED");
        transaction.setSystemAccountId(systemUserId); // Добавьте это поле в PaymentTransaction

        transactionRepository.save(transaction);
    }

    public Map<String, Object> getBalanceResponse(Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<PaymentAccount> accountOpt = paymentRepository.findByUserId(userId);

            if (accountOpt.isPresent()) {
                PaymentAccount account = accountOpt.get();
                response.put("status", "success");
                response.put("user_id", userId);
                response.put("balance", account.getCash());
            } else {
                response.put("status", "success");
                response.put("user_id", userId);
                response.put("balance", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                response.put("message", "No account found, returning zero balance");
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getSystemBalance() {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<PaymentAccount> systemAccount = paymentRepository.findByUserId(SYSTEM_USER_ID);

            if (systemAccount.isPresent()) {
                PaymentAccount account = systemAccount.get();
                response.put("status", "success");
                response.put("balance", account.getCash());
                response.put("description", account.getDescription());
            } else {
                response.put("status", "error");
                response.put("message", "Системный счет не найден");
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> accountExistsResponse(Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean exists = paymentRepository.existsByUserId(userId);

            response.put("status", "success");
            response.put("user_id", userId);
            response.put("account_exists", exists);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "payment-service");
        response.put("timestamp", System.currentTimeMillis());

        try {
            paymentRepository.count();
            response.put("database", "connected");

            // Проверяем наличие системного счета
            boolean systemExists = paymentRepository.existsByUserId(SYSTEM_USER_ID);
            response.put("system_account", systemExists ? "OK" : "MISSING");

        } catch (Exception e) {
            response.put("database", "disconnected");
        }

        return response;
    }

    // Остальные методы (getBalance, accountExists, deleteAccount и т.д.)
    public BigDecimal getBalance(Long userId) {
        return paymentRepository.findByUserId(userId)
                .map(PaymentAccount::getCash)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    public boolean accountExists(Long userId) {
        return paymentRepository.existsByUserId(userId);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        paymentRepository.findByUserId(userId)
                .ifPresent(paymentRepository::delete);
    }

    @PostConstruct
    public void init() {
        if (!paymentRepository.existsByUserId(SYSTEM_USER_ID)) {
            PaymentAccount systemAccount = new PaymentAccount();
            PaymentCarts systemCarts = new PaymentCarts();
            systemAccount.setUserId(SYSTEM_USER_ID);
            systemAccount.setCash(BigDecimal.ZERO);
            systemAccount.setDescription("Системный счет");
            systemCarts.setCartNumber("0000 0000 0000 0000");
            systemAccount.setCreatedAt(LocalDateTime.now());
            systemAccount.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(systemAccount);
        }
    }

    @Transactional
    public void handleUserRoleChange(Map<String, Object> userData) {
        Long userId = Long.valueOf(userData.get("id").toString());
        String newRole = userData.get("role").toString();

        Optional<PaymentAccount> accountOpt = paymentRepository.findByUserId(userId);

        if (accountOpt.isPresent() && !"client".equalsIgnoreCase(newRole)) {
            PaymentAccount account = accountOpt.get();
            BigDecimal oldBalance = account.getCash();

            account.setCash(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            paymentRepository.save(account);

            createTransaction(userId, oldBalance.negate(), "ACCOUNT_RESET", null,
                    "Account reset due to role change to " + newRole,
                    oldBalance, BigDecimal.ZERO, null);
        }
    }

    public Map<String, Object> deleteAccountResponse(Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            paymentRepository.findByUserId(userId)
                    .ifPresent(paymentRepository::delete);

            response.put("status", "success");
            response.put("message", "Account deleted successfully");

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    // Создание записи в payment_carts
    @Transactional
    public Map<String, Object> createPaymentCart(Long userId, String cardNumber) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Проверяем, существует ли уже карта для этого пользователя
            if (paymentCartsRepository.existsByIdUsers(userId)) {
                response.put("status", "error");
                response.put("message", "Card already exists for user: " + userId);
                return response;
            }

            // Проверяем, существует ли уже такой номер карты
            if (paymentCartsRepository.existsByCartNumber(cardNumber)) {
                response.put("status", "error");
                response.put("message", "Card number already exists");
                return response;
            }

            PaymentCarts paymentCart = new PaymentCarts(userId, cardNumber);
            paymentCart = paymentCartsRepository.save(paymentCart);

            response.put("status", "success");
            response.put("message", "Payment cart created successfully");
            response.put("id", paymentCart.getId());
            response.put("user_id", paymentCart.getIdUsers());
            response.put("cart_number", paymentCart.getCartNumber());
            response.put("balance", paymentCart.getBalans());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    // Получение информации о карте
    public Map<String, Object> getCardInfo(Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<PaymentCarts> cartOpt = paymentCartsRepository.findByIdUsers(userId);

            if (cartOpt.isPresent()) {
                PaymentCarts cart = cartOpt.get();
                response.put("status", "success");
                response.put("user_id", userId);
                response.put("cardNumber", maskCardNumber(cart.getCartNumber()));
                response.put("balance", cart.getBalans());
            } else {
                response.put("status", "success");
                response.put("user_id", userId);
                response.put("cardNumber", null);
                response.put("message", "No card found for user");
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    // Вспомогательный метод для маскирования номера карты
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}