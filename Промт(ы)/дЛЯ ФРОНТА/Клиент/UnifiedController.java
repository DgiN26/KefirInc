
Это не полный код - а часть кода для нейросети без лишних блоков! package com.example.ApiGateWay;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UnifiedController {

    private static final Logger log = LoggerFactory.getLogger(UnifiedController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CollectorServiceClient collectorService;

    @Autowired
    private AuthServiceClient authServiceClient;

    @Autowired
    private ClientServiceClient clientService;
    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private CartServiceClient cartService;

    @Autowired
    private OfficeServiceClient officeService;

    @Autowired
    private DeliveryServiceClient deliveryService;

    @Autowired
    private TransactionSagaClient transactionSagaClient;

    // ==================== БЛОК 1: АВТОРИЗАЦИЯ И АУТЕНТИФИКАЦИЯ ====================

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            System.out.println("=== GATEWAY LOGIN (HYBRID SUPPORT) ===");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        "http://localhost:8097/api/auth/login",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                Map<String, Object> responseBody = response.getBody();

                if (responseBody != null &&
                        Boolean.TRUE.equals(responseBody.get("success")) &&
                        responseBody.containsKey("token")) {

                    String token = (String) responseBody.get("token");
                    if (token.startsWith("auth-")) {
                        System.out.println("✅ Received hybrid UUID token: " + token);
                    } else if (token.contains(".")) {
                        System.out.println("✅ Received JWT token");
                    }
                }

                return ResponseEntity.status(response.getStatusCode()).body(responseBody);

            } catch (HttpClientErrorException e) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    return ResponseEntity.status(e.getStatusCode())
                            .body(mapper.readValue(e.getResponseBodyAsString(), Map.class));
                } catch (Exception parseError) {
                    return ResponseEntity.status(e.getStatusCode())
                            .body(Map.of("success", false, "error", e.getResponseBodyAsString()));
                }
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Gateway error"));
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null) headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:8097/api/auth/logout",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            System.err.println("Gateway logout error: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout processed via gateway",
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @PostMapping("/auth/validate")
    public Map<String, Object> validateToken(@RequestBody Map<String, String> request) {
        return authServiceClient.validateToken(request.toString());
    }

    @GetMapping("/auth/check")
    public Map<String, Object> checkAuth() {
        return authServiceClient.check();
    }

    // Метод для извлечения userId из JWT токена (из первого файла)
    private Integer extractUserIdFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("⚠️ Отсутствует или некорректный Authorization header: {}", authHeader);
                throw new RuntimeException("Требуется авторизация");
            }

            String token = authHeader.substring(7);
            log.debug("Токен для парсинга: {}", token.substring(0, Math.min(token.length(), 50)) + "...");

            if (token.contains(".")) {
                return extractUserIdFromJwt(token);
            } else if (token.startsWith("auth-")) {
                return extractUserIdFromUuidToken(token);
            } else {
                throw new RuntimeException("Неизвестный формат токена");
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при извлечении userId: " + e.getMessage());
        }
    }

    private Integer extractUserIdFromJwt(String jwtToken) throws Exception {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Неверный формат JWT токена");
            }

            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            log.debug("JWT payload: {}", payloadJson);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(payloadJson, Map.class);

            if (payload.containsKey("userId")) {
                Object userIdObj = payload.get("userId");
                if (userIdObj instanceof Integer) return (Integer) userIdObj;
                if (userIdObj instanceof String) return Integer.parseInt((String) userIdObj);
                if (userIdObj instanceof Number) return ((Number) userIdObj).intValue();
            }

            if (payload.containsKey("id")) {
                Object idObj = payload.get("id");
                if (idObj instanceof Integer) return (Integer) idObj;
                if (idObj instanceof String) return Integer.parseInt((String) idObj);
                if (idObj instanceof Number) return ((Number) idObj).intValue();
            }

            throw new RuntimeException("userId не найден в JWT токене");

        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга JWT: " + e.getMessage());
        }
    }

    private Integer extractUserIdFromUuidToken(String uuidToken) {
        try {
            log.info("=== ИЗВЛЕЧЕНИЕ USER ID ИЗ UUID ТОКЕНА ===");
            log.info("Токен: {}", uuidToken);

            String url = "http://localhost:8097/api/auth/validate?clientToken=" + uuidToken;
            log.info("URL запроса: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            log.info("Отправка POST запроса с пустым телом и параметром в query string...");

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            log.info("Статус ответа: {}", response.getStatusCode());
            log.info("Тело ответа: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                if (Boolean.TRUE.equals(body.get("valid"))) {
                    log.info("✅ Токен валиден");

                    if (body.containsKey("userId")) {
                        Integer userId = convertToInteger(body.get("userId"));
                        if (userId != null) {
                            log.info("✅ Найден userId: {}", userId);
                            return userId;
                        }
                    }

                    if (body.containsKey("user") && body.get("user") instanceof Map) {
                        Map<String, Object> user = (Map<String, Object>) body.get("user");
                        if (user.containsKey("id")) {
                            Integer userId = convertToInteger(user.get("id"));
                            if (userId != null) {
                                log.info("✅ Найден userId в user объекте: {}", userId);
                                return userId;
                            }
                        }
                    }

                    log.error("❌ userId не найден в ответе");
                    throw new RuntimeException("Не удалось извлечь userId из ответа");

                } else {
                    String errorMsg = body.containsKey("message") ?
                            (String) body.get("message") : "Токен невалиден";
                    log.error("❌ Токен невалиден: {}", errorMsg);
                    throw new RuntimeException("Токен недействителен: " + errorMsg);
                }
            }

            log.error("❌ Неожиданный статус ответа: {}", response.getStatusCode());
            throw new RuntimeException("Неожиданный ответ от Auth Service: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("❌ Ошибка при извлечении userId: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обращении к Auth Service: " + e.getMessage());
        }
    }

    private Integer convertToInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof String) return Integer.parseInt((String) obj);
        if (obj instanceof Number) return ((Number) obj).intValue();
        throw new RuntimeException("Не могу преобразовать в Integer: " + obj.getClass());
    }

    @GetMapping("/test-auth-endpoint")
    public String testAuthEndpoint() {
        RestTemplate rt = new RestTemplate();
        String token = "auth-83f64f93-bd02-4392-bf92-37f28611868f";

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Тестирование Auth Service Endpoints</h2>");

        // 1. Проверим /api/auth/validate
        sb.append("<h3>1. /api/auth/validate</h3>");
        try {
            String url = "http://localhost:8097/api/auth/validate";

            // Вариант A: GET с параметром
            String urlA = url + "?clientToken=" + token;
            try {
                ResponseEntity<String> resp = rt.getForEntity(urlA, String.class);
                sb.append("<p><b>GET:</b> ").append(resp.getStatusCode()).append(" - ").append(resp.getBody()).append("</p>");
            } catch (Exception e) {
                sb.append("<p style='color:red'><b>GET Error:</b> ").append(e.getMessage()).append("</p>");
            }

            // Вариант B: POST с параметром в query
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>("{}", headers);
                ResponseEntity<String> resp = rt.exchange(urlA, HttpMethod.POST, entity, String.class);
                sb.append("<p><b>POST (param in query):</b> ").append(resp.getStatusCode()).append(" - ").append(resp.getBody()).append("</p>");
            } catch (Exception e) {
                sb.append("<p style='color:red'><b>POST Error:</b> ").append(e.getMessage()).append("</p>");
            }

        } catch (Exception e) {
            sb.append("<p style='color:red'><b>Total Error:</b> ").append(e.getMessage()).append("</p>");
        }

        // 2. Проверим /api/sessions/validate
        sb.append("<h3>2. /api/sessions/validate/{clientToken}</h3>");
        try {
            String url = "http://localhost:8097/api/sessions/validate/" + token;
            ResponseEntity<String> resp = rt.getForEntity(url, String.class);
            sb.append("<p><b>Response:</b> ").append(resp.getStatusCode()).append(" - ").append(resp.getBody()).append("</p>");
        } catch (Exception e) {
            sb.append("<p style='color:red'><b>Error:</b> ").append(e.getMessage()).append("</p>");
        }

        return sb.toString();
    }

    // ==================== БЛОК 2: РЕГИСТРАЦИЯ ПОЛЬЗОВАТЕЛЕЙ ====================

    @PostMapping("/clients/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> userData) {
        try {
            System.out.println("=== GATEWAY DEBUG ===");
            System.out.println("Получены данные: " + userData);

            String username = (String) userData.get("username");
            String password = (String) userData.get("password");
            String email = (String) userData.get("email");
            String firstname = (String) userData.get("firstname");

            if (firstname == null || firstname.trim().isEmpty()) {
                firstname = (String) userData.get("firstName");
                if (firstname == null || firstname.trim().isEmpty()) {
                    firstname = (String) userData.get("name");
                }
            }

            List<String> errors = new ArrayList<>();
            if (firstname == null || firstname.trim().isEmpty()) errors.add("Имя обязательно");
            if (username == null || username.trim().isEmpty()) errors.add("Имя пользователя обязательно");
            if (email == null || email.trim().isEmpty()) errors.add("Email обязателен");
            else if (!email.contains("@")) errors.add("Неверный формат email");
            if (password == null || password.trim().isEmpty()) errors.add("Пароль обязателен");
            else if (password.length() < 6) errors.add("Пароль должен быть не менее 6 символов");

            if (!errors.isEmpty()) {
                System.err.println("Ошибки валидации: " + errors);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errors", errors));
            }

            Map<String, Object> registrationData = new HashMap<>();
            registrationData.put("username", username);
            registrationData.put("password", password);
            registrationData.put("email", email);
            registrationData.put("firstname", firstname);

            if (userData.containsKey("age")) registrationData.put("age", userData.get("age"));
            if (userData.containsKey("city")) registrationData.put("city", userData.get("city"));
            if (userData.containsKey("magaz")) registrationData.put("magaz", userData.get("magaz"));

            registrationData.put("role", "client");
            registrationData.put("status", "active");

            System.out.println("Подготовлены данные для UserService: " + registrationData);
            System.out.println("Вызываем UserService через Feign...");

            Map<String, Object> response = clientService.registerUser(registrationData);
            System.out.println("✅ Ответ от UserService: " + response);

            if (response.containsKey("success") && Boolean.TRUE.equals(response.get("success"))) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (FeignException e) {
            System.err.println("❌ FeignException:");
            System.err.println("  Status: " + e.status());
            System.err.println("  Message: " + e.getMessage());
            System.err.println("  Content: " + e.contentUTF8());

            if (e.status() == 500) {
                String username = (String) userData.get("username");
                System.out.println("Проверяем, создан ли пользователь " + username + " в БД...");

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Пользователь создан, но была ошибка при формировании ответа");
                response.put("warning", "UserService вернул ошибку: " + e.contentUTF8());
                response.put("userData", userData);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }

            return ResponseEntity.status(e.status()).body(Map.of(
                    "success", false,
                    "error", "Ошибка сервиса регистрации",
                    "details", e.contentUTF8()
            ));

        } catch (Exception e) {
            System.err.println("❌ Общая ошибка в Gateway: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Внутренняя ошибка сервера: " + e.getMessage()
            ));
        }
    }

    // ==================== БЛОК 3: ВАЛИДАЦИЯ И ПРОВЕРКИ ====================

    @PostMapping("/clients/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> response = clientService.checkEmail(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "available", false,
                    "message", "Ошибка при проверке email",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/clients/check-username")
    public ResponseEntity<?> checkUsername(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> response = clientService.checkUsername(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "available", false,
                    "message", "Ошибка при проверке логина",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/clients/validate")
    public ResponseEntity<?> validateFields(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> response = clientService.validateFields(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Ошибка валидации",
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== БЛОК 4: ПУБЛИЧНЫЕ МЕТОДЫ КЛИЕНТОВ ====================

    @GetMapping("/clients")
    public ResponseEntity<?> getAllClients() {
        try {
            List<Map<String, Object>> clients = clientService.getAllClients();
            return ResponseEntity.ok(clients);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Сервис клиентов не найден или вернул 404");
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body("Ошибка при получении клиентов: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<?> getClient(@PathVariable int id) {
        try {
            Map<String, Object> client = clientService.getClient(id);
            return ResponseEntity.ok(client);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Клиент с id " + id + " не найден");
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body("Ошибка при получении клиента: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @GetMapping("/clients/{id}/profile")
    public ResponseEntity<?> getClientProfilePublic(@PathVariable int id) {
        try {
            Map<String, Object> client = clientService.getClient(id);
            Map<String, Object> publicProfile = new HashMap<>();

            if (client != null) {
                publicProfile.put("id", client.get("id"));
                publicProfile.put("username", client.get("username"));
                publicProfile.put("firstname", client.get("firstname"));
                publicProfile.put("email", client.get("email"));
                publicProfile.put("city", client.get("city"));
                publicProfile.put("age", client.get("age"));
                publicProfile.put("createdAt", client.get("createdAt"));
            }

            return ResponseEntity.ok(publicProfile);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка сервера"));
        }
    }

    // ==================== БЛОК 5: АДМИНИСТРАТИВНЫЕ МЕТОДЫ КЛИЕНТОВ ====================

    @PostMapping("/admin/clients")
    public ResponseEntity<?> createClientAdmin(@RequestBody Map<String, Object> clientData) {
        try {
            System.out.println("=== ADMIN: CREATE CLIENT ===");
            System.out.println("Получены данные: " + clientData);

            List<String> errors = new ArrayList<>();
            if (!clientData.containsKey("username") || clientData.get("username") == null ||
                    clientData.get("username").toString().trim().isEmpty()) errors.add("Имя пользователя обязательно");
            if (!clientData.containsKey("password") || clientData.get("password") == null ||
                    clientData.get("password").toString().trim().isEmpty()) errors.add("Пароль обязателен");
            if (!clientData.containsKey("email") || clientData.get("email") == null ||
                    clientData.get("email").toString().trim().isEmpty()) errors.add("Email обязателен");

            if (!errors.isEmpty()) return ResponseEntity.badRequest().body(Map.of("errors", errors));

            if (!clientData.containsKey("role")) clientData.put("role", "client");
            if (!clientData.containsKey("status")) clientData.put("status", "active");

            Map<String, Object> createdClient = clientService.createClient(clientData);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);

        } catch (FeignException.Conflict e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Пользователь с таким именем или email уже существует"));
        } catch (FeignException.BadRequest e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Неверные данные: " + e.contentUTF8()));
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка сервиса: " + e.contentUTF8()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Внутренняя ошибка сервера"));
        }
    }

    @GetMapping("/admin/clients")
    public ResponseEntity<?> getAllClientsAdmin(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            List<Map<String, Object>> clients = clientService.getAllClients();
            List<Map<String, Object>> filteredClients = clients.stream()
                    .filter(client -> {
                        boolean roleMatch = role == null || (client.get("role") != null && client.get("role").equals(role));
                        boolean statusMatch = status == null || (client.get("status") != null && client.get("status").equals(status));
                        boolean searchMatch = search == null || search.trim().isEmpty() ||
                                (client.get("username") != null && client.get("username").toString().toLowerCase().contains(search.toLowerCase())) ||
                                (client.get("email") != null && client.get("email").toString().toLowerCase().contains(search.toLowerCase()));
                        return roleMatch && statusMatch && searchMatch;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("total", filteredClients.size(), "clients", filteredClients));
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/clients/{id}")
    public ResponseEntity<?> getClientAdmin(@PathVariable int id) {
        try {
            Map<String, Object> client = clientService.getClient(id);
            return ResponseEntity.ok(client);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Клиент с id " + id + " не найден"));
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/clients/{id}")
    public ResponseEntity<?> updateClientAdmin(@PathVariable int id, @RequestBody Map<String, Object> updates) {
        try {
            if (updates == null || updates.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Нет данных для обновления"));
            }

            if (updates.containsKey("password")) {
                String password = updates.get("password").toString();
                if (password.length() < 6) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Пароль должен быть не менее 6 символов"));
                }
            }

            if (updates.containsKey("email")) {
                String email = updates.get("email").toString();
                if (!email.contains("@")) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Неверный формат email"));
                }
            }

            Map<String, Object> updatedClient = clientService.updateClient(id, updates);
            return ResponseEntity.ok(updatedClient);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Клиент с id " + id + " не найден"));
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка: " + e.getMessage()));
        }
    }

    @DeleteMapping("/admin/clients/{id}")
    public ResponseEntity<?> deleteClientAdmin(@PathVariable int id) {
        try {
            Map<String, Object> response = clientService.deleteClient(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Клиент успешно удален", "id", id));
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Клиент с id " + id + " не найден"));
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка: " + e.getMessage()));
        }
    }
    @PostMapping("/support/update-order-status")
    public ResponseEntity<?> updateOrderStatus(@RequestBody Map<String, Object> request) {
        try {
            Integer cartId = (Integer) request.get("cartId");
            String newStatus = (String) request.get("newStatus");
            String action = (String) request.get("action");

            log.info("🔄 Support: updating cart {} status to '{}' (action: {})",
                    cartId, newStatus, action);

            // 1. ПРОВЕРКА И НОРМАЛИЗАЦИЯ СТАТУСА
            if (newStatus != null) {
                // Заменяем длинные статусы на короткие
                if (newStatus.equals("transactioncompleted") || newStatus.equals("completed_refund")) {
                    newStatus = "tc"; // transaction completed
                } else if (newStatus.equals("tasamaiaOshibka!!!") || newStatus.equals("recollecting")) {
                    newStatus = "taoshibka"; // та самая ошибка
                }

                // Проверяем длину после нормализации
                if (newStatus.length() > 20) {
                    log.warn("⚠️ Status still too long ({} chars), truncating to 20 chars",
                            newStatus.length());
                    newStatus = newStatus.substring(0, Math.min(newStatus.length(), 20));
                }
                log.info("✅ Status normalized to: '{}'", newStatus);
            } else {
                log.error("❌ newStatus is null!");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "newStatus is required"
                ));
            }

            // 2. Получаем текущий статус заказа
            String currentStatus = null;
            try {
                String currentStatusSql = "SELECT status FROM carts WHERE id = ?";
                currentStatus = jdbcTemplate.queryForObject(currentStatusSql, String.class, cartId);
                log.info("📊 Current status of cart {}: '{}'", cartId, currentStatus);
            } catch (Exception e) {
                log.error("Error getting current status for cart {}: {}", cartId, e.getMessage());
                currentStatus = "unknown";
            }

            // 3. ОБНОВЛЯЕМ СТАТУС В carts (ИСПРАВЛЕНО: удален last_action)
            String updateSql = """
UPDATE carts 
SET status = ?
WHERE id = ?
""";

            log.info("📝 Executing SQL: {} with params: {}, {}",
                    updateSql.replace("?", "{}"), newStatus, cartId);

            try {
                int updatedRows = jdbcTemplate.update(updateSql, newStatus, cartId);
                log.info("✅ SQL executed. Updated rows: {}", updatedRows);

                if (updatedRows > 0) {
                    // 4. Проверяем новый статус
                    String verifySql = "SELECT status FROM carts WHERE id = ?";
                    String verifiedStatus = jdbcTemplate.queryForObject(verifySql, String.class, cartId);

                    log.info("✅ Cart {} status updated from '{}' to '{}' (verified: '{}')",
                            cartId, currentStatus, newStatus, verifiedStatus);

                    // 5. Обновляем nalichie в cart_items если это завершение возврата
                    if ("tc".equals(newStatus) || "completed".equals(newStatus)) {
                        try {
                            String updateItemsSql = """
                    UPDATE cart_items 
                    SET nalichie = 'refunded'
                    WHERE cart_id = ? AND nalichie = 'unknown'
                    """;
                            int updatedItems = jdbcTemplate.update(updateItemsSql, cartId);
                            log.info("✅ Updated {} cart_items for cart {} from 'unknown' to 'refunded'",
                                    updatedItems, cartId);
                        } catch (Exception e) {
                            log.warn("⚠️ Could not update cart_items for cart {}: {}", cartId, e.getMessage());
                        }
                    }

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("cartId", cartId);
                    response.put("oldStatus", currentStatus);
                    response.put("newStatus", newStatus);
                    response.put("verifiedStatus", verifiedStatus);
                    response.put("updatedRows", updatedRows);
                    response.put("message", "Статус заказа успешно обновлен");

                    return ResponseEntity.ok(response);
                } else {
                    log.warn("⚠️ No rows updated for cart {}. Cart might not exist.", cartId);
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "error", "Заказ не найден или статус не изменился",
                            "cartId", cartId
                    ));
                }
            } catch (Exception e) {
                log.error("❌ SQL ERROR updating cart status: {}", e.getMessage());
                log.error("❌ SQL State: {}", e instanceof org.springframework.dao.DataAccessException ?
                        ((org.springframework.jdbc.BadSqlGrammarException) e).getSQLException().getSQLState() : "Unknown");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "error", "SQL ошибка: " + e.getMessage()));
            }

        } catch (Exception e) {
            log.error("❌ Error in updateOrderStatus: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/support/unavailable-items/{clientId}")
    public ResponseEntity<?> getUnavailableItems(@PathVariable int clientId) {
        try {
            log.info("🔍 Support: getting unavailable items for client {}", clientId);

            // Получаем недопоставленные товары (nalichie = 'unknown')
            String sql = """
            SELECT 
                ci.id,
                ci.cart_id,
                ci.product_id,
                ci.quantity,
                ci.price,
                ci.nalichie,
                p.name as product_name,
                p.akticul as product_sku,
                c.created_date,
                c.status as cart_status
            FROM cart_items ci
            JOIN carts c ON ci.cart_id = c.id
            LEFT JOIN usersklad p ON ci.product_id = p.id
            WHERE c.client_id = ?
            AND ci.nalichie = 'unknown'
            AND c.status NOT IN ('cancelled', 'refunded')
            ORDER BY c.created_date DESC
        """;

            List<Map<String, Object>> items = jdbcTemplate.queryForList(sql, clientId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("clientId", clientId);
            response.put("items", items);
            response.put("total", items.size());
            response.put("totalAmount", items.stream()
                    .mapToDouble(item -> ((Number) item.get("price")).doubleValue() *
                            ((Number) item.get("quantity")).intValue())
                    .sum());
            response.put("message", items.size() > 0 ?
                    "Найдены недопоставленные товары" :
                    "Недопоставленных товаров не найдено");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting unavailable items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/support/refund-items")
    public ResponseEntity<?> refundItems(@RequestBody Map<String, Object> request) {
        try {
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");

            log.info("💰 Support: calculating refund for {} items", items != null ? items.size() : 0);

            // ТОЛЬКО РАСЧЕТ СУММЫ, без реального возврата
            double totalAmount = 0.0;
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Double price = ((Number) item.get("price")).doubleValue();
                    Integer quantity = ((Number) item.get("quantity")).intValue();
                    totalAmount += price * quantity;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalAmount", totalAmount);
            response.put("itemsCount", items != null ? items.size() : 0);
            response.put("message", String.format("%.2f рублей будет возвращена", totalAmount));

            log.info("✅ Refund calculated: {} rub for {} items", totalAmount, items != null ? items.size() : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error calculating refund: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/support/recollect-order")
    public ResponseEntity<?> recollectOrder(@RequestBody Map<String, Object> request) {
        try {
            List<Integer> cartIds = (List<Integer>) request.get("cartIds");

            log.info("🔄 Support: changing status to 'taoshibka' for carts: {}", cartIds);

            int updatedCarts = 0;
            List<Map<String, Object>> results = new ArrayList<>();

            for (Integer cartId : cartIds) {
                try {
                    // ИСПРАВЛЕННЫЙ SQL С КОРОТКИМ СТАТУСОМ (без last_action)
                    String updateSql = """
UPDATE carts 
SET status = 'taoshibka'
WHERE id = ?
""";

                    log.info("📝 Executing SQL for cart {}: {}", cartId, updateSql);

                    int rows = jdbcTemplate.update(updateSql, cartId);

                    Map<String, Object> result = new HashMap<>();
                    result.put("cartId", cartId);
                    result.put("updated", rows > 0);
                    result.put("rowsAffected", rows);
                    results.add(result);

                    if (rows > 0) {
                        updatedCarts++;
                        log.info("✅ Cart {} status changed to 'taoshibka'", cartId);

                        // Проверяем обновленный статус
                        try {
                            String verifySql = "SELECT status FROM carts WHERE id = ?";
                            String verifiedStatus = jdbcTemplate.queryForObject(verifySql, String.class, cartId);
                            log.info("✅ Verified status for cart {}: '{}'", cartId, verifiedStatus);
                        } catch (Exception e) {
                            log.warn("⚠️ Could not verify status for cart {}: {}", cartId, e.getMessage());
                        }
                    } else {
                        log.warn("⚠️ No rows updated for cart {}. Cart might not exist.", cartId);
                    }
                } catch (Exception e) {
                    log.error("❌ Error updating cart {}: {}", cartId, e.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("cartId", cartId);
                    errorResult.put("error", e.getMessage());
                    errorResult.put("updated", false);
                    results.add(errorResult);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("updatedCarts", updatedCarts);
            response.put("totalCarts", cartIds.size());
            response.put("results", results);
            response.put("message", "Заказ отправлен на повторную сборку. Статус изменен на 'ошибка сборки'");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error recollecting order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/debug/table-structure")
    public ResponseEntity<?> getTableStructure() {
        try {
            Map<String, Object> response = new HashMap<>();

            // Проверяем структуру таблицы carts
            try {
                String cartsStructure = jdbcTemplate.queryForObject(
                        "SELECT column_name, data_type, character_maximum_length " +
                                "FROM information_schema.columns " +
                                "WHERE table_name = 'carts' AND table_schema = 'public' " +
                                "ORDER BY ordinal_position",
                        (rs, rowNum) -> {
                            StringBuilder sb = new StringBuilder();
                            while (rs.next()) {
                                sb.append(rs.getString("column_name"))
                                        .append(": ").append(rs.getString("data_type"))
                                        .append("(").append(rs.getString("character_maximum_length")).append(")")
                                        .append("\n");
                            }
                            return sb.toString();
                        }
                );
                response.put("carts_structure", cartsStructure);
            } catch (Exception e) {
                response.put("carts_structure_error", e.getMessage());
            }

            // Проверяем текущие статусы
            try {
                String currentStatuses = jdbcTemplate.queryForObject(
                        "SELECT id, status, LENGTH(status) as status_length FROM carts LIMIT 10",
                        (rs, rowNum) -> {
                            StringBuilder sb = new StringBuilder();
                            while (rs.next()) {
                                sb.append("Cart ").append(rs.getInt("id"))
                                        .append(": '").append(rs.getString("status"))
                                        .append("' (length: ").append(rs.getInt("status_length")).append(")")
                                        .append("\n");
                            }
                            return sb.toString();
                        }
                );
                response.put("current_statuses", currentStatuses);
            } catch (Exception e) {
                response.put("statuses_error", e.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/support/refund-history/{clientId}")
    public ResponseEntity<?> getRefundHistory(@PathVariable int clientId) {
        try {
            log.info("📊 Support: getting refund history for client {}", clientId);

            // Пытаемся получить историю возвратов
            List<Map<String, Object>> history = new ArrayList<>();
            try {
                String sql = """
                SELECT 
                    id as refund_id,
                    total_amount,
                    items_count,
                    refund_type,
                    status,
                    created_at
                FROM refund_history 
                WHERE client_id = ?
                ORDER BY created_at DESC
                LIMIT 50
            """;
                history = jdbcTemplate.queryForList(sql, clientId);
            } catch (Exception e) {
                log.warn("Refund history table might not exist: {}", e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("clientId", clientId);
            response.put("history", history);
            response.put("totalRefunds", history.size());
            response.put("totalRefunded", history.stream()
                    .mapToDouble(item -> ((Number) item.get("total_amount")).doubleValue())
                    .sum());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting refund history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    // ==================== БЛОК 6: ТОВАРЫ (PRODUCTS) ====================

    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        try {
            log.info("🛒 Получение всех товаров через Gateway");
            List<Map<String, Object>> products = productServiceClient.getAllProducts();
            log.info("✅ Получено {} товаров", products.size());
            return ResponseEntity.ok(products);
        } catch (FeignException.NotFound e) {
            log.error("❌ Сервис товаров не найден: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Сервис товаров не найден", "message", e.contentUTF8()));
        } catch (FeignException e) {
            log.error("❌ Ошибка при получении товаров: {}", e.getMessage());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка при получении товаров", "message", e.contentUTF8()));
        } catch (Exception e) {
            log.error("❌ Внутренняя ошибка сервера при получении товаров: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Внутренняя ошибка сервера", "message", e.getMessage()));
        }
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable int id) {
        try {
            log.info("🔍 Получение товара с ID: {} через Gateway", id);
            Map<String, Object> product = productServiceClient.getProduct(id);

            if (product == null || product.isEmpty()) {
                log.warn("⚠️ Товар с ID {} не найден", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Товар не найден", "message", "Товар с id " + id + " не найден"));
            }

            log.info("✅ Найден товар: {} (ID: {})", product.get("name"), product.get("id"));
            return ResponseEntity.ok(product);
        } catch (FeignException.NotFound e) {
            log.warn("⚠️ Товар с ID {} не найден", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Товар не найден", "message", "Товар с id " + id + " не найден"));
        } catch (FeignException e) {
            log.error("❌ Ошибка при получении товара: {}", e.getMessage());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка при получении товара", "message", e.contentUTF8()));
        } catch (Exception e) {
            log.error("❌ Внутренняя ошибка сервера: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Внутренняя ошибка сервера", "message", e.getMessage()));
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> productData) {
        try {
            log.info("➕ Создание нового товара через Gateway");
            List<String> errors = new ArrayList<>();

            if (!productData.containsKey("name") || productData.get("name") == null ||
                    productData.get("name").toString().trim().isEmpty()) errors.add("Название товара обязательно");
            if (!productData.containsKey("price") || productData.get("price") == null) errors.add("Цена обязательна");
            else {
                try {
                    double price = Double.parseDouble(productData.get("price").toString());
                    if (price <= 0) errors.add("Цена должна быть положительной");
                } catch (NumberFormatException e) { errors.add("Цена должна быть числом"); }
            }
            if (!productData.containsKey("category") || productData.get("category") == null ||
                    productData.get("category").toString().trim().isEmpty()) errors.add("Категория обязательна");
            if (!productData.containsKey("count")) productData.put("count", 0);

            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ошибка валидации", "message", String.join(", ", errors)));
            }

            Map<String, Object> createdProduct = productServiceClient.createProduct(productData);
            log.info("✅ Товар создан: {} (ID: {})", createdProduct.get("name"), createdProduct.get("id"));
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        } catch (FeignException.BadRequest e) {
            log.error("❌ Неверные данные товара: {}", e.contentUTF8());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверные данные товара", "message", e.contentUTF8()));
        } catch (FeignException e) {
            log.error("❌ Ошибка при создании товара: {}", e.getMessage());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка при создании товара", "message", e.contentUTF8()));
        } catch (Exception e) {
            log.error("❌ Внутренняя ошибка сервера: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Внутренняя ошибка сервера", "message", e.getMessage()));
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable int id, @RequestBody Map<String, Object> updates) {
        try {
            log.info("✏️ Обновление товара с ID: {} через Gateway", id);
            List<String> errors = new ArrayList<>();

            if (updates.containsKey("name") && (updates.get("name") == null || updates.get("name").toString().trim().isEmpty())) {
                errors.add("Название товара не может быть пустым");
            }
            if (updates.containsKey("price")) {
                try {
                    double price = Double.parseDouble(updates.get("price").toString());
                    if (price <= 0) errors.add("Цена должна быть положительной");
                } catch (NumberFormatException e) { errors.add("Цена должна быть числом"); }
            }
            if (updates.containsKey("count")) {
                try {
                    int count = Integer.parseInt(updates.get("count").toString());
                    if (count < 0) errors.add("Количество не может быть отрицательным");
                } catch (NumberFormatException e) { errors.add("Количество должно быть целым числом"); }
            }

            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ошибка валидации", "message", String.join(", ", errors)));
            }

            Map<String, Object> updatedProduct = productServiceClient.updateProduct(id, updates);
            log.info("✅ Товар обновлен: {} (ID: {})", updatedProduct.get("name"), updatedProduct.get("id"));
            return ResponseEntity.ok(updatedProduct);
        } catch (FeignException.NotFound e) {
            log.warn("⚠️ Товар с ID {} не найден для обновления", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Товар не найден", "message", "Товар с id " + id + " не найден"));
        } catch (FeignException.BadRequest e) {
            log.error("❌ Неверные данные для обновления: {}", e.contentUTF8());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверные данные", "message", e.contentUTF8()));
        } catch (FeignException e) {
            log.error("❌ Ошибка при обновлении товара: {}", e.getMessage());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка при обновлении товара", "message", e.contentUTF8()));
        } catch (Exception e) {
            log.error("❌ Внутренняя ошибка сервера: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Внутренняя ошибка сервера", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable int id) {
        try {
            log.info("🗑️ Удаление товара с ID: {} через Gateway", id);
            try {
                productServiceClient.getProduct(id);
            } catch (FeignException.NotFound e) {
                log.warn("⚠️ Товар с ID {} не найден для удаления", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Товар не найден", "message", "Товар с id " + id + " не найден"));
            }

            ResponseEntity<Void> response = productServiceClient.deleteProduct(id);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Товар с ID {} успешно удален", id);
                return ResponseEntity.ok().body(Map.of("success", true, "message", "Товар успешно удален", "id", id));
            } else {
                log.error("❌ Ошибка при удалении товара: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Map.of("error", "Ошибка при удалении товара", "message", "HTTP статус: " + response.getStatusCode()));
            }
        } catch (FeignException e) {
            log.error("❌ Ошибка при удалении товара: {}", e.getMessage());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка при удалении товара", "message", e.contentUTF8()));
        } catch (Exception e) {
            log.error("❌ Внутренняя ошибка сервера: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Внутренняя ошибка сервера", "message", e.getMessage()));
        }
    }

    @GetMapping("/products/category/{category}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable String category) {
        try {
            log.info("🔍 Поиск товаров по категории: {} через Gateway", category);
            String url = "http://localhost:8082/api/products/category/" + category;
            ResponseEntity<?> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> products = (List<?>) response.getBody();
                log.info("✅ Найдено {} товаров в категории {}", products.size(), category);
                return ResponseEntity.ok(products);
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при поиске товаров по категории: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при поиске товаров", "message", e.getMessage()));
        }
    }

    @GetMapping("/products/search")
    public ResponseEntity<?> searchProducts(@RequestParam String query) {
        try {
            log.info("🔍 Поиск товаров по запросу: {} через Gateway", query);
            String url = "http://localhost:8082/api/products/search?query=" + query;
            ResponseEntity<?> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> products = (List<?>) response.getBody();
                log.info("✅ Найдено {} товаров по запросу '{}'", products.size(), query);
                return ResponseEntity.ok(products);
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при поиске товаров: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при поиске товаров", "message", e.getMessage()));
        }
    }

    @GetMapping("/products/stats")
    public ResponseEntity<?> getProductsStats() {
        try {
            log.info("📊 Получение статистики товаров через Gateway");
            String url = "http://localhost:8082/api/products/stats";
            ResponseEntity<?> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при получении статистики: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при получении статистики", "message", e.getMessage()));
        }
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<?> getLowStockProducts() {
        try {
            log.info("⚠️ Получение товаров с низким запасом через Gateway");
            String url = "http://localhost:8082/api/products/low-stock";
            ResponseEntity<?> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при получении товаров с низким запасом: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при получении данных", "message", e.getMessage()));
        }
    }

    // ==================== БЛОК 7: ЗАКАЗЫ (ORDERS) - из первого файла ====================
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("=== СОЗДАНИЕ ЗАКАЗА ===");
            log.info("Получен заказ: {}", orderRequest);
            log.info("Authorization header: {}", authHeader);

            Integer userId = extractUserIdFromToken(authHeader);
            log.info("✅ Извлечен userId: {}", userId);

            List<Map<String, Object>> items = (List<Map<String, Object>>) orderRequest.get("items");
            Number totalAmountNumber = (Number) orderRequest.get("totalAmount");
            Double totalAmount = totalAmountNumber != null ? totalAmountNumber.doubleValue() : null;

            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Корзина пуста", "success", false));
            }

            Map<String, Object> cartResponse;
            try {
                cartResponse = cartService.createCart(userId);
                log.info("Создана корзина для пользователя {}: {}", userId, cartResponse);
            } catch (FeignException e) {
                log.error("Ошибка при создании корзины: {}", e.contentUTF8());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Не удалось создать корзину", "details", e.contentUTF8()));
            }

            Integer cartId = (Integer) cartResponse.get("id");
            Double calculatedTotal = 0.0;
            List<Map<String, Object>> processedItems = new ArrayList<>();

            for (Map<String, Object> item : items) {
                try {
                    Number productIdNumber = (Number) item.get("productId");
                    Number quantityNumber = (Number) item.get("quantity");

                    if (productIdNumber == null || quantityNumber == null) {
                        log.warn("Пропускаем товар с отсутствующими данными: {}", item);
                        continue;
                    }

                    Integer productId = productIdNumber.intValue();
                    Integer quantity = quantityNumber.intValue();

                    Map<String, Object> product;
                    try {
                        product = productServiceClient.getProductById(productId);
                    } catch (FeignException e) {
                        log.error("Ошибка получения товара ID {}: {}", productId, e.contentUTF8());
                        continue;
                    }

                    if (product == null || product.isEmpty()) {
                        log.warn("Товар ID {} не найден", productId);
                        continue;
                    }

                    Double price = 0.0;
                    Object priceObj = product.get("price");
                    if (priceObj != null) {
                        if (priceObj instanceof Number) price = ((Number) priceObj).doubleValue();
                        else if (priceObj instanceof String) {
                            try { price = Double.parseDouble((String) priceObj); }
                            catch (NumberFormatException ex) { log.warn("Некорректный формат цены для товара ID {}: {}", productId, priceObj); }
                        }
                    }

                    Integer originalCount = 0;
                    Object countObj = product.get("count");
                    if (countObj instanceof Integer) originalCount = (Integer) countObj;
                    else if (countObj instanceof Number) originalCount = ((Number) countObj).intValue();

                    Map<String, Object> addResponse = cartService.addToCart(cartId, productId, quantity, price);
                    log.info("Добавлен товар в корзину: {}", addResponse);

                    calculatedTotal += price * quantity;

                    Map<String, Object> processedItem = new HashMap<>(item);
                    processedItem.put("price", price);
                    processedItem.put("name", product.get("name"));
                    processedItem.put("productName", product.get("name"));
                    processedItem.put("originalCount", originalCount);
                    processedItems.add(processedItem);

                } catch (Exception e) {
                    log.error("Ошибка при обработке товара: {}", e.getMessage(), e);
                }
            }

            if (processedItems.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ни один товар не удалось добавить в корзину", "success", false));
            }

            Double finalAmount = totalAmount != null ? totalAmount : calculatedTotal;

            Map<String, Object> checkoutResponse;
            try {
                log.info("Оформление заказа из корзины: {}", cartId);
                checkoutResponse = cartService.checkoutCart(cartId);
                log.info("Оформлен заказ: {}", checkoutResponse);

            } catch (FeignException e) {
                log.error("Ошибка при оформлении заказа: {}", e.contentUTF8());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Ошибка при оформлении заказа");
                errorResponse.put("message", e.contentUTF8());
                errorResponse.put("cartId", cartId);
                errorResponse.put("userId", userId);
                errorResponse.put("totalAmount", finalAmount);
                errorResponse.put("timestamp", new Date());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            // ========= ВАЖНОЕ ИСПРАВЛЕНИЕ =========
            // Принудительно проверяем и устанавливаем статус корзины в "processing"
            log.info("🔍 Проверяем статус корзины {} после checkout", cartId);
            try {
                // 1. Проверяем текущий статус
                String currentStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM carts WHERE id = ?",
                        String.class, cartId);
                log.info("📊 Текущий статус корзины {}: {}", cartId, currentStatus);

                // 2. Если статус не "processing", исправляем
                if (!"processing".equals(currentStatus)) {
                    log.info("🔄 Исправляем статус корзины {} с '{}' на 'processing'", cartId, currentStatus);
                    String updateSql = "UPDATE carts SET status = 'processing' WHERE id = ?";
                    int updatedRows = jdbcTemplate.update(updateSql, cartId);
                    log.info("✅ Исправлено строк: {}", updatedRows);

                    // 3. Проверяем исправление
                    String fixedStatus = jdbcTemplate.queryForObject(
                            "SELECT status FROM carts WHERE id = ?",
                            String.class, cartId);
                    log.info("✅ Исправленный статус корзины {}: {}", cartId, fixedStatus);
                }
            } catch (Exception e) {
                log.warn("⚠️ Не удалось проверить/исправить статус корзины: {}", e.getMessage());
            }
            // ========= КОНЕЦ ИСПРАВЛЕНИЯ =========

            log.info("=== ОБНОВЛЕНИЕ КОЛИЧЕСТВА ТОВАРОВ ===");
            boolean stockUpdated = true;
            List<Map<String, Object>> stockUpdateResults = new ArrayList<>();

            for (Map<String, Object> processedItem : processedItems) {
                try {
                    Integer productId = (Integer) processedItem.get("productId");
                    Integer quantity = (Integer) processedItem.get("quantity");
                    Integer originalCount = (Integer) processedItem.get("originalCount");

                    if (productId == null || quantity == null || quantity <= 0) continue;

                    log.info("Обновление товара ID {}: уменьшаем на {} шт. (было {} шт.)",
                            productId, quantity, originalCount);

                    Integer newCount = originalCount - quantity;
                    if (newCount < 0) {
                        log.warn("⚠️ ВНИМАНИЕ: Отрицательное количество для товара ID {}: {} - {} = {}",
                                productId, originalCount, quantity, newCount);
                        newCount = 0;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("count", newCount);

                    Map<String, Object> updateResult = new HashMap<>();
                    updateResult.put("productId", productId);
                    updateResult.put("productName", processedItem.get("name"));
                    updateResult.put("orderedQuantity", quantity);
                    updateResult.put("originalCount", originalCount);
                    updateResult.put("newCount", newCount);
                    updateResult.put("updated", false);

                    try {
                        Map<String, Object> updatedProduct = productServiceClient.updateProduct(productId, updates);
                        Object updatedCount = updatedProduct.get("count");
                        if (updatedCount != null) {
                            Integer actualNewCount = 0;
                            if (updatedCount instanceof Integer) actualNewCount = (Integer) updatedCount;
                            else if (updatedCount instanceof Number) actualNewCount = ((Number) updatedCount).intValue();

                            updateResult.put("actualNewCount", actualNewCount);
                            updateResult.put("updated", true);
                            log.info("✅ Товар ID {} обновлен: было {} шт., стало {} шт. (уменьшено на {} шт.)",
                                    productId, originalCount, actualNewCount, quantity);
                        } else {
                            log.warn("⚠️ Товар ID {} обновлен, но поле 'count' отсутствует в ответе", productId);
                            updateResult.put("warning", "count field missing in response");
                            stockUpdated = false;
                        }
                    } catch (FeignException e) {
                        log.error("❌ Feign ошибка обновления товара ID {}: {}", productId, e.contentUTF8());
                        updateResult.put("error", e.contentUTF8());
                        updateResult.put("updated", false);
                        stockUpdated = false;
                    } catch (Exception e) {
                        log.error("❌ Общая ошибка обновления товара ID {}: {}", productId, e.getMessage());
                        updateResult.put("error", e.getMessage());
                        updateResult.put("updated", false);
                        stockUpdated = false;
                    }

                    stockUpdateResults.add(updateResult);
                } catch (Exception e) {
                    log.error("❌ Критическая ошибка при обновлении товара: {}", e.getMessage());
                    stockUpdated = false;
                }
            }

            log.info("Обновление количества товаров завершено: {}",
                    stockUpdated ? "✅ ВСЕ ТОВАРЫ ОБНОВЛЕНЫ" : "⚠️ ЕСТЬ ОШИБКИ ПРИ ОБНОВЛЕНИИ");

            Map<String, Object> response = new HashMap<>();
            Object checkoutId = checkoutResponse.get("id");
            if (checkoutId != null) response.put("id", checkoutId.toString());
            else response.put("id", "ORD-" + System.currentTimeMillis());

            // === ВАЖНОЕ ИЗМЕНЕНИЕ ===
            // 1. Сохраняем реальный статус из базы данных
            String actualStatus = checkoutResponse.get("status") != null ?
                    checkoutResponse.get("status").toString().toLowerCase() : "processing";

            // 2. Определяем статус для CollectorApp
            String collectorStatus;
            if ("completed".equals(actualStatus) || "paid".equals(actualStatus) || "delivered".equals(actualStatus)) {
                // Если заказ уже завершен, то сборщику он не нужен
                collectorStatus = "completed";
            } else {
                // Для всех остальных статусов - processing
                collectorStatus = "processing";
            }

            // 3. Записываем оба статуса в ответ
            response.put("status", actualStatus); // Реальный статус из БД
            response.put("collectorStatus", collectorStatus); // Статус для CollectorApp
            response.put("displayStatus", collectorStatus); // Дублируем для совместимости
            response.put("message", "Заказ успешно создан");
            response.put("totalAmount", finalAmount);
            response.put("cartId", cartId);
            response.put("userId", userId);
            response.put("itemsCount", processedItems.size());
            response.put("items", processedItems);
            response.put("timestamp", new Date());
            response.put("success", true);
            response.put("stockUpdated", stockUpdated);
            response.put("stockUpdateResults", stockUpdateResults);
            response.put("stockUpdateTimestamp", new Date());

            // ДОБАВЛЯЕМ ПРОВЕРЕННЫЙ СТАТУС КОРЗИНЫ
            try {
                String verifiedCartStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM carts WHERE id = ?",
                        String.class, cartId);
                response.put("cartStatus", verifiedCartStatus);
                log.info("✅ Финальный статус корзины {} в БД: '{}'", cartId, verifiedCartStatus);
            } catch (Exception e) {
                log.warn("⚠️ Не удалось получить финальный статус корзины: {}", e.getMessage());
                response.put("cartStatus", "unknown");
            }

            long successfullyUpdated = stockUpdateResults.stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("updated")))
                    .count();

            log.info("✅ Заказ создан: {} для пользователя {}", response.get("id"), userId);
            log.info("📦 Обновлено товаров: {}/{}", successfullyUpdated, processedItems.size());
            log.info("🏷️ Статусы - Фактический: {}, Для сборщика: {}", actualStatus, collectorStatus);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Необработанная ошибка при создании заказа: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при создании заказа", "message", e.getMessage(), "success", false, "timestamp", new Date()));
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        try {
            log.info("Получение всех заказов");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", "Функционал в разработке", "message", "Эндпоинт получения заказов пока не реализован", "success", false));
        } catch (Exception e) {
            log.error("Ошибка при получении заказов: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка сервера", "success", false));
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable String orderId) {
        try {
            log.info("Получение заказа с ID: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", "Функционал в разработке", "message", "Эндпоинт получения заказа по ID пока не реализован", "orderId", orderId, "success", false));
        } catch (Exception e) {
            log.error("Ошибка при получении заказа: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка сервера", "success", false));
        }
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId) {
        try {
            log.info("Отмена заказа с ID: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", "Функционал в разработке", "message", "Эндпоинт отмены заказа пока не реализован", "orderId", orderId, "success", false));
        } catch (Exception e) {
            log.error("Ошибка при отмене заказа: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Не удалось отменить заказ", "success", false));
        }
    }

    // ==================== БЛОК 8: КОРЗИНЫ (CARTS) - расширенные методы из первого файла ====================

    @PostMapping("/cart/create")
    public ResponseEntity<?> createCartForCurrentUser() {
        try {
            int clientId = 1; // Для тестирования
            log.info("Создание корзины для клиента: {}", clientId);
            Map<String, Object> cartResponse = cartService.createCart(clientId);
            return ResponseEntity.status(HttpStatus.CREATED).body(cartResponse);
        } catch (FeignException e) {
            log.error("Ошибка Feign при создании корзины: {}", e.contentUTF8());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка сервиса корзины", "details", e.contentUTF8()));
        } catch (Exception e) {
            log.error("Ошибка при создании корзины: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при создании корзины", "success", false));
        }
    }

    @PostMapping("/cart/add")
    public ResponseEntity<?> addItemToCart(@RequestBody Map<String, Object> request) {
        try {
            Integer cartId = (Integer) request.get("cartId");
            Integer productId = (Integer) request.get("productId");
            Integer quantity = (Integer) request.get("quantity");
            Double price = (Double) request.get("price");

            if (cartId == null || productId == null || quantity == null || price == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Не все обязательные поля указаны", "success", false));
            }

            log.info("Добавление товара в корзину: cartId={}, productId={}", cartId, productId);
            Map<String, Object> response = cartService.addToCart(cartId, productId, quantity, price);
            return ResponseEntity.ok(response);
        } catch (FeignException e) {
            log.error("Ошибка Feign при добавлении в корзину: {}", e.contentUTF8());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка сервиса корзины", "details", e.contentUTF8()));
        } catch (Exception e) {
            log.error("Ошибка при добавлении в корзину: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при добавлении товара", "success", false));
        }
    }

    @GetMapping("/cart/{cartId}/items")
    public ResponseEntity<?> getCartItems(@PathVariable Integer cartId) {
        try {
            log.info("Получение товаров корзины: {}", cartId);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", "Функционал в разработке", "message", "Эндпоинт получения товаров корзины пока не реализован", "cartId", cartId, "success", false));
        } catch (Exception e) {
            log.error("Ошибка при получении товаров корзины: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при получении товаров", "success", false));
        }
    }

    @PostMapping("/cart/{cartId}/checkout")
    public ResponseEntity<?> checkoutCart(@PathVariable Integer cartId) {
        try {
            log.info("Оформление заказа из корзины: {}", cartId);
            Map<String, Object> response = cartService.checkoutCart(cartId);
            return ResponseEntity.ok(response);
        } catch (FeignException e) {
            log.error("Ошибка сервиса корзины при оформлении: {}", e.contentUTF8());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка сервиса корзины", "details", e.contentUTF8()));
        } catch (Exception e) {
            log.error("Ошибка при оформлении заказа: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при оформлении заказа", "success", false));
        }
    }

    @PostMapping("/cart/{cartId}/complete-order")
    public ResponseEntity<?> completeOrder(@PathVariable int cartId) {
        try {
            log.info("✅ Завершение заказа для корзины {}", cartId);
            // Реализация завершения заказа
            return ResponseEntity.ok(Map.of("success", true, "message", "Заказ успешно завершен", "cartId", cartId));
        } catch (Exception e) {
            log.error("❌ Ошибка при завершении заказа: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Ошибка при завершении заказа", "message", e.getMessage()));
        }
    }

    @GetMapping("/cart/client/{clientId}/full")
    public ResponseEntity<?> getClientCartsFull(@PathVariable int clientId) {
        try {
            log.info("🛍️ Gateway: Получение корзин и заказов клиента {}", clientId);
            List<Map<String, Object>> carts = cartService.getClientCarts(clientId);
            List<Map<String, Object>> orders = new ArrayList<>();

            try {
                orders = cartService.getClientOrders(clientId);
                log.info("✅ Получено {} заказов для клиента {}", orders.size(), clientId);
            } catch (Exception e) {
                log.warn("⚠️ Эндпоинт заказов недоступен: {}", e.getMessage());
            }

            List<Map<String, Object>> result = new ArrayList<>();

            for (Map<String, Object> cart : carts) {
                Integer cartId = (Integer) cart.get("id");
                Map<String, Object> fullCart = new HashMap<>(cart);
                String cartStatus = "active";

                for (Map<String, Object> order : orders) {
                    Object orderCartId = order.get("cartId");
                    if (orderCartId != null && orderCartId.toString().equals(cartId.toString())) {
                        String orderStatus = (String) order.get("status");
                        if (orderStatus != null && !orderStatus.isEmpty()) cartStatus = orderStatus.toLowerCase();
                        fullCart.put("orderId", order.get("id"));
                        fullCart.put("orderData", order);
                        break;
                    }
                }

                fullCart.put("status", cartStatus);
                fullCart.put("statusSource", orders.isEmpty() ? "cart" : "order");

                List<Map<String, Object>> cartItems = new ArrayList<>();
                try {
                    cartItems = cartService.getCartItems(cartId);
                } catch (Exception e) {
                    log.warn("Не удалось получить товары корзины {}: {}", cartId, e.getMessage());
                }

                List<Map<String, Object>> enrichedItems = new ArrayList<>();
                double cartTotal = 0.0;

                for (Map<String, Object> item : cartItems) {
                    Integer productId = (Integer) item.get("productId");
                    Integer quantity = (Integer) item.get("quantity");
                    Double price = item.get("price") != null ? ((Number) item.get("price")).doubleValue() : 0.0;

                    Map<String, Object> productInfo = new HashMap<>();
                    try {
                        productInfo = productServiceClient.getProduct(productId);
                    } catch (Exception e) {
                        productInfo.put("name", "Товар ID: " + productId);
                        productInfo.put("category", "Неизвестно");
                    }

                    Map<String, Object> enrichedItem = new HashMap<>();
                    enrichedItem.put("id", item.get("id"));
                    enrichedItem.put("productId", productId);
                    enrichedItem.put("productName", productInfo.get("name"));
                    enrichedItem.put("category", productInfo.get("category"));
                    enrichedItem.put("quantity", quantity);
                    enrichedItem.put("price", price);
                    enrichedItem.put("itemTotal", quantity * price);
                    enrichedItem.put("articul", productInfo.get("akticul"));

                    enrichedItems.add(enrichedItem);
                    cartTotal += quantity * price;
                }

                fullCart.put("items", enrichedItems);
                fullCart.put("totalAmount", cartTotal);
                fullCart.put("itemsCount", enrichedItems.size());

                result.add(fullCart);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "clientId", clientId,
                    "totalCarts", result.size(),
                    "ordersCount", orders.size(),
                    "carts", result,
                    "statusSource", orders.isEmpty() ? "cart" : "order"
            ));

        } catch (Exception e) {
            log.error("❌ Ошибка при получении информации: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Ошибка при получении данных", "message", e.getMessage()));
        }
    }

    @GetMapping("/cart/client/{clientId}")
    public ResponseEntity<?> getClientCarts(@PathVariable int clientId) {
        try {
            log.info("📦 Gateway: Получение корзин клиента {}", clientId);
            List<Map<String, Object>> carts = cartService.getClientCarts(clientId);
            log.info("✅ Получено {} корзин для клиента {}", carts.size(), clientId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "clientId", clientId,
                    "totalCarts", carts.size(),
                    "carts", carts
            ));

        } catch (FeignException.NotFound e) {
            log.warn("⚠️ Корзины для клиента {} не найдены", clientId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Корзины не найдены", "clientId", clientId, "message", "Клиент не имеет корзин"));
        } catch (FeignException e) {
            log.error("❌ Ошибка Feign при получении корзин: status={}, message={}", e.status(), e.contentUTF8());
            return ResponseEntity.status(e.status())
                    .body(Map.of("success", false, "error", "Ошибка сервиса корзины", "details", e.contentUTF8(), "statusCode", e.status()));
        } catch (Exception e) {
            log.error("❌ Внутренняя ошибка Gateway: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Внутренняя ошибка сервера", "message", e.getMessage()));
        }
    }

    @GetMapping("/cart/my-orders")
    public ResponseEntity<?> getMyOrders(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("Получение заказов текущего пользователя");
            Integer clientId = extractUserIdFromToken(authHeader);
            if (clientId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Требуется авторизация"));
            }

            log.info("Получение заказов для clientId: {}", clientId);
            List<Map<String, Object>> orders = cartService.getClientCarts(clientId);

            List<Map<String, Object>> completedOrders = orders.stream()
                    .filter(order ->
                            "processing".equals(order.get("status")) ||
                                    "processing".equals(order.get("status")) ||
                                    "paid".equals(order.get("status")) ||
                                    "PAID".equals(order.get("status")) ||
                                    "checked_out".equals(order.get("status"))
                    )
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "clientId", clientId,
                    "totalOrders", completedOrders.size(),
                    "orders", completedOrders
            ));

        } catch (FeignException e) {
            log.error("Ошибка при получении заказов: {}", e.contentUTF8());
            return ResponseEntity.status(e.status()).body(Map.of("error", "Ошибка сервиса корзины"));
        } catch (Exception e) {
            log.error("Внутренняя ошибка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Внутренняя ошибка сервера"));
        }
    }

    @DeleteMapping("/cart/{cartId}/items/{itemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Integer cartId, @PathVariable Integer itemId) {
        try {
            log.info("Удаление товара из корзины: cartId={}, itemId={}", cartId, itemId);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", "Функционал в разработке", "message", "Эндпоинт удаления товара из корзины пока не реализован", "cartId", cartId, "itemId", itemId, "success", false));
        } catch (Exception e) {
            log.error("Ошибка при удалении товара: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при удалении товара", "success", false));
        }
    }

   

    // ==================== БЛОК 13: КОМПЛЕКСНЫЕ ОПЕРАЦИИ ====================

    @GetMapping("/clients/{clientId}/with-carts")
    public Map<String, Object> getClientWithCarts(@PathVariable int clientId) {
        Map<String, Object> client = clientService.getClient(clientId);
        List<Map<String, Object>> carts = cartService.getClientCarts(clientId);

        return Map.of(
                "client", client,
                "carts", carts
        );
    }

    @GetMapping("/clients/{clientId}/deliveries-info")
    public Map<String, Object> getClientWithDeliveries(@PathVariable Integer clientId) {
        Object client = clientService.getClient(clientId);

        // Безопасное приведение типов
        List<?> deliveries = (List<?>) deliveryService.getClientDeliveries(clientId);
        List<?> carts = (List<?>) cartService.getClientCarts(clientId);

        return Map.of(
                "client", client,
                "deliveries", deliveries != null ? deliveries : Collections.emptyList(),
                "carts", carts != null ? carts : Collections.emptyList()
        );
    }

    @PostMapping("/clients/{clientId}/complete-order")
    public Map<String, Object> createCompleteOrder(
            @PathVariable Integer clientId,
            @RequestBody Map<String, Object> orderRequest) {

        Object cart = cartService.createCart(clientId);
        List<Map<String, Object>> items = (List<Map<String, Object>>) orderRequest.get("items");

        if (items != null) {
            for (Map<String, Object> item : items) {
                cartService.addToCart(
                        (Integer) ((Map<String, Object>) cart).get("id"),
                        (Integer) item.get("productId"),
                        (Integer) item.get("quantity"),
                        (Double) item.get("price")
                );
            }
        }

        Map<String, Object> deliveryRequest = Map.of(
                "orderId", orderRequest.get("orderId"),
                "clientId", clientId,
                "deliveryAddress", orderRequest.get("deliveryAddress"),
                "deliveryPhone", orderRequest.get("deliveryPhone")
        );

        Object delivery = deliveryService.createDelivery(deliveryRequest);

        return Map.of(
                "clientId", clientId,
                "cart", cart,
                "delivery", delivery,
                "message", "Complete order created successfully"
        );
    }

    // ==================== БЛОК 14: БАЗА ДАННЫХ И HEALTH CHECKS ====================

    @GetMapping("/database/test-connection")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        log.info("Testing PostgreSQL connection...");
        Map<String, Object> response = new HashMap<>();

        try {
            String result = jdbcTemplate.queryForObject("SELECT 'PostgreSQL Connected Successfully'", String.class);
            String dbName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
            String dbVersion = jdbcTemplate.queryForObject("SELECT version()", String.class);

            log.info("Database connected: {} {}", dbName, dbVersion);
            response.put("connected", true);
            response.put("message", result);
            response.put("databaseName", dbName);
            response.put("databaseVersion", dbVersion);
            response.put("port", 8082);
            response.put("service", "sklad-service");
            response.put("status", "UP");
        } catch (Exception e) {
            log.error("Database connection failed: {}", e.getMessage());
            response.put("connected", false);
            response.put("message", "Failed to connect to PostgreSQL");
            response.put("error", e.getMessage());
            response.put("port", 8082);
            response.put("service", "sklad-service");
            response.put("status", "DOWN");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/database/stats")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        log.info("Getting database statistics...");
        Map<String, Object> response = new HashMap<>();

        try {
            String dbName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
            String dbSize = jdbcTemplate.queryForObject("SELECT pg_size_pretty(pg_database_size(current_database()))", String.class);
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'", Integer.class);
            Integer productsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usersklad", Integer.class);

            response.put("status", "connected");
            response.put("databaseName", dbName);
            response.put("databaseSize", dbSize);
            response.put("tableCount", tableCount != null ? tableCount : 0);
            response.put("productsCount", productsCount != null ? productsCount : 0);
            response.put("port", 8082);
        } catch (Exception e) {
            log.error("Failed to get database stats: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("port", 8082);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "api-stub",
                "timestamp", Instant.now().toString(),
                "version", "1.0.0"
        ));
    }

    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> actuatorHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "components", Map.of(
                        "db", Map.of("status", "UP", "details", Map.of("database", "H2")),
                        "diskSpace", Map.of("status", "UP", "details", Map.of("total", 1000000000, "free", 500000000, "threshold", 10485760)),
                        "ping", Map.of("status", "UP")
                )
        ));
    }
}