package com.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoderConfig.PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private AuthService authService;

    @Autowired
    private SessionService sessionService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            System.out.println("=== AUTH SERVICE LOGIN (HYBRID UUID+JWT) ===");

            String username = request.get("username");
            String password = request.get("password");

            System.out.println("Username: " + username);

            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                System.out.println("User not found: " + username);
                return ResponseEntity.status(400)
                        .body(Map.of(
                                "success", false,
                                "error", "Пользователь не найден"
                        ));
            }

            User user = userOpt.get();
            System.out.println("User found: " + user.getUsername());

            // Проверяем статус
            if ("banned".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(403)
                        .body(Map.of(
                                "success", false,
                                "error", "Ваш аккаунт заблокирован",
                                "status", "banned"
                        ));
            }

            // Проверяем пароль с BCrypt
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            System.out.println("Password matches: " + passwordMatches);

            if (passwordMatches) {
                // === ГИБРИДНЫЙ ПОДХОД (UUID + JWT) ===

                // 1. Генерируем UUID для сессии
                String sessionUUID = UUID.randomUUID().toString();
                System.out.println("Generated session UUID: " + sessionUUID);

                // 2. Создаем упрощенный токен для клиента (UUID-based)
                String clientToken = "auth-" + sessionUUID;

                // 3. Создаем JWT с дополнительными claims
                String jwtToken = null;
                try {
                    jwtToken = createJwtToken(user, sessionUUID);
                    System.out.println("JWT created successfully");
                } catch (Exception jwtError) {
                    System.err.println("⚠️ JWT creation failed: " + jwtError.getMessage());
                    // Продолжаем без JWT
                    jwtToken = "jwt-error-" + sessionUUID;
                }

                // 4. Сохраняем сессию в базе через SessionService
                try {
                    sessionService.createUserSession(sessionUUID, user.getId(), jwtToken);
                    System.out.println("Session saved successfully");
                } catch (Exception sessionError) {
                    System.err.println("⚠️ Session creation failed: " + sessionError.getMessage());
                }

                // 5. Формируем ответ
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("token", clientToken);  // Клиент получает UUID-based токен
                response.put("sessionId", sessionUUID);
                response.put("user", createUserResponse(user));
                response.put("message", "Вход выполнен успешно");

                if (jwtToken != null && !jwtToken.startsWith("jwt-error-")) {
                    response.put("jwtToken", jwtToken);
                    response.put("tokenType", "hybrid-uuid-jwt");
                } else {
                    response.put("tokenType", "uuid-only");
                    response.put("warning", "JWT не сгенерирован, используется только UUID");
                }

                System.out.println("✅ Login successful for user: " + username);
                System.out.println("Response structure: " + response.keySet());

                return ResponseEntity.ok(response);

            } else {
                System.out.println("Invalid password for user: " + username);
                return ResponseEntity.status(400)
                        .body(Map.of(
                                "success", false,
                                "error", "Неверный пароль"
                        ));
            }

        } catch (Exception e) {
            System.err.println("ERROR in login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "error", "Внутренняя ошибка сервера: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/logout-test")
    public ResponseEntity<?> logoutTest() {
        System.out.println("=== LOGOUT TEST ENDPOINT ===");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logout endpoint is working",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @RequestParam(value = "clientToken", required = false) String clientToken) {
        try {
            System.out.println("=== VALIDATE TOKEN (DUAL MODE) ===");

            String tokenToValidate = null;
            String tokenType = "unknown";

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                tokenToValidate = authHeader.substring(7);
                tokenType = "jwt";
                System.out.println("Validating JWT token from Authorization header");
            }
            else if (clientToken != null && clientToken.startsWith("auth-")) {
                tokenToValidate = clientToken;
                tokenType = "uuid";
                System.out.println("Validating UUID token from clientToken");
            }

            if (tokenToValidate == null) {
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "valid", false,
                                "message", "No valid token provided"
                        ));
            }

            Map<String, Object> validationResult;

            if ("jwt".equals(tokenType)) {
                validationResult = validateJwtToken(tokenToValidate);
            } else {
                validationResult = validateUuidToken(tokenToValidate);
            }

            boolean isValid = (boolean) validationResult.get("valid");

            if (isValid) {
                return ResponseEntity.ok(validationResult);
            } else {
                return ResponseEntity.status(401)
                        .body(validationResult);
            }

        } catch (Exception e) {
            System.err.println("ERROR in validateToken: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "valid", false,
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @RequestParam(value = "clientToken", required = false) String clientToken) {
        try {
            Optional<User> userOpt = Optional.empty();

            if (clientToken != null && clientToken.startsWith("auth-")) {
                userOpt = getUserByUuidToken(clientToken);
            }
            else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                userOpt = getUserByJwtToken(authHeader.substring(7));
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "user", createUserResponse(user)
                ));
            } else {
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "success", false,
                                "error", "Authentication required"
                        ));
            }

        } catch (Exception e) {
            System.err.println("ERROR in getCurrentUser: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Failed to get current user"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    @RequestParam(value = "clientToken", required = false) String clientToken) {
        try {
            System.out.println("=== AUTH SERVICE LOGOUT ===");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");
            response.put("timestamp", System.currentTimeMillis());

            boolean tokenInvalidated = false;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwtToken = authHeader.substring(7);
                try {
                    String username = jwtUtil.extractUsername(jwtToken);
                    if (jwtUtil.validateToken(jwtToken, username)) {
                        tokenBlacklistService.blacklistToken(jwtToken);
                        response.put("details", "JWT token invalidated");
                        response.put("username", username);
                        tokenInvalidated = true;
                        System.out.println("JWT Token blacklisted for user: " + username);
                    }
                } catch (Exception e) {
                    System.out.println("JWT validation failed during logout: " + e.getMessage());
                }
            }

            if (clientToken != null && clientToken.startsWith("auth-")) {
                String sessionUUID = clientToken.substring(5);
                sessionService.invalidateSession(sessionUUID);
                response.put("details", "UUID session invalidated");
                tokenInvalidated = true;
                System.out.println("UUID session invalidated: " + sessionUUID);
            }

            if (!tokenInvalidated) {
                response.put("details", "No valid token provided");
                System.out.println("Logout without valid token");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("ERROR in logout: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Both current and new password are required"));
            }

            String username = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                username = jwtUtil.extractUsername(token);
                if (!jwtUtil.validateToken(token, username)) {
                    return ResponseEntity.status(401)
                            .body(Map.of("success", false, "error", "Invalid or expired token"));
                }
            } else {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "error", "Authorization required"));
            }

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("success", false, "error", "User not found"));
            }

            User user = userOpt.get();

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.status(400)
                        .body(Map.of("success", false, "error", "Current password is incorrect"));
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tokenBlacklistService.blacklistToken(token);
            }

            sessionService.invalidateAllUserSessions(user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password changed successfully. Please login again."
            ));

        } catch (Exception e) {
            System.err.println("ERROR in changePassword: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Failed to change password"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                          @RequestParam(value = "clientToken", required = false) String clientToken) {
        try {
            System.out.println("=== REFRESH TOKEN ===");

            String username = null;
            User user = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String oldToken = authHeader.substring(7);
                username = jwtUtil.extractUsername(oldToken);
                if (!jwtUtil.validateToken(oldToken, username)) {
                    return ResponseEntity.status(401)
                            .body(Map.of("success", false, "error", "Invalid or expired token"));
                }
                user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            }
            else if (clientToken != null && clientToken.startsWith("auth-")) {
                Optional<UserSession> sessionOpt = sessionService.validateSession(clientToken);
                if (sessionOpt.isEmpty()) {
                    return ResponseEntity.status(401)
                            .body(Map.of("success", false, "error", "Invalid or expired session"));
                }
                user = userRepository.findById(sessionOpt.get().getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                username = user.getUsername();
            } else {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "error", "No valid token provided"));
            }

            String sessionUUID = UUID.randomUUID().toString();
            String newJwtToken = createJwtToken(user, sessionUUID);
            String newClientToken = "auth-" + sessionUUID;

            sessionService.createUserSession(sessionUUID, user.getId(), newJwtToken);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String oldToken = authHeader.substring(7);
                tokenBlacklistService.blacklistToken(oldToken);
            }

            if (clientToken != null && clientToken.startsWith("auth-")) {
                String oldSessionUUID = clientToken.substring(5);
                sessionService.invalidateSession(oldSessionUUID);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", newClientToken);
            response.put("jwtToken", newJwtToken);
            response.put("sessionId", sessionUUID);
            response.put("expiresIn", 86400000L);
            response.put("message", "Token refreshed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("ERROR in refreshToken: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Failed to refresh token"));
        }
    }

    @GetMapping("/security/hash-password")
    public ResponseEntity<?> hashPassword(@RequestParam String password) {
        try {
            String hashedPassword = passwordEncoder.encode(password);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hashedPassword", hashedPassword,
                    "algorithm", "BCrypt"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Failed to hash password"));
        }
    }

    @PostMapping("/security/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestBody Map<String, String> request) {
        try {
            String rawPassword = request.get("rawPassword");
            String hashedPassword = request.get("hashedPassword");

            if (rawPassword == null || hashedPassword == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Both rawPassword and hashedPassword are required"));
            }

            boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "matches", matches,
                    "algorithm", "BCrypt"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Failed to verify password"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            long totalUsers = userRepository.count();
            long blacklistedTokens = tokenBlacklistService.getBlacklistedTokensCount();
            long activeSessions = sessionService.getActiveSessionsCount();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("blacklistedTokens", blacklistedTokens);
            stats.put("activeSessions", activeSessions);
            stats.put("service", "Auth Service");
            stats.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Failed to get stats"));
        }
    }

    @GetMapping("/system/info")
    public ResponseEntity<?> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Authentication Service");
        info.put("version", "2.0.0");
        info.put("authentication", "Hybrid (JWT + UUID)");
        info.put("passwordEncoding", "BCrypt");
        info.put("database", "PostgreSQL");
        info.put("timestamp", new Date().toString());
        info.put("features", Arrays.asList(
                "JWT Token Authentication",
                "UUID Session Management",
                "Token Blacklisting",
                "BCrypt Password Hashing",
                "Session Invalidation",
                "Dual Token Validation"
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "info", info
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Auth Service");
        health.put("timestamp", new Date().toString());
        health.put("version", "1.0.0");

        try {
            userRepository.count();
            health.put("database", "Connected");
        } catch (Exception e) {
            health.put("database", "Disconnected: " + e.getMessage());
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth service is working!");
    }

    @GetMapping("/users/count")
    public ResponseEntity<?> getUsersCount() {
        try {
            long count = userRepository.count();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Database connection error", "message", e.getMessage()));
        }
    }

    @GetMapping("/check-user/{username}")
    public ResponseEntity<?> checkUser(@PathVariable String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return ResponseEntity.ok(Map.of(
                        "exists", true,
                        "user", Map.of(
                                "id", user.getId(),
                                "username", user.getUsername(),
                                "role", user.getRole()
                        )
                ));
            } else {
                return ResponseEntity.ok(Map.of("exists", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to check user", "message", e.getMessage()));
        }
    }

    private String createJwtToken(User user, String sessionUUID) {
        try {
            System.out.println("Creating JWT token for user: " + user.getUsername());

            Map<String, Object> claims = new HashMap<>();
            claims.put("sessionId", sessionUUID);
            claims.put("userId", user.getId());
            claims.put("role", user.getRole());
            claims.put("status", user.getStatus());
            claims.put("firstname", user.getFirstname());
            claims.put("email", user.getEmail());

            String jwtToken = jwtUtil.generateTokenWithClaims(user.getUsername(), claims);
            System.out.println("✅ JWT token created successfully");
            System.out.println("JWT Token preview: " + (jwtToken != null ?
                    jwtToken.substring(0, Math.min(50, jwtToken.length())) + "..." : "null"));

            return jwtToken;
        } catch (Exception e) {
            System.err.println("❌ Failed to create JWT token: " + e.getMessage());
            e.printStackTrace();

            // Временный токен на случай ошибки JWT
            return "temp-jwt-" + sessionUUID + "-" + System.currentTimeMillis();
        }
    }

    private Map<String, Object> validateJwtToken(String token) {
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            return Map.of(
                    "valid", false,
                    "message", "Token has been invalidated (logged out)"
            );
        }

        String username = jwtUtil.extractUsername(token);
        boolean isValid = jwtUtil.validateToken(token, username);

        if (isValid) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("tokenType", "jwt");
            response.put("username", username);
            response.put("userId", jwtUtil.getClaimFromToken(token, claims -> claims.get("userId", Integer.class)));
            response.put("role", jwtUtil.getClaimFromToken(token, claims -> claims.get("role", String.class)));
            response.put("status", jwtUtil.getClaimFromToken(token, claims -> claims.get("status", String.class)));
            response.put("sessionId", jwtUtil.getClaimFromToken(token, claims -> claims.get("sessionId", String.class)));
            response.put("message", "JWT token is valid");

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                response.put("user", createUserResponse(userOpt.get()));
            }

            return response;
        } else {
            return Map.of(
                    "valid", false,
                    "message", "JWT token is invalid or expired"
            );
        }
    }

    private Map<String, Object> validateUuidToken(String clientToken) {
        String sessionUUID = clientToken.substring(5);

        Optional<UserSession> sessionOpt = sessionService.validateSession(clientToken);

        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            Optional<User> userOpt = userRepository.findById(session.getUserId());

            if (userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("tokenType", "uuid");
                response.put("sessionId", sessionUUID);
                response.put("userId", session.getUserId());
                response.put("user", createUserResponse(userOpt.get()));
                response.put("message", "UUID session is valid");
                return response;
            }
        }

        return Map.of(
                "valid", false,
                "message", "UUID session is invalid or expired"
        );
    }

    private Optional<User> getUserByJwtToken(String jwtToken) {
        try {
            if (tokenBlacklistService.isTokenBlacklisted(jwtToken)) {
                return Optional.empty();
            }

            String username = jwtUtil.extractUsername(jwtToken);
            if (jwtUtil.validateToken(jwtToken, username)) {
                return userRepository.findByUsername(username);
            }
        } catch (Exception e) {
            System.err.println("Error getting user by JWT: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<User> getUserByUuidToken(String clientToken) {
        Optional<UserSession> sessionOpt = sessionService.validateSession(clientToken);
        if (sessionOpt.isPresent()) {
            return userRepository.findById(sessionOpt.get().getUserId());
        }
        return Optional.empty();
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("firstname", user.getFirstname() != null ? user.getFirstname() : "");
        userResponse.put("age", user.getAge() != null ? user.getAge() : 0);
        userResponse.put("city", user.getCity() != null ? user.getCity() : "");
        userResponse.put("magaz", user.getMagaz() != null ? user.getMagaz() : "");
        userResponse.put("email", user.getEmail() != null ? user.getEmail() : "");
        userResponse.put("status", user.getStatus() != null ? user.getStatus() : "active");
        userResponse.put("role", user.getRole() != null ? user.getRole() : "client");
        userResponse.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        userResponse.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "");
        return userResponse;
    }
}