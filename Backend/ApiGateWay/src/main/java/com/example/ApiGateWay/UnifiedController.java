package com.example.ApiGateWay;

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

            response.put("status", "CREATED");
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

            long successfullyUpdated = stockUpdateResults.stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("updated")))
                    .count();

            log.info("✅ Заказ создан: {} для пользователя {}", response.get("id"), userId);
            log.info("📦 Обновлено товаров: {}/{}", successfullyUpdated, processedItems.size());

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
                            "COMPLETED".equals(order.get("status")) ||
                                    "completed".equals(order.get("status")) ||
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

    // ==================== БЛОК 9: СБОРЩИКИ (COLLECTORS) ====================

    @PostMapping("/collector/collectors")
    public Map<String, Object> createCollector(@RequestBody Map<String, Object> collector) {
        return collectorService.createCollector(collector);
    }

    @GetMapping("/collector/collectors")
    public List<Map<String, Object>> getAllCollectors() {
        return collectorService.getAllCollectors();
    }

    @GetMapping("/collector/collectors/{collectorId}")
    public Map<String, Object> getCollector(@PathVariable String collectorId) {
        return collectorService.getCollector(collectorId);
    }

    @PutMapping("/collector/collectors/{collectorId}/status")
    public Map<String, Object> updateCollectorStatus(@PathVariable String collectorId, @RequestParam String status) {
        return collectorService.updateCollectorStatus(collectorId, status);
    }

    @PutMapping("/collector/collectors/{collectorId}/location")
    public Map<String, Object> updateCollectorLocation(@PathVariable String collectorId, @RequestParam String location) {
        return collectorService.updateCollectorLocation(collectorId, location);
    }

    @PostMapping("/collector/tasks")
    public Map<String, Object> createCollectorTask(@RequestBody Map<String, Object> task) {
        return collectorService.createTask(task);
    }

    @GetMapping("/collector/tasks")
    public List<Map<String, Object>> getAllTasks() {
        return collectorService.getAllTasks();
    }

    @GetMapping("/collector/tasks/{taskId}")
    public Map<String, Object> getTask(@PathVariable String taskId) {
        return collectorService.getTask(taskId);
    }

    @GetMapping("/collector/tasks/collector/{collectorId}")
    public List<Map<String, Object>> getCollectorTasks(@PathVariable String collectorId) {
        return collectorService.getCollectorTasks(collectorId);
    }

    @GetMapping("/collector/tasks/pending")
    public List<Map<String, Object>> getPendingTasks() {
        return collectorService.getPendingTasks();
    }

    @PutMapping("/collector/tasks/{taskId}/status")
    public Map<String, Object> updateTaskStatus(@PathVariable String taskId, @RequestParam String status) {
        return collectorService.updateTaskStatus(taskId, status);
    }

    @PostMapping("/collector/tasks/{taskId}/report-problem")
    public Map<String, Object> reportProblem(@PathVariable String taskId,
                                             @RequestParam String problemType,
                                             @RequestParam String comments) {
        return collectorService.reportProblem(taskId, problemType, comments);
    }

    @GetMapping("/collector/tasks/problems")
    public List<Map<String, Object>> getProblemTasks() {
        return collectorService.getProblemTasks();
    }

    @PutMapping("/collector/tasks/{taskId}/complete")
    public Map<String, Object> completeTask(@PathVariable String taskId) {
        return collectorService.completeTask(taskId);
    }

    @PostMapping("/collector/transactions/process-order")
    public Map<String, Object> processCollectorTransaction(@RequestBody Map<String, Object> transactionRequest) {
        return collectorService.processOrderTransaction(transactionRequest);
    }

    @PostMapping("/collector/tasks/{taskId}/report-problem-and-process")
    public Map<String, Object> reportProblemAndProcess(
            @PathVariable String taskId,
            @RequestParam String problemType,
            @RequestParam String comments,
            @RequestParam String clientId,
            @RequestParam String productId,
            @RequestParam Integer quantity) {

        Map<String, Object> problemTask = collectorService.reportProblem(taskId, problemType, comments);
        Map<String, Object> transactionRequest = Map.of(
                "taskId", taskId,
                "collectorId", problemTask.get("collectorId"),
                "clientId", clientId,
                "productId", productId,
                "quantity", quantity,
                "problemType", problemType,
                "comments", comments
        );

        Map<String, Object> transactionResult = collectorService.processOrderTransaction(transactionRequest);

        return Map.of(
                "problemReport", problemTask,
                "transactionResult", transactionResult,
                "message", "Проблема зарегистрирована и транзакция обработана"
        );
    }

    @GetMapping("/collector/{collectorId}/full-info")
    public Map<String, Object> getCollectorFullInfo(@PathVariable String collectorId) {
        Map<String, Object> collector = collectorService.getCollector(collectorId);
        List<Map<String, Object>> tasks = collectorService.getCollectorTasks(collectorId);
        List<Map<String, Object>> problemTasks = tasks.stream()
                .filter(task -> "PROBLEM".equals(task.get("status")))
                .toList();

        return Map.of(
                "collector", collector,
                "totalTasks", tasks.size(),
                "activeTasks", tasks.stream().filter(task ->
                        "NEW".equals(task.get("status")) || "IN_PROGRESS".equals(task.get("status"))).count(),
                "problemTasks", problemTasks.size(),
                "tasks", tasks
        );
    }

    // ==================== БЛОК 10: ДОСТАВКА (DELIVERY) ====================

    @PostMapping("/deliveries")
    public Object createDelivery(@RequestBody Map<String, Object> deliveryRequest) {
        return deliveryService.createDelivery(deliveryRequest);
    }

    @PostMapping("/deliveries/{deliveryId}/assign")
    public Object assignCourier(@PathVariable Integer deliveryId, @RequestBody Map<String, Object> request) {
        return deliveryService.assignCourier(deliveryId, request);
    }

    @PostMapping("/deliveries/{deliveryId}/status")
    public Object updateDeliveryStatus(@PathVariable Integer deliveryId, @RequestBody Map<String, Object> request) {
        return deliveryService.updateDeliveryStatus(deliveryId, request);
    }

    @GetMapping("/deliveries/client/{clientId}")
    public List<Object> getClientDeliveries(@PathVariable Integer clientId) {
        return deliveryService.getClientDeliveries(clientId);
    }

    @GetMapping("/deliveries/courier/{courierId}")
    public List<Object> getCourierDeliveries(@PathVariable Integer courierId) {
        return deliveryService.getCourierDeliveries(courierId);
    }

    @GetMapping("/deliveries/active")
    public List<Object> getActiveDeliveries() {
        return deliveryService.getActiveDeliveries();
    }

    @GetMapping("/deliveries")
    public List<Object> getAllDeliveries() {
        return deliveryService.getAllDeliveries();
    }

    @GetMapping("/deliveries/order/{orderId}")
    public List<Object> getDeliveriesByOrderId(@PathVariable Integer orderId) {
        return deliveryService.getDeliveriesByOrderId(orderId);
    }

    @GetMapping("/deliveries/order/{orderId}/first")
    public Object getFirstDeliveryByOrderId(@PathVariable Integer orderId) {
        return deliveryService.getFirstDeliveryByOrderId(orderId);
    }

    @PostMapping("/deliveries/{deliveryId}/cancel")
    public Object cancelDelivery(@PathVariable Integer deliveryId) {
        return deliveryService.cancelDelivery(deliveryId);
    }

    @GetMapping("/deliveries/{deliveryId}")
    public Object getDelivery(@PathVariable Integer deliveryId) {
        return deliveryService.getDelivery(deliveryId);
    }

    @GetMapping("/orders/{orderId}/delivery-full-info")
    public Map<String, Object> getOrderDeliveryInfo(@PathVariable Integer orderId) {
        List<Object> deliveries = deliveryService.getDeliveriesByOrderId(orderId);
        Object firstDelivery = deliveryService.getFirstDeliveryByOrderId(orderId);

        long activeDeliveries = deliveries.stream()
                .filter(delivery -> {
                    if (delivery instanceof Map) {
                        Map<String, Object> deliveryMap = (Map<String, Object>) delivery;
                        String status = (String) deliveryMap.get("deliveryStatus");
                        return !"DELIVERED".equals(status) && !"CANCELLED".equals(status);
                    }
                    return false;
                })
                .count();

        return Map.of(
                "orderId", orderId,
                "totalDeliveries", deliveries.size(),
                "activeDeliveries", activeDeliveries,
                "firstDelivery", firstDelivery,
                "allDeliveries", deliveries
        );
    }

    // ==================== БЛОК 11: ТРАНЗАКЦИОННЫЕ МЕТОДЫ (SAGA) ====================

    @PostMapping("/saga/transactions")
    public Map<String, Object> createTransaction(@RequestBody Map<String, Object> transactionRequest) {
        return transactionSagaClient.createTransaction(transactionRequest);
    }

    @GetMapping("/saga/transactions/{transactionId}")
    public Map<String, Object> getTransaction(@PathVariable String transactionId) {
        return transactionSagaClient.getTransaction(transactionId);
    }

    @GetMapping("/saga/transactions/collector/{collectorId}")
    public List<Map<String, Object>> getCollectorTransactions(@PathVariable String collectorId) {
        return transactionSagaClient.getCollectorTransactions(collectorId);
    }

    @GetMapping("/saga/transactions/active")
    public List<Map<String, Object>> getActiveTransactions() {
        return transactionSagaClient.getActiveTransactions();
    }

    @GetMapping("/saga/transactions/paused")
    public List<Map<String, Object>> getPausedTransactions() {
        return transactionSagaClient.getPausedTransactions();
    }

    @PostMapping("/saga/transactions/{transactionId}/scan")
    public Map<String, Object> scanItem(@PathVariable String transactionId, @RequestBody Map<String, Object> scanRequest) {
        return transactionSagaClient.scanItem(transactionId, scanRequest);
    }

    @PostMapping("/saga/transactions/{transactionId}/report-problem")
    public Map<String, Object> reportProblem(@PathVariable String transactionId, @RequestBody Map<String, Object> problemRequest) {
        return transactionSagaClient.reportProblem(transactionId, problemRequest);
    }

    @PostMapping("/saga/transactions/{transactionId}/client-decision")
    public Map<String, Object> processClientDecision(@PathVariable String transactionId, @RequestBody Map<String, Object> decisionRequest) {
        return transactionSagaClient.processClientDecision(transactionId, decisionRequest);
    }

    @GetMapping("/saga/steps/{transactionId}")
    public List<Map<String, Object>> getTransactionSteps(@PathVariable String transactionId) {
        return transactionSagaClient.getTransactionSteps(transactionId);
    }

    @PostMapping("/saga/steps/{stepId}/retry")
    public Map<String, Object> retryStep(@PathVariable Long stepId) {
        return transactionSagaClient.retryStep(stepId);
    }

    @GetMapping("/saga/health")
    public Map<String, Object> checkSagaHealth() {
        return transactionSagaClient.checkSagaHealth();
    }

    @PostMapping("/saga/compensation/{transactionId}/initiate")
    public Map<String, Object> initiateCompensation(@PathVariable String transactionId,
                                                    @RequestParam String reason,
                                                    @RequestParam(required = false) String details) {
        return transactionSagaClient.initiateCompensation(transactionId, reason, details);
    }

    @GetMapping("/saga/compensation/history/{transactionId}")
    public List<Map<String, Object>> getCompensationHistory(@PathVariable String transactionId) {
        return transactionSagaClient.getCompensationHistory(transactionId);
    }

    @PutMapping("/saga/transactions/{transactionId}/status")
    public Map<String, Object> updateTransactionStatus(@PathVariable String transactionId, @RequestBody Map<String, Object> statusRequest) {
        return transactionSagaClient.updateTransactionStatus(transactionId, statusRequest);
    }

    @PostMapping("/saga/transactions/complete-order")
    public Map<String, Object> createCompleteOrderWithSaga(@RequestBody Map<String, Object> orderRequest) {
        return transactionSagaClient.createCompleteOrderWithSaga(orderRequest);
    }

    @GetMapping("/saga/transactions/{transactionId}/full-info")
    public Map<String, Object> getTransactionFullInfo(@PathVariable String transactionId) {
        return transactionSagaClient.getTransactionFullInfo(transactionId);
    }

    @PostMapping("/saga/transactions/{transactionId}/start")
    public Map<String, Object> startTransaction(@PathVariable String transactionId) {
        Map<String, Object> statusRequest = Map.of("status", "ACTIVE");
        return transactionSagaClient.updateTransactionStatus(transactionId, statusRequest);
    }

    @PostMapping("/saga/transactions/{transactionId}/pause")
    public Map<String, Object> pauseTransaction(@PathVariable String transactionId, @RequestParam(required = false) String reason) {
        Map<String, Object> statusRequest = Map.of("status", "PAUSED", "reason", reason);
        return transactionSagaClient.updateTransactionStatus(transactionId, statusRequest);
    }

    @PostMapping("/saga/transactions/{transactionId}/resume")
    public Map<String, Object> resumeTransaction(@PathVariable String transactionId) {
        Map<String, Object> statusRequest = Map.of("status", "ACTIVE");
        return transactionSagaClient.updateTransactionStatus(transactionId, statusRequest);
    }

    @PostMapping("/saga/transactions/{transactionId}/complete")
    public Map<String, Object> completeTransaction(@PathVariable String transactionId) {
        Map<String, Object> statusRequest = Map.of("status", "COMPLETED");
        return transactionSagaClient.updateTransactionStatus(transactionId, statusRequest);
    }

    @PostMapping("/saga/transactions/{transactionId}/cancel")
    public Map<String, Object> cancelTransaction(@PathVariable String transactionId, @RequestParam(required = false) String reason) {
        String compensationReason = reason != null ? "CANCELLED: " + reason : "CANCELLED";
        return transactionSagaClient.initiateCompensation(transactionId, compensationReason, "Manual cancellation");
    }

    @PostMapping("/orders/with-saga-orchestration")
    public Map<String, Object> createOrderWithSagaOrchestration(@RequestBody Map<String, Object> orderRequest) {
        Map<String, Object> transactionResponse = transactionSagaClient.createTransaction(orderRequest);
        String transactionId = (String) transactionResponse.get("id");
        String clientId = (String) orderRequest.get("clientId");
        String orderId = (String) orderRequest.get("orderId");

        Object cartResponse = cartService.createCart(Integer.parseInt(clientId));
        List<Map<String, Object>> items = (List<Map<String, Object>>) orderRequest.get("items");

        if (items != null) {
            for (Map<String, Object> item : items) {
                cartService.addToCart(
                        (Integer) ((Map<String, Object>) cartResponse).get("id"),
                        Integer.parseInt((String) item.get("productId")),
                        (Integer) item.get("quantity"),
                        ((Number) item.get("price")).doubleValue()
                );
            }
        }

        Map<String, Object> deliveryRequest = Map.of(
                "orderId", orderId,
                "clientId", clientId,
                "deliveryAddress", orderRequest.get("deliveryAddress"),
                "deliveryPhone", orderRequest.get("deliveryPhone")
        );

        Object deliveryResponse = deliveryService.createDelivery(deliveryRequest);

        if (items != null && !items.isEmpty()) {
            Map<String, Object> firstItem = items.get(0);
            Map<String, Object> scanRequest = Map.of(
                    "productId", firstItem.get("productId"),
                    "quantity", firstItem.get("quantity"),
                    "location", "Warehouse A"
            );
            transactionSagaClient.scanItem(transactionId, scanRequest);
        }

        return Map.of(
                "transaction", transactionResponse,
                "cart", cartResponse,
                "delivery", deliveryResponse,
                "message", "Order created with saga orchestration"
        );
    }


    // ==================== БЛОК 12: OFFICE - расширенные методы из второго файла ====================

    @GetMapping("/office/test")
    public ResponseEntity<?> officeTest() {
        try {
            log.info("✅ Office test endpoint called");
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Office API is working!");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Office test error: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Office test failed: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/office/problems/active")
    public ResponseEntity<?> getActiveProblems() {
        try {
            log.info("🔍 Office: getting active problems");

            String statusCheckSql = "SELECT DISTINCT status FROM carts ORDER BY status";
            List<String> availableStatuses = jdbcTemplate.queryForList(statusCheckSql, String.class);
            log.info("✅ Available statuses in carts: {}", availableStatuses);

            String problemStatus = null;
            List<Map<String, Object>> problems = new ArrayList<>();

            for (String status : availableStatuses) {
                if (status != null && status.equalsIgnoreCase("problem")) {
                    problemStatus = status;
                    log.info("✅ Found exact 'problem' status: '{}'", problemStatus);
                    break;
                }
            }

            if (problemStatus != null) {
                String sql = """
            SELECT 
                c.id as order_id,
                c.client_id,
                COALESCE(u.firstname, 'Клиент #' || c.client_id) as client_name,
                COALESCE(u.email, 'client' || c.client_id || '@example.com') as client_email,
                COALESCE(u.city, 'Москва') as client_city,
                COALESCE(u.age::text, '30') as client_phone,
                c.created_date as created_at,
                c.status as order_status,
                'COLLECTOR_' || (c.id % 10 + 1) as collector_id,
                'Требует внимания офиса' as details
            FROM carts c
            LEFT JOIN users u ON c.client_id = u.id
            WHERE c.status = ?
            ORDER BY c.created_date DESC
            LIMIT 20
            """;

                problems = jdbcTemplate.queryForList(sql, problemStatus);
                log.info("✅ Found {} problem records with status '{}'", problems.size(), problemStatus);
            } else {
                log.info("📭 No 'problem' status found in carts table");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("problems", problems);
            response.put("total", problems.size());
            response.put("message", problems.size() > 0 ? "Problems loaded successfully" : "No problems found in the system");
            response.put("used_status", problemStatus);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting problems: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("problems", new ArrayList<>());
            response.put("total", 0);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        }
    }

    private List<Map<String, Object>> generateTestProblems() {
        List<Map<String, Object>> problems = new ArrayList<>();
        Random random = new Random();
        String[] clientNames = {"Иван Иванов", "Мария Петрова", "Алексей Сидоров", "Екатерина Волкова", "Дмитрий Козлов"};
        String[] cities = {"Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань"};
        String[] problemsList = {
                "Ноутбук ASUS ROG отсутствует на складе",
                "Мышь Logitech MX повреждена при осмотре",
                "Клавиатура Mechanical не соответствует заказу",
                "Монитор 27\" временно отсутствует",
                "Наушники Sony с браком"
        };

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> problem = new HashMap<>();
            problem.put("id", i);
            problem.put("order_id", 1000 + i);
            problem.put("client_id", i);
            problem.put("client_name", clientNames[i-1]);
            problem.put("client_email", "client" + i + "@example.com");
            problem.put("client_city", cities[random.nextInt(cities.length)]);
            problem.put("client_phone", "+7 (999) " + (100 + i) + "-" + (10 + i) + "-" + (20 + i));
            problem.put("collector_id", "COLLECTOR_" + (random.nextInt(10) + 1));
            problem.put("details", problemsList[i-1]);
            problem.put("created_at", new Date(System.currentTimeMillis() - random.nextInt(3600000)));
            problem.put("order_status", "problem");
            problem.put("status", random.nextBoolean() ? "PENDING" : "NOTIFIED");

            problems.add(problem);
        }

        return problems;
    }

    @GetMapping("/office/check-relations")
    public ResponseEntity<?> checkTableRelations() {
        try {
            log.info("🔗 Checking table relations");
            Map<String, Object> result = new HashMap<>();

            String[] tables = {"users", "carts", "cart_items"};
            Map<String, Boolean> tableExists = new HashMap<>();

            for (String table : tables) {
                try {
                    String checkSql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?)";
                    Boolean exists = jdbcTemplate.queryForObject(checkSql, Boolean.class, table);
                    tableExists.put(table, exists);
                } catch (Exception e) {
                    tableExists.put(table, false);
                }
            }
            result.put("tables_exist", tableExists);

            if (tableExists.getOrDefault("users", false)) {
                String usersSql = """
                SELECT column_name, data_type, is_nullable 
                FROM information_schema.columns 
                WHERE table_schema = 'public' AND table_name = 'users' 
                ORDER BY ordinal_position
                """;
                List<Map<String, Object>> usersStructure = jdbcTemplate.queryForList(usersSql);
                result.put("users_structure", usersStructure);

                String sampleUsers = "SELECT id, username, firstname, email, status FROM users LIMIT 5";
                List<Map<String, Object>> usersSample = jdbcTemplate.queryForList(sampleUsers);
                result.put("users_sample", usersSample);
            }

            if (tableExists.getOrDefault("carts", false)) {
                String cartsSql = """
                SELECT column_name, data_type, is_nullable 
                FROM information_schema.columns 
                WHERE table_schema = 'public' AND table_name = 'carts' 
                ORDER BY ordinal_position
                """;
                List<Map<String, Object>> cartsStructure = jdbcTemplate.queryForList(cartsSql);
                result.put("carts_structure", cartsStructure);

                String statusSql = "SELECT status, COUNT(*) as count FROM carts GROUP BY status ORDER BY status";
                List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(statusSql);
                result.put("carts_status_stats", statusStats);

                String relationsSql = """
                SELECT 
                    COUNT(DISTINCT c.client_id) as unique_client_ids,
                    COUNT(DISTINCT u.id) as unique_user_ids,
                    SUM(CASE WHEN u.id IS NULL THEN 1 ELSE 0 END) as missing_users
                FROM carts c
                LEFT JOIN users u ON c.client_id = u.id
                """;
                Map<String, Object> relations = jdbcTemplate.queryForMap(relationsSql);
                result.put("table_relations", relations);
            }

            String sampleProblemSql = """
            SELECT 
                c.id as cart_id,
                c.client_id,
                u.firstname,
                u.email,
                c.status,
                c.created_date
            FROM carts c
            LEFT JOIN users u ON c.client_id = u.id
            WHERE c.status = 'problem'
            LIMIT 5
            """;

            try {
                List<Map<String, Object>> sampleProblems = jdbcTemplate.queryForList(sampleProblemSql);
                result.put("sample_problems_query", sampleProblems);
            } catch (Exception queryError) {
                result.put("sample_problems_error", queryError.getMessage());
            }

            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error checking relations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/office/simple-test")
    public ResponseEntity<?> simpleTest() {
        try {
            log.info("✅ Office simple test endpoint");
            List<Map<String, Object>> testProblems = new ArrayList<>();

            Random random = new Random();
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> problem = new HashMap<>();
                problem.put("id", i);
                problem.put("order_id", 1000 + i);
                problem.put("client_name", "Клиент Тест " + i);
                problem.put("client_email", "client" + i + "@example.com");
                problem.put("collector_id", "COLLECTOR_" + (random.nextInt(10) + 1));
                problem.put("details", "Тестовая проблема #" + i);
                problem.put("status", "PENDING");
                problem.put("created_at", new Date());
                testProblems.add(problem);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("problems", testProblems);
            response.put("total", testProblems.size());
            response.put("message", "Test data generated");
            response.put("timestamp", System.currentTimeMillis());
            response.put("note", "Это тестовые данные без подключения к БД");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Simple test error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/office/notify-client")
    public ResponseEntity<?> notifyClient(@RequestBody Map<String, Object> request) {
        try {
            Integer problemId = (Integer) request.get("problemId");
            String message = (String) request.get("message");
            String clientEmail = (String) request.get("clientEmail");
            String clientName = (String) request.get("clientName");

            log.info("📧 Office: sending email to {} ({}) for problem #{}",
                    clientName, clientEmail, problemId);

            log.info("\n" + "=".repeat(60));
            log.info("📧 EMAIL SIMULATION");
            log.info("To: {}", clientEmail);
            log.info("Subject: Problem with order #{}", problemId);
            log.info("Message:\n{}", message);
            log.info("=".repeat(60) + "\n");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email sent to client");
            response.put("clientEmail", clientEmail);
            response.put("clientName", clientName);
            response.put("problemId", problemId);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error sending email: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/office/make-decision")
    public ResponseEntity<?> makeDecision(@RequestBody Map<String, Object> request) {
        try {
            Integer orderId = (Integer) request.get("orderId");
            String decision = (String) request.get("decision");
            String comments = (String) request.get("comments");

            log.info("🤔 Office: making decision for order #{}, decision: {}", orderId, decision);

            String getOrderSql = "SELECT client_id, status FROM carts WHERE id = ?";
            Map<String, Object> orderInfo = jdbcTemplate.queryForMap(getOrderSql, orderId);
            Integer clientId = (Integer) orderInfo.get("client_id");
            String currentStatus = (String) orderInfo.get("status");

            String newStatus;
            String decisionText;

            if ("CANCEL_ORDER".equals(decision)) {
                newStatus = "cancelled";
                decisionText = "Order cancelled";
            } else if ("APPROVE_WITHOUT_PRODUCT".equals(decision)) {
                newStatus = "processing";
                decisionText = "Continue without product";
            } else if ("WAIT_FOR_PRODUCT".equals(decision)) {
                newStatus = "waiting";
                decisionText = "Wait for product";
            } else {
                newStatus = "processing";
                decisionText = "Continue";
            }

            String updateSql = "UPDATE carts SET status = ? WHERE id = ?";
            int updatedRows = jdbcTemplate.update(updateSql, newStatus, orderId);

            if (updatedRows > 0) {
                log.info("✅ Order #{} status changed from '{}' to '{}'",
                        orderId, currentStatus, newStatus);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("orderId", orderId);
                response.put("clientId", clientId);
                response.put("oldStatus", currentStatus);
                response.put("newStatus", newStatus);
                response.put("decision", decision);
                response.put("decisionText", decisionText);
                response.put("message", "Decision successfully applied");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Order not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            log.error("❌ Error making decision: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/office/order/{orderId}/full-info")
    public ResponseEntity<?> getOrderFullInfo(@PathVariable Integer orderId) {
        try {
            log.info("📄 Office: full information for order #{}", orderId);

            Map<String, Object> order;
            try {
                String orderSql = "SELECT * FROM carts WHERE id = ?";
                order = jdbcTemplate.queryForMap(orderSql, orderId);
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Order not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Map<String, Object> client = new HashMap<>();
            Integer clientId = null;
            if (order.get("client_id") != null) {
                try {
                    clientId = (Integer) order.get("client_id");
                    if (clientId != null) {
                        String clientSql = "SELECT id, firstname, email, phone, city FROM users WHERE id = ?";
                        client = jdbcTemplate.queryForMap(clientSql, clientId);
                    }
                } catch (Exception e) {
                    log.warn("Could not get client info for client_id {}: {}", clientId, e.getMessage());
                    client.put("error", "Client not found");
                    client.put("client_id", clientId);
                }
            }

            List<Map<String, Object>> items = new ArrayList<>();
            try {
                String itemsSql = """
            SELECT ci.*, 
                   p.name as product_name, 
                   p.price as product_price
            FROM cart_items ci
            LEFT JOIN usersklad p ON ci.product_id = p.id
            WHERE ci.cart_id = ?
            """;
                items = jdbcTemplate.queryForList(itemsSql, orderId);
            } catch (Exception e) {
                log.warn("Could not get items for order {}: {}", orderId, e.getMessage());
            }

            double totalAmount = 0.0;
            for (Map<String, Object> item : items) {
                Object priceObj = item.get("product_price");
                Object quantityObj = item.get("quantity");

                if (priceObj != null && quantityObj != null) {
                    try {
                        if (priceObj instanceof Number && quantityObj instanceof Number) {
                            double price = ((Number) priceObj).doubleValue();
                            int quantity = ((Number) quantityObj).intValue();
                            totalAmount += price * quantity;
                        }
                    } catch (Exception e) {
                        log.warn("Error calculating amount for item: {}", e.getMessage());
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("order", order);
            response.put("client", client);
            response.put("items", items);
            response.put("totalAmount", totalAmount);
            response.put("itemCount", items.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting order info: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/office/debug/database")
    public ResponseEntity<?> debugDatabase() {
        try {
            log.info("🔧 Office: database diagnostics");
            Map<String, Object> debugInfo = new HashMap<>();

            String cartsSql = "SELECT id, client_id, status, created_date FROM carts WHERE status = 'problem' ORDER BY id DESC";
            List<Map<String, Object>> problemCarts = jdbcTemplate.queryForList(cartsSql);
            debugInfo.put("problem_carts", problemCarts);
            debugInfo.put("problem_carts_count", problemCarts.size());

            String usersSql = "SELECT COUNT(*) as user_count FROM users";
            Long userCount = jdbcTemplate.queryForObject(usersSql, Long.class);
            debugInfo.put("user_count", userCount);

            String itemsSql = "SELECT COUNT(*) as item_count FROM cart_items";
            Long itemCount = jdbcTemplate.queryForObject(itemsSql, Long.class);
            debugInfo.put("cart_item_count", itemCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("debug", debugInfo);
            response.put("message", "Diagnostics completed");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error in diagnostics: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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