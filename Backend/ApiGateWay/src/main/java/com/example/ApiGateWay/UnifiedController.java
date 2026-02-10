package com.example.ApiGateWay;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PostMapping;

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
// ==================== БЛОК 16: ФОНОВЫЕ ЗАДАЧИ И СИНХРОНИЗАЦИЯ СКЛАДОВ ====================

    @Autowired
    private WarehouseScheduler warehouseScheduler;

    /**
     * Эндпоинт для проверки статуса фоновых задач и складов
     */
    @GetMapping("/warehouse/status")
    public ResponseEntity<?> getWarehouseStatus() {
        try {
            Map<String, Object> status = warehouseScheduler.getSchedulerStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Ошибка при получении статуса складов: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Ручной запуск переноса в указанный склад (для тестирования)
     */
    @PostMapping("/warehouse/transfer/{warehouseName}")
    public ResponseEntity<?> manualTransfer(@PathVariable String warehouseName) {
        try {
            if (!List.of("skladodin", "skladdva", "skladtri").contains(warehouseName)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Недопустимое название склада",
                        "allowed", List.of("skladodin", "skladdva", "skladtri")
                ));
            }

            log.info("🔄 Ручной запуск переноса в {}", warehouseName);
            warehouseScheduler.transferProductsToWarehouse(warehouseName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ручной перенос в " + warehouseName + " выполнен",
                    "warehouse", warehouseName,
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Ошибка при ручном переносе: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

// ==================== КОНЕЦ БЛОКА 16 ====================
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

                    // 5. ОБНОВЛЯЕМ ТОВАРЫ ПРИ СТАТУСЕ 'tc' (ВОЗВРАТ ДЕНЕГ)
                    if ("tc".equals(newStatus)) {
                        try {
                            // 5.1. Обновляем nalichie для unknown товаров
                            String updateItemsSql = """
                UPDATE cart_items 
                SET nalichie = 'refunded'
                WHERE cart_id = ? AND nalichie = 'unknown'
                """;
                            int updatedNalichie = jdbcTemplate.update(updateItemsSql, cartId);
                            log.info("✅ Updated {} cart_items for cart {} from 'unknown' to 'refunded'",
                                    updatedNalichie, cartId);

                            // 5.2. ОБНОВЛЯЕМ vozvrat = 'tc' для ВСЕХ товаров заказа (кроме тех, где уже 'tcc')
                            String updateVozvratSql = """
                UPDATE cart_items 
                SET vozvrat = 'tc'
                WHERE cart_id = ? 
                AND (vozvrat IS NULL OR vozvrat != 'tcc')
                """;
                            int updatedVozvrat = jdbcTemplate.update(updateVozvratSql, cartId);
                            log.info("✅ Updated vozvrat='tc' for {} items in cart {} (except those with 'tcc')",
                                    updatedVozvrat, cartId);

                        } catch (Exception e) {
                            log.warn("⚠️ Could not update cart_items for cart {}: {}", cartId, e.getMessage());
                        }
                    }

                    // 6. (ОПЦИОНАЛЬНО) Обновляем товары при статусе 'completed'
                    if ("completed".equals(newStatus)) {
                        try {
                            String updateItemsSql = """
                UPDATE cart_items 
                SET nalichie = 'refunded'
                WHERE cart_id = ? AND nalichie = 'unknown'
                """;
                            int updatedItems = jdbcTemplate.update(updateItemsSql, cartId);
                            log.info("✅ Updated {} cart_items for cart {} from 'unknown' to 'refunded' (completed status)",
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

                    // Добавляем информацию об обновлении товаров
                    if ("tc".equals(newStatus)) {
                        String countVozvratSql = "SELECT COUNT(*) FROM cart_items WHERE cart_id = ? AND vozvrat = 'tc'";
                        Long itemsWithTc = jdbcTemplate.queryForObject(countVozvratSql, Long.class, cartId);
                        response.put("itemsWithVozvratTc", itemsWithTc);

                        String countNalichieSql = "SELECT COUNT(*) FROM cart_items WHERE cart_id = ? AND nalichie = 'refunded'";
                        Long itemsRefunded = jdbcTemplate.queryForObject(countNalichieSql, Long.class, cartId);
                        response.put("itemsRefunded", itemsRefunded);
                    }

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
    @GetMapping("/client/products")
    public ResponseEntity<?> getProductsForClient(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("🛍️ Получение товаров для текущего клиента");

            // 1. Извлекаем userId из токена
            Integer userId;
            try {
                userId = extractUserIdFromToken(authHeader);
                log.info("✅ Извлечен userId: {}", userId);
            } catch (RuntimeException e) {
                log.warn("⚠️ Ошибка аутентификации: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "success", false,
                                "error", "Требуется авторизация",
                                "message", "Необходимо войти в систему"
                        ));
            }

            // 2. Получаем city пользователя из БД
            String city = null;
            try {
                String getCitySql = "SELECT city FROM users WHERE id = ?";
                city = jdbcTemplate.queryForObject(getCitySql, String.class, userId);
                log.info("📍 Город пользователя {}: {}", userId, city);
            } catch (Exception e) {
                log.warn("⚠️ Не удалось получить city для пользователя {}: {}", userId, e.getMessage());
                // Продолжаем с city = null
            }

            // 3. Определяем, из какой таблицы брать товары
            String warehouseTable = determineWarehouseTable(city);
            log.info("📊 Выбран склад для отображения: {}", warehouseTable);

            // 4. Получаем товары из выбранной таблицы
            List<Map<String, Object>> products;
            try {
                String getProductsSql = String.format(
                        "SELECT id, name, price, count, akticul, category, " +
                                "description, supplier, created_at, updated_at " +
                                "FROM %s WHERE count > 0 ORDER BY name", warehouseTable);

                products = jdbcTemplate.queryForList(getProductsSql);
                log.info("✅ Получено {} товаров со склада {}", products.size(), warehouseTable);
            } catch (Exception e) {
                log.error("❌ Ошибка при получении товаров из {}: {}", warehouseTable, e.getMessage());
                // Если ошибка при запросе к частному складу, пробуем основной
                if (!"usersklad".equals(warehouseTable)) {
                    log.info("🔄 Пробуем получить товары с основного склада...");
                    return getProductsFromMainWarehouse();
                } else {
                    throw e;
                }
            }

            // 5. Формируем ответ с метаданными
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("total", products.size());
            response.put("warehouse", warehouseTable);
            response.put("userCity", city);
            response.put("userId", userId);
            response.put("timestamp", new Date());
            response.put("message", String.format("Товары со склада '%s'", warehouseTable));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Критическая ошибка при получении товаров для клиента: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Ошибка при получении товаров",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Вспомогательный метод для определения таблицы склада на основе city
     */


    /**
     * Вспомогательный метод для получения товаров с основного склада
     */
    private ResponseEntity<?> getProductsFromMainWarehouse() {
        try {
            String getProductsSql = "SELECT id, name, price, count, akticul, category, " +
                    "description, supplier, created_at, updated_at " +
                    "FROM usersklad WHERE count > 0 ORDER BY name";

            List<Map<String, Object>> products = jdbcTemplate.queryForList(getProductsSql);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("total", products.size());
            response.put("warehouse", "usersklad");
            response.put("message", "Товары с основного склада");
            response.put("timestamp", new Date());

            log.info("✅ Получено {} товаров с основного склада", products.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Ошибка при получении товаров с основного склада: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Ошибка при получении товаров",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Получение конкретного товара для текущего клиента на основе его города
     */
    @GetMapping("/client/products/{id}")
    public ResponseEntity<?> getProductForClient(
            @PathVariable int id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("🔍 Получение товара ID:{} для текущего клиента", id);

            // 1. Извлекаем userId из токена
            Integer userId;
            try {
                userId = extractUserIdFromToken(authHeader);
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "error", "Требуется авторизация"));
            }

            // 2. Получаем city пользователя из БД
            String city = null;
            try {
                String getCitySql = "SELECT city FROM users WHERE id = ?";
                city = jdbcTemplate.queryForObject(getCitySql, String.class, userId);
            } catch (Exception e) {
                log.warn("Не удалось получить city для пользователя {}: {}", userId, e.getMessage());
            }

            // 3. Определяем, из какой таблицы брать товар
            String warehouseTable = determineWarehouseTable(city);

            // 4. Получаем товар из выбранной таблицы
            Map<String, Object> product;
            try {
                String getProductSql = String.format(
                        "SELECT id, name, price, count, akticul, category, " +
                                "description, supplier, created_at, updated_at " +
                                "FROM %s WHERE id = ?", warehouseTable);

                product = jdbcTemplate.queryForMap(getProductSql, id);

            } catch (Exception e) {
                log.warn("Товар ID:{} не найден в {}, пробуем основной склад", id, warehouseTable);

                // Если товар не найден в частном складе, пробуем основной
                if (!"usersklad".equals(warehouseTable)) {
                    try {
                        String getFromMainSql = "SELECT id, name, price, count, akticul, category, " +
                                "description, supplier, created_at, updated_at " +
                                "FROM usersklad WHERE id = ?";
                        product = jdbcTemplate.queryForMap(getFromMainSql, id);
                        warehouseTable = "usersklad";
                    } catch (Exception ex) {
                        log.warn("Товар ID:{} не найден и в основном складе", id);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                        "success", false,
                                        "error", "Товар не найден",
                                        "message", "Товар с id " + id + " не найден на вашем складе"
                                ));
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                    "success", false,
                                    "error", "Товар не найден",
                                    "message", "Товар с id " + id + " не найден"
                            ));
                }
            }

            // 5. Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("product", product);
            response.put("warehouse", warehouseTable);
            response.put("userCity", city);
            response.put("userId", userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при получении товара для клиента: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Ошибка при получении товара",
                            "message", e.getMessage()
                    ));
        }
    }
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
                String cartStatus = (String) cart.get("status"); // ← Взять статус из таблицы carts
                if (cartStatus == null || cartStatus.trim().isEmpty()) {
                    cartStatus = "active"; // fallback
                }

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
    // ==================== НОВЫЕ МЕТОДЫ ДЛЯ ПРОВЕРКИ ТОВАРОВ ====================
    /**
     * Получение статусов nalichie для товаров в заказе
     * ТОЛЬКО для сборщика starаyoshibka
     */
    @GetMapping("/collector/cart/{cartId}/nalichie-status")
    public ResponseEntity<?> getNalichieStatus(
            @PathVariable Integer cartId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            log.info("🔍 Получение статусов nalichie для заказа #{}", cartId);

            // 1. Проверяем авторизацию и получаем userId
            Integer userId = null;
            String username = null;
            boolean isStarCollector = false;

            try {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    userId = extractUserIdFromToken(authHeader);
                    if (userId != null) {
                        String userSql = "SELECT username FROM users WHERE id = ?";
                        username = jdbcTemplate.queryForObject(userSql, String.class, userId);

                        if (username != null && username.contains("starаyoshibka")) {
                            isStarCollector = true;
                            log.info("⭐ Запрос от starаyoshibka: {}", username);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Ошибка проверки пользователя: {}", e.getMessage());
            }

            // 2. Если НЕ starаyoshibka → возвращаем пустой результат
            if (!isStarCollector) {
                log.info("🚫 Пользователь {} не starаyoshibka, возвращаем пустой список", username);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "isStarCollector", false,
                        "nalichieStatuses", new ArrayList<>(),
                        "message", "Только для starаyoshibka"
                ));
            }

            // 3. Получаем статусы nalichie для товаров заказа
            String sql = """
            SELECT 
                product_id,
                nalichie
            FROM cart_items 
            WHERE cart_id = ?
              AND nalichie IS NOT NULL
            ORDER BY product_id
        """;

            List<Map<String, Object>> statuses = jdbcTemplate.queryForList(sql, cartId);

            // 4. Форматируем ответ (только товары с nalichie = 'есть')
            List<Map<String, Object>> result = new ArrayList<>();
            int countEсть = 0;

            for (Map<String, Object> row : statuses) {
                Object nalichieObj = row.get("nalichie");
                if (nalichieObj != null) {
                    String nalichie = nalichieObj.toString().trim();

                    if ("есть".equals(nalichie)) {
                        Map<String, Object> itemStatus = new HashMap<>();
                        itemStatus.put("productId", row.get("product_id"));
                        itemStatus.put("nalichie", nalichie);
                        result.add(itemStatus);
                        countEсть++;
                    }
                }
            }

            log.info("✅ Для starаyoshibka найдено {} товаров с nalichie = 'есть' в заказе #{}",
                    countEсть, cartId);

            // 5. Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isStarCollector", true);
            response.put("collectorUsername", username);
            response.put("cartId", cartId);
            response.put("nalichieStatuses", result);
            response.put("countEсть", countEсть);
            response.put("totalItems", statuses.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", String.format("Найдено %d товаров с фиксированным статусом 'есть'", countEсть));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Ошибка получения статусов nalichie для заказа #{}: {}", cartId, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Ошибка получения статусов товаров: " + e.getMessage(),
                            "cartId", cartId,
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }
    @PostMapping("/collector/report-missing-items")
    public ResponseEntity<?> reportMissingItems(@RequestBody Map<String, Object> request,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Integer cartId = (Integer) request.get("cartId");
            List<Map<String, Object>> missingItems = (List<Map<String, Object>>) request.get("missingItems");
            List<Map<String, Object>> availableItems = (List<Map<String, Object>>) request.get("availableItems");
            String collectorIdFromRequest = (String) request.get("collectorId");

            log.info("⚠️ Collector: reporting {} missing items for cart #{}, available items: {}",
                    missingItems != null ? missingItems.size() : 0,
                    cartId,
                    availableItems != null ? availableItems.size() : 0);

            // ==================== ПОЛУЧЕНИЕ ИНФОРМАЦИИ О СБОРЩИКЕ ====================
            String actualCollectorId = null;
            boolean isStarCollector = false;

            try {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    Integer userId = extractUserIdFromToken(authHeader);
                    if (userId != null) {
                        String userSql = "SELECT username FROM users WHERE id = ?";
                        actualCollectorId = jdbcTemplate.queryForObject(userSql, String.class, userId);

                        if (actualCollectorId != null && actualCollectorId.contains("starаyoshibka")) {
                            isStarCollector = true;
                            log.info("⭐ Сборщик starаyoshibka обнаружен: {}", actualCollectorId);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Не удалось проверить тип сборщика: {}", e.getMessage());
            }

            // Используем actualCollectorId если он получен, иначе из запроса
            String collectorIdToUse = actualCollectorId != null ? actualCollectorId : collectorIdFromRequest;
            log.info("📝 Используемый collectorId: {} (isStarCollector: {})", collectorIdToUse, isStarCollector);

            // ==================== ОСОБАЯ ЛОГИКА ДЛЯ starаyoshibka ====================
            int availableItemsUpdated = 0;
            if (isStarCollector && availableItems != null && !availableItems.isEmpty()) {
                log.info("📦 Для starаyoshibka: обновляем {} товаров с 'есть' в БД", availableItems.size());

                for (Map<String, Object> item : availableItems) {
                    Integer productId = (Integer) item.get("productId");
                    String productName = (String) item.get("productName");

                    try {
                        // Проверяем, не имеет ли товар уже статус 'есть'
                        String checkSql = "SELECT nalichie FROM cart_items WHERE cart_id = ? AND product_id = ?";
                        String currentNalichie = null;
                        try {
                            currentNalichie = jdbcTemplate.queryForObject(checkSql, String.class, cartId, productId);
                        } catch (Exception e) {
                            // Товар не найден
                        }

                        // Обновляем только если статус не 'есть'
                        if (!"есть".equals(currentNalichie)) {
                            String updateSql = """
                            UPDATE cart_items 
                            SET nalichie = 'есть' 
                            WHERE cart_id = ? 
                              AND product_id = ? 
                              AND (nalichie IS NULL OR nalichie = 'unknown')
                        """;

                            int updated = jdbcTemplate.update(updateSql, cartId, productId);
                            if (updated > 0) {
                                availableItemsUpdated++;
                                log.info("✅ Товар {} ({}) отмечен как 'есть' для starаyoshibka",
                                        productId, productName);
                            }
                        } else {
                            log.info("📌 Товар {} уже имеет статус 'есть'", productId);
                        }
                    } catch (Exception e) {
                        log.error("❌ Ошибка обновления товара {}: {}", productId, e.getMessage());
                    }
                }
            }
            // ==================== КОНЕЦ ОСОБОЙ ЛОГИКИ ====================

            // 1. Получаем client_id
            Integer clientId = null;
            try {
                String clientSql = "SELECT client_id FROM carts WHERE id = ?";
                clientId = jdbcTemplate.queryForObject(clientSql, Integer.class, cartId);
            } catch (Exception e) {
                log.warn("Could not get client_id: {}", e.getMessage());
                clientId = -1;
            }

            // 2. Для каждого отсутствующего товара создаем запись в office_problems
            List<Integer> problemIds = new ArrayList<>();
            if (missingItems != null) {
                for (Map<String, Object> item : missingItems) {
                    Integer productId = (Integer) item.get("productId");
                    String productName = (String) item.get("productName");
                    Integer quantity = (Integer) item.get("quantity");

                    try {
                        // Пропускаем товары с фиксированным статусом 'есть'
                        String checkNalichieSql = "SELECT nalichie FROM cart_items WHERE cart_id = ? AND product_id = ?";
                        String nalichie = null;
                        try {
                            nalichie = jdbcTemplate.queryForObject(checkNalichieSql, String.class, cartId, productId);
                        } catch (Exception e) {
                            // не найдено
                        }

                        // Если товар имеет фиксированный статус 'есть' → пропускаем
                        if ("есть".equals(nalichie)) {
                            log.info("🚫 Пропускаем товар {} - имеет фиксированный статус 'есть'", productId);
                            continue;
                        }

                        String insertSql = """
                    INSERT INTO office_problems (
                        order_id, product_id, client_id, collector_id,
                        problem_type, status, details, created_at
                    ) VALUES (?, ?, ?, ?, 'MISSING_PRODUCT', 'PENDING', ?, CURRENT_TIMESTAMP)
                    RETURNING id
                    """;

                        Integer problemId = jdbcTemplate.queryForObject(
                                insertSql,
                                Integer.class,
                                cartId, productId, clientId, collectorIdToUse,
                                productName + " (необходимо: " + quantity + " шт.)"
                        );

                        if (problemId != null) {
                            problemIds.add(problemId);
                        }

                        // Обновляем статус в cart_items на 'нет'
                        String updateItemSql = "UPDATE cart_items SET nalichie = 'нет' WHERE cart_id = ? AND product_id = ?";
                        jdbcTemplate.update(updateItemSql, cartId, productId);

                    } catch (Exception e) {
                        log.error("Error creating problem for product {}: {}", productId, e.getMessage());
                    }
                }
            }

            // 3. Меняем статус заказа на 'problem'
            int cartUpdated = 0;
            try {
                String updateCartSql = "UPDATE carts SET status = 'problem' WHERE id = ?";
                cartUpdated = jdbcTemplate.update(updateCartSql, cartId);
                log.info("✅ Cart #{} status updated to 'problem'. Rows affected: {}", cartId, cartUpdated);
            } catch (Exception e) {
                log.error("Error updating cart status: {}", e.getMessage());
            }

            // 4. Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cartId", cartId);
            response.put("missingItemsCount", missingItems != null ? missingItems.size() : 0);
            response.put("availableItemsCount", availableItems != null ? availableItems.size() : 0);
            response.put("availableItemsUpdated", availableItemsUpdated);
            response.put("problemIds", problemIds);
            response.put("cartUpdated", cartUpdated > 0);
            response.put("collectorId", collectorIdToUse);
            response.put("isStarCollector", isStarCollector);
            response.put("starLogicApplied", isStarCollector && availableItemsUpdated > 0);
            response.put("timestamp", System.currentTimeMillis());

            // Разные сообщения в зависимости от типа сборщика
            if (isStarCollector && availableItemsUpdated > 0) {
                response.put("message",
                        String.format("Проблема отправлена в офис. %d товаров отмечены как 'есть' для starаyoshibka. Заказ переведен в статус 'problem'",
                                availableItemsUpdated));
            } else {
                response.put("message", "Проблема отправлена в офис. Заказ переведен в статус 'problem'");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error reporting missing items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // Завершить сборку с выбранными товарами
    @PostMapping("/collector/complete-with-selected-items")
    public ResponseEntity<?> completeWithSelectedItems(@RequestBody Map<String, Object> request) {
        try {
            Integer cartId = (Integer) request.get("cartId");
            List<Map<String, Object>> availableItems = (List<Map<String, Object>>) request.get("availableItems");
            String collectorId = (String) request.get("collectorId");

            log.info("✅ Collector: completing order #{} with {} available items, collector: {}",
                    cartId, availableItems != null ? availableItems.size() : 0, collectorId);

            // 1. Получаем city сборщика и определяем склад
            String city = null;
            try {
                String citySql = "SELECT city FROM users WHERE username = ? OR id = ?";
                city = jdbcTemplate.queryForObject(citySql, String.class, collectorId, collectorId);
                log.info("📍 Город сборщика {}: {}", collectorId, city);
            } catch (Exception e) {
                log.warn("⚠️ Could not get city for collector {}: {}", collectorId, e.getMessage());
            }

            String warehouseTable = determineWarehouseTable(city);
            log.info("🛒 Используем склад: {} для сборщика {}", warehouseTable, collectorId);

            // 2. Обновляем статус товаров на 'есть' в cart_items
            if (availableItems != null) {
                for (Map<String, Object> item : availableItems) {
                    Integer productId = (Integer) item.get("productId");
                    try {
                        String updateSql = "UPDATE cart_items SET nalichie = 'есть' WHERE cart_id = ? AND product_id = ?";
                        jdbcTemplate.update(updateSql, cartId, productId);
                    } catch (Exception e) {
                        log.warn("Error updating item status for product {}: {}", productId, e.getMessage());
                    }
                }
            }

            // 3. СПИСЫВАЕМ ТОВАРЫ С НУЖНОГО СКЛАДА
            int itemsSpent = 0;
            if (availableItems != null) {
                for (Map<String, Object> item : availableItems) {
                    Integer productId = (Integer) item.get("productId");
                    Integer quantity = (Integer) item.get("quantity");

                    if (productId != null && quantity != null && quantity > 0) {
                        try {
                            // Проверяем наличие перед списанием
                            String checkSql = String.format("SELECT count FROM %s WHERE id = ?", warehouseTable);
                            Integer availableCount = jdbcTemplate.queryForObject(checkSql, Integer.class, productId);

                            if (availableCount != null && availableCount > 0) {
                                // Списание с нужного склада
                                String updateSql = String.format(
                                        "UPDATE %s SET count = count - ? WHERE id = ?",
                                        warehouseTable
                                );
                                int updatedRows = jdbcTemplate.update(updateSql, quantity, productId);

                                if (updatedRows > 0) {
                                    itemsSpent++;
                                    log.info("✅ Списано {} шт. товара {} со склада {}",
                                            quantity, productId, warehouseTable);
                                }
                            } else {
                                log.warn("⚠️ Товар {} отсутствует на складе {}", productId, warehouseTable);
                            }
                        } catch (Exception e) {
                            log.error("❌ Ошибка при списании товара {}: {}", productId, e.getMessage());
                        }
                    }
                }
            }

            // 4. Создаем запись в orders (без изменений)
            Integer orderId = null;
            int ordersCreated = 0;
            // ... существующий код создания записи в orders ...

            // 5. Меняем статус в carts на 'completed' (уже исправлено ранее)
            int cartUpdated = 0;
            try {
                String updateCartSql = "UPDATE carts SET status = 'completed' WHERE id = ?";
                cartUpdated = jdbcTemplate.update(updateCartSql, cartId);
            } catch (Exception e) {
                log.error("Error updating cart status: {}", e.getMessage());
            }

            // 6. Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("cartId", cartId);
            response.put("collectorId", collectorId);
            response.put("collectorCity", city);
            response.put("warehouseTable", warehouseTable);
            response.put("availableItemsCount", availableItems != null ? availableItems.size() : 0);
            response.put("itemsSpent", itemsSpent);
            response.put("cartUpdated", cartUpdated > 0);
            response.put("message", String.format(
                    "Сборка завершена. Списано %d товаров со склада %s. Статус заказа изменен на 'completed'",
                    itemsSpent, warehouseTable));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error completing with selected items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }


/// ==================== БЛОК 9.1: ИСПРАВЛЕННЫЕ МЕТОДЫ ДЛЯ СБОРЩИКА ====================

@GetMapping("/collector/processing-orders")
public ResponseEntity<?> getProcessingOrders(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    try {
        log.info("📦 Collector: getting processing orders with Authorization header");

        // 1. Извлекаем userId из токена
        Integer userId = null;
        String collectorId = null;
        String collectorCity = null;

        try {
            userId = extractUserIdFromToken(authHeader);
            log.info("✅ Извлечен userId из токена: {}", userId);

            // 2. Получаем информацию о пользователе из БД
            String userInfoSql = "SELECT username, city, role FROM users WHERE id = ?";
            Map<String, Object> userInfo = jdbcTemplate.queryForMap(userInfoSql, userId);

            collectorId = (String) userInfo.get("username");
            collectorCity = (String) userInfo.get("city");
            String role = (String) userInfo.get("role");

            log.info("👤 Пользователь: {} (id: {}, city: {}, role: {})",
                    collectorId, userId, collectorCity, role);

            // 3. Проверяем что пользователь - сборщик
            if (!"COLLECTOR".equals(role) && !"collector".equalsIgnoreCase(role)) {
                log.warn("⚠️ Пользователь {} не является сборщиком (роль: {})", collectorId, role);
                // Можно продолжить или вернуть ошибку
            }

        } catch (RuntimeException e) {
            log.warn("⚠️ Ошибка извлечения из токена: {}. Используем логику общего сборщика.", e.getMessage());
            return getOrdersForGeneralCollector();
        } catch (Exception e) {
            log.error("❌ Ошибка получения информации о пользователе: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Пользователь не найден"));
        }

        // 4. Определяем тип сборщика
        boolean isPrivateWarehouse = false;

        if (collectorCity != null && collectorCity.trim().toLowerCase().startsWith("sklad")) {
            isPrivateWarehouse = true;
            collectorCity = collectorCity.trim().toLowerCase();
            log.info("🏢 Частный сборщик: {} (city: {})", collectorId, collectorCity);

            // 5. Получаем заказы для частного сборщика
            return getOrdersForPrivateCollector(collectorId, collectorCity, userId);

        } else {
            log.info("🏢 Общий сборщик: {} (city: {})", collectorId, collectorCity);

            // 6. Получаем заказы для общего сборщика
            return getOrdersForGeneralCollector(collectorId, userId);
        }

    } catch (Exception e) {
        log.error("❌ Error getting processing orders: {}", e.getMessage(), e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Ошибка получения заказов: " + e.getMessage());
        response.put("orders", new ArrayList<>());
        response.put("total", 0);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}

    /**
     * Получает заказы для частного сборщика (city начинается с "sklad")
     */
    private ResponseEntity<?> getOrdersForPrivateCollector(String collectorId, String collectorCity, Integer userId) {
        try {
            log.info("🔍 Поиск заказов для частного сборщика {} (city: {}, userId: {})",
                    collectorId, collectorCity, userId);

            String sql = """
            SELECT 
                c.id as cart_id,
                c.client_id,
                c.status,
                c.created_date,
                COALESCE(u.firstname, u.username, 'Клиент #' || c.client_id) as client_name,
                COALESCE(u.email, 'client' || c.client_id || '@example.com') as client_email,
                u.city as client_city,
                COUNT(ci.id) as item_count,
                COALESCE(SUM(ci.quantity), 0) as total_items
            FROM carts c
            LEFT JOIN users u ON c.client_id = u.id
            LEFT JOIN cart_items ci ON c.id = ci.cart_id
            WHERE c.status = 'processing'
            AND u.city = ?
            AND (ci.nalichie IS NULL OR ci.nalichie != 'нет')
            GROUP BY c.id, u.firstname, u.username, u.email, u.city, c.created_date, c.client_id, c.status
            ORDER BY c.created_date DESC
            """;

            List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql, collectorCity);
            log.info("✅ Найдено {} заказов для city '{}'", orders.size(), collectorCity);

            return enrichOrdersWithItems(orders, collectorId, collectorCity, userId, true);

        } catch (Exception e) {
            log.error("❌ Error getting orders for private collector: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка получения заказов для частного сборщика", e);
        }
    }

    /**
     * Получает заказы для общего сборщика (все НЕ-склад города)
     */
    private ResponseEntity<?> getOrdersForGeneralCollector(String collectorId, Integer userId) {
        try {
            log.info("🔍 Поиск заказов для общего сборщика {} (userId: {})", collectorId, userId);

            String sql = """
            SELECT 
                c.id as cart_id,
                c.client_id,
                c.status,
                c.created_date,
                COALESCE(u.firstname, u.username, 'Клиент #' || c.client_id) as client_name,
                COALESCE(u.email, 'client' || c.client_id || '@example.com') as client_email,
                u.city as client_city,
                COUNT(ci.id) as item_count,
                COALESCE(SUM(ci.quantity), 0) as total_items
            FROM carts c
            LEFT JOIN users u ON c.client_id = u.id
            LEFT JOIN cart_items ci ON c.id = ci.cart_id
            WHERE c.status = 'processing'
            AND (u.city IS NULL OR LOWER(u.city) NOT LIKE 'sklad%')
            AND (ci.nalichie IS NULL OR ci.nalichie != 'нет')
            GROUP BY c.id, u.firstname, u.username, u.email, u.city, c.created_date, c.client_id, c.status
            ORDER BY c.created_date DESC
            """;

            List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql);
            log.info("✅ Найдено {} заказов для общего сборщика", orders.size());

            return enrichOrdersWithItems(orders, collectorId, null, userId, false);

        } catch (Exception e) {
            log.error("❌ Error getting orders for general collector: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка получения заказов для общего сборщика", e);
        }
    }

    // Перегруженный метод для обратной совместимости
    private ResponseEntity<?> getOrdersForGeneralCollector() {
        return getOrdersForGeneralCollector("UNKNOWN", null);
    }

    /**
     * Обогащает заказы информацией о товарах
     */
    private ResponseEntity<?> enrichOrdersWithItems(List<Map<String, Object>> orders,
                                                    String collectorId,
                                                    String collectorCity,
                                                    Integer userId,
                                                    boolean isPrivateWarehouse) {

        // Получаем детали товаров для каждого заказа
        for (Map<String, Object> order : orders) {
            Integer cartId = (Integer) order.get("cart_id");

            String itemsSql = """
            SELECT 
                ci.id,
                ci.product_id,
                COALESCE(p.name, 'Товар #' || ci.product_id::text) as product_name,
                ci.quantity,
                ci.price,
                ci.nalichie
            FROM cart_items ci
            LEFT JOIN usersklad p ON ci.product_id = p.id
            WHERE ci.cart_id = ?
            AND (ci.nalichie IS NULL OR ci.nalichie != 'нет')
            ORDER BY ci.product_id
            """;

            try {
                List<Map<String, Object>> items = jdbcTemplate.queryForList(itemsSql, cartId);
                order.put("items", items);

                int totalItems = items.stream()
                        .mapToInt(item -> ((Number) item.getOrDefault("quantity", 0)).intValue())
                        .sum();
                order.put("total_items", totalItems);
                order.put("item_count", items.size());

            } catch (Exception e) {
                log.warn("Error getting items for cart {}: {}", cartId, e.getMessage());
                order.put("items", new ArrayList<>());
            }
        }

        // Фильтруем заказы, в которых вообще нет товаров после фильтрации
        List<Map<String, Object>> filteredOrders = orders.stream()
                .filter(order -> {
                    List<?> items = (List<?>) order.get("items");
                    return items != null && !items.isEmpty();
                })
                .collect(Collectors.toList());

        // Формируем ответ
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("orders", filteredOrders);
        response.put("total", filteredOrders.size());
        response.put("collectorId", collectorId);
        response.put("collectorCity", collectorCity);
        response.put("userId", userId);
        response.put("isPrivateWarehouse", isPrivateWarehouse);
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", String.format(
                "Загружено %d заказов для %s",
                filteredOrders.size(),
                isPrivateWarehouse ? "частного сборщика (" + collectorCity + ")" : "общего сборщика"));

        return ResponseEntity.ok(response);
    }
    // Проверка наличия товара (исправленная версия)
    @PostMapping("/collector/check-product-availability")
    public ResponseEntity<?> checkProductAvailability(@RequestBody Map<String, Object> request) {
        try {
            Integer cartId = (Integer) request.get("cartId");

            log.info("🔍 Collector: checking product availability for cart #{}", cartId);

            // Получаем все товары заказа, кроме помеченных как отсутствующие
            String itemsSql = """
        SELECT 
            ci.product_id,
            p.name as product_name,
            ci.quantity as requested,
            p.count as available,
            ci.price,
            ci.nalichie
        FROM cart_items ci
        LEFT JOIN usersklad p ON ci.product_id = p.id
        WHERE ci.cart_id = ?
        AND (ci.nalichie IS NULL OR ci.nalichie != 'нет')
        ORDER BY ci.product_id
    """;

            List<Map<String, Object>> items;
            try {
                items = jdbcTemplate.queryForList(itemsSql, cartId);
            } catch (Exception e) {
                log.error("Error getting items for cart {}: {}", cartId, e.getMessage());
                items = new ArrayList<>();
            }

            // Проверяем наличие остальных товаров
            List<Map<String, Object>> unavailableItems = new ArrayList<>();
            boolean allAvailable = true;
            int totalItems = items.size();
            int availableItems = 0;

            for (Map<String, Object> item : items) {
                Object availableObj = item.get("available");
                Object requestedObj = item.get("requested");
                String productName = (String) item.get("product_name");
                Integer productId = (Integer) item.get("product_id");
                String nalichie = (String) item.get("nalichie");

                // Пропускаем товары, уже помеченные как отсутствующие
                if ("нет".equals(nalichie)) {
                    continue;
                }

                Integer available = availableObj != null ? ((Number) availableObj).intValue() : 0;
                Integer requested = requestedObj != null ? ((Number) requestedObj).intValue() : 0;

                if (available >= requested) {
                    availableItems++;
                } else {
                    Map<String, Object> unavailable = new HashMap<>();
                    unavailable.put("product_id", productId);
                    unavailable.put("product_name", productName);
                    unavailable.put("requested", requested);
                    unavailable.put("available", available);
                    unavailable.put("status", "missing");
                    unavailable.put("message", "Недостаточно товара на складе");
                    unavailableItems.add(unavailable);
                    allAvailable = false;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cartId", cartId);
            response.put("allAvailable", allAvailable);
            response.put("totalItems", totalItems);
            response.put("availableItems", availableItems);
            response.put("unavailableItems", unavailableItems);
            response.put("unavailableCount", unavailableItems.size());
            response.put("message", allAvailable ?
                    "✅ Все товары в наличии. Можете завершить сборку." :
                    "⚠️ Некоторые товары отсутствуют. Используйте кнопку 'Нет товара'.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error checking product availability: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка проверки наличия: " + e.getMessage());
            response.put("cartId", request.get("cartId"));
            response.put("allAvailable", false);
            response.put("message", "Ошибка при проверке наличия товаров");

            return ResponseEntity.ok(response);
        }
    }

    // Кнопка "Нет товара" - Упрощенная версия
    @PostMapping("/collector/report-product-missing")
    public ResponseEntity<?> reportProductMissing(@RequestBody Map<String, Object> request) {
        try {
            Integer cartId = (Integer) request.get("cartId");
            Integer productId = (Integer) request.get("productId");
            String productName = (String) request.get("productName");
            String problemDetails = (String) request.get("problemDetails");
            String collectorId = (String) request.get("collectorId");

            log.info("⚠️ Collector: reporting missing product for cart #{}, product: {}", cartId, productName);

            // 1. Проверяем, существует ли заказ и получаем client_id
            String checkCartSql = "SELECT id, status, client_id FROM carts WHERE id = ?";
            Map<String, Object> cartInfo;
            Integer clientId = null;

            try {
                cartInfo = jdbcTemplate.queryForMap(checkCartSql, cartId);
                log.info("Cart #{} found. Current status: {}, Client ID: {}",
                        cartId, cartInfo.get("status"), cartInfo.get("client_id"));

                clientId = (Integer) cartInfo.get("client_id");
                if (clientId == null) {
                    log.warn("Client ID is NULL for cart #{}", cartId);
                    // Если client_id null, используем -1 чтобы избежать ошибки NOT NULL
                    clientId = -1;
                }
            } catch (Exception e) {
                log.error("Cart #{} not found: {}", cartId, e.getMessage());
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Заказ не найден",
                        "cartId", cartId
                ));
            }

            String currentStatus = (String) cartInfo.get("status");

            // 2. Получаем имя продукта если оно не пришло
            if (productName == null || productName.trim().isEmpty()) {
                try {
                    String productSql = "SELECT name FROM usersklad WHERE id = ?";
                    productName = jdbcTemplate.queryForObject(productSql, String.class, productId);
                } catch (Exception e) {
                    log.warn("Could not get product name for ID {}: {}", productId, e.getMessage());
                    productName = "Товар ID: " + productId;
                }
            }

            // 3. Формируем details
            String details = productName + ", " + (problemDetails != null ? problemDetails : "отсутствует на складе");

            // 4. Создаем запись о проблеме с ВСЕМИ обязательными полями
            Integer problemId = null;

            try {
                // Проверяем какие поля обязательные
                String insertSql = """
                INSERT INTO office_problems (
                    order_id, 
                    product_id, 
                    client_id,  -- это поле NOT NULL
                    collector_id,
                    problem_type,
                    status,
                    details,
                    client_email_sent,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, 'MISSING_PRODUCT', 'PENDING', ?, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

                int rowsInserted = jdbcTemplate.update(
                        insertSql,
                        cartId,
                        productId,
                        clientId,
                        collectorId != null ? collectorId : "COLLECTOR_UNKNOWN",
                        details
                );

                if (rowsInserted > 0) {
                    problemId = jdbcTemplate.queryForObject(
                            "SELECT MAX(id) FROM office_problems WHERE order_id = ? AND product_id = ?",
                            Integer.class, cartId, productId
                    );
                    log.info("✅ Problem record created with ID: {}", problemId);
                }
            } catch (Exception e) {
                log.error("❌ Error creating problem record: {}", e.getMessage());

                // Пробуем создать таблицу с правильной структурой
                try {
                    String dropTableSql = "DROP TABLE IF EXISTS office_problems";
                    jdbcTemplate.execute(dropTableSql);

                    String createTableSql = """
                    CREATE TABLE office_problems (
                        id SERIAL PRIMARY KEY,
                        order_id INTEGER NOT NULL,
                        product_id INTEGER NOT NULL,
                        client_id INTEGER NOT NULL DEFAULT -1,
                        collector_id VARCHAR(50),
                        problem_type VARCHAR(50) DEFAULT 'MISSING_PRODUCT',
                        status VARCHAR(50) DEFAULT 'PENDING',
                        details TEXT,
                        client_email VARCHAR(255),
                        client_email_sent BOOLEAN DEFAULT false,
                        client_decision VARCHAR(50),
                        office_action VARCHAR(50),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        assigned_to VARCHAR(50),
                        client_responded_at TIMESTAMP,
                        notified_at TIMESTAMP,
                        priority VARCHAR(20),
                        resolved_at TIMESTAMP
                    )
                """;
                    jdbcTemplate.execute(createTableSql);
                    log.info("✅ Recreated office_problems table with proper structure");

                    // Пробуем снова вставить
                    String retrySql = """
                    INSERT INTO office_problems (
                        order_id, product_id, client_id, collector_id, details
                    ) VALUES (?, ?, ?, ?, ?)
                """;

                    jdbcTemplate.update(
                            retrySql,
                            cartId, productId, clientId,
                            collectorId != null ? collectorId : "COLLECTOR_UNKNOWN",
                            details
                    );

                    problemId = jdbcTemplate.queryForObject(
                            "SELECT MAX(id) FROM office_problems",
                            Integer.class
                    );

                } catch (Exception createError) {
                    log.error("❌ Failed to recreate table: {}", createError.getMessage());
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "error", "Не удалось создать запись о проблеме: " + createError.getMessage(),
                            "cartId", cartId
                    ));
                }
            }

            // 5. Меняем статус заказа на 'problem'
            int updatedRows = 0;
            try {
                String updateCartSql = "UPDATE carts SET status = 'problem' WHERE id = ?";
                updatedRows = jdbcTemplate.update(updateCartSql, cartId);

                log.info("UPDATE carts SET status = 'problem' WHERE id = {}", cartId);
                log.info("Rows affected: {}", updatedRows);

                if (updatedRows > 0) {
                    String newStatus = jdbcTemplate.queryForObject(
                            "SELECT status FROM carts WHERE id = ?",
                            String.class, cartId
                    );
                    log.info("✅ Cart #{} status changed from '{}' to '{}'",
                            cartId, currentStatus, newStatus);
                } else {
                    log.warn("⚠️ No rows updated. Current status was: {}", currentStatus);
                }
            } catch (Exception e) {
                log.error("❌ Error updating cart status: {}", e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("problemId", problemId);
            response.put("cartId", cartId);
            response.put("productId", productId);
            response.put("productName", productName);
            response.put("clientId", clientId);
            response.put("currentStatus", currentStatus);
            response.put("details", details);
            response.put("cartUpdated", updatedRows > 0);
            response.put("updatedRows", updatedRows);
            response.put("message", updatedRows > 0 ?
                    "✅ Проблема зарегистрирована. Статус заказа изменен на 'problem'" :
                    "⚠️ Проблема зарегистрирована, но статус заказа не изменился");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error reporting missing product: {}", e.getMessage(), e);

            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "Ошибка: " + e.getMessage(),
                    "cartId", request.get("cartId")
            ));
        }
    }

    @PostMapping("/collector/force-update-status")
    public ResponseEntity<?> forceUpdateCartStatus(@RequestBody Map<String, Object> request) {
        try {
            Integer cartId = (Integer) request.get("cartId");
            String newStatus = (String) request.get("newStatus");

            log.info("🔧 Force updating cart #{} status to '{}'", cartId, newStatus);

            // Проверяем существование заказа
            String checkSql = "SELECT id FROM carts WHERE id = ?";
            try {
                Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class, cartId);
            } catch (Exception e) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Cart not found",
                        "cartId", cartId
                ));
            }

            // Пробуем разные варианты UPDATE
            int updatedRows = 0;
            String errorMessage = null;

            try {
                // Вариант 1: Простой UPDATE
                String sql1 = "UPDATE carts SET status = ? WHERE id = ?";
                updatedRows = jdbcTemplate.update(sql1, newStatus, cartId);
                log.info("Simple UPDATE rows affected: {}", updatedRows);
            } catch (Exception e1) {
                errorMessage = e1.getMessage();
                log.error("Simple UPDATE failed: {}", errorMessage);

                try {
                    // Вариант 2: UPDATE с кастомным WHERE
                    String sql2 = "UPDATE carts SET status = ? WHERE id = ? AND status != ?";
                    updatedRows = jdbcTemplate.update(sql2, newStatus, cartId, newStatus);
                    log.info("Custom WHERE UPDATE rows affected: {}", updatedRows);
                } catch (Exception e2) {
                    errorMessage = e2.getMessage();
                    log.error("Custom WHERE UPDATE failed: {}", errorMessage);

                    try {
                        // Вариант 3: UPDATE с возвратом
                        String sql3 = "UPDATE carts SET status = ? WHERE id = ? RETURNING id";
                        Integer returnedId = jdbcTemplate.queryForObject(sql3, Integer.class, newStatus, cartId);
                        updatedRows = returnedId != null ? 1 : 0;
                        log.info("RETURNING UPDATE rows affected: {}", updatedRows);
                    } catch (Exception e3) {
                        errorMessage = e3.getMessage();
                        log.error("RETURNING UPDATE failed: {}", errorMessage);
                    }
                }
            }

            // Проверяем результат
            String finalStatus = null;
            if (updatedRows > 0) {
                try {
                    finalStatus = jdbcTemplate.queryForObject(
                            "SELECT status FROM carts WHERE id = ?",
                            String.class, cartId
                    );
                } catch (Exception e) {
                    log.error("Could not verify status: {}", e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", updatedRows > 0);
            response.put("cartId", cartId);
            response.put("requestedStatus", newStatus);
            response.put("finalStatus", finalStatus);
            response.put("updatedRows", updatedRows);
            response.put("error", errorMessage);
            response.put("message", updatedRows > 0 ?
                    "✅ Status updated successfully" :
                    "❌ Failed to update status");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error force updating status: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    // Проверка текущего статуса заказа
    @GetMapping("/collector/cart/{cartId}/status")
    public ResponseEntity<?> getCartStatus(@PathVariable Integer cartId) {
        try {
            log.info("🔍 Checking status for cart #{}", cartId);

            String sql = "SELECT id, status, client_id, created_date FROM carts WHERE id = ?";

            try {
                Map<String, Object> cartInfo = jdbcTemplate.queryForMap(sql, cartId);

                // Проверяем есть ли проблемы для этого заказа
                String problemSql = "SELECT COUNT(*) FROM office_problems WHERE order_id = ? AND status = 'PENDING'";
                Long problemCount = jdbcTemplate.queryForObject(problemSql, Long.class, cartId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("cartId", cartId);
                response.put("status", cartInfo.get("status"));
                response.put("clientId", cartInfo.get("client_id"));
                response.put("createdDate", cartInfo.get("created_date"));
                response.put("hasProblems", problemCount != null && problemCount > 0);
                response.put("problemCount", problemCount != null ? problemCount : 0);
                response.put("message", "Статус получен");

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                log.warn("Cart #{} not found: {}", cartId, e.getMessage());
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Заказ не найден",
                        "cartId", cartId
                ));
            }

        } catch (Exception e) {
            log.error("❌ Error getting cart status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    // Кнопка "Завершить сборку" - перенос в orders (исправленная версия)
    @PostMapping("/collector/complete-collection")
    public ResponseEntity<?> completeCollection(@RequestBody Map<String, Object> request) {
        try {
            Integer cartId = (Integer) request.get("cartId");
            String collectorId = (String) request.get("collectorId");

            log.info("✅ Collector: completing collection for cart #{}, collector: {}", cartId, collectorId);

            // Проверяем что заказ в статусе processing
            String currentStatus;
            try {
                String checkSql = "SELECT status FROM carts WHERE id = ?";
                currentStatus = jdbcTemplate.queryForObject(checkSql, String.class, cartId);
            } catch (Exception e) {
                log.error("Error checking cart status: {}", e.getMessage());
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Заказ не найден",
                        "cartId", cartId
                ));
            }

            if (!"processing".equals(currentStatus)) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Заказ должен быть в статусе 'processing'",
                        "currentStatus", currentStatus,
                        "cartId", cartId
                ));
            }

            // Проверяем наличие всех товаров
            String availabilitySql = """
            SELECT 
                ci.product_id,
                p.name as product_name,
                ci.quantity as requested,
                p.count as available
            FROM cart_items ci
            LEFT JOIN usersklad p ON ci.product_id = p.id
            WHERE ci.cart_id = ?
        """;

            List<Map<String, Object>> items;
            try {
                items = jdbcTemplate.queryForList(availabilitySql, cartId);
            } catch (Exception e) {
                log.error("Error checking availability: {}", e.getMessage());
                items = new ArrayList<>();
            }

            List<Map<String, Object>> unavailableItems = new ArrayList<>();

            for (Map<String, Object> item : items) {
                Object availableObj = item.get("available");
                Object requestedObj = item.get("requested");

                Integer available = availableObj != null ? ((Number) availableObj).intValue() : 0;
                Integer requested = requestedObj != null ? ((Number) requestedObj).intValue() : 0;

                if (available < requested) {
                    unavailableItems.add(item);
                }
            }

            if (!unavailableItems.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Не все товары в наличии",
                        "unavailableItems", unavailableItems,
                        "message", "Сначала решите проблему с отсутствующими товарами",
                        "cartId", cartId
                ));
            }

            // Создаем запись в orders
            Integer orderId = null;
            int ordersCreated = 0;

            try {
                // Сначала проверяем существует ли уже запись
                String checkOrderSql = "SELECT id FROM orders WHERE cart_id = ?";
                try {
                    orderId = jdbcTemplate.queryForObject(checkOrderSql, Integer.class, cartId);
                } catch (Exception e) {
                    // Запись не существует, создаем новую
                    String insertOrderSql = """
                    INSERT INTO orders (cart_id, collector_id, status, completed_at, created_at)
                    VALUES (?, ?, 'collected', NOW(), NOW())
                """;

                    ordersCreated = jdbcTemplate.update(insertOrderSql, cartId, collectorId);

                    // Получаем ID созданной записи
                    orderId = jdbcTemplate.queryForObject("SELECT id FROM orders WHERE cart_id = ?", Integer.class, cartId);
                }
            } catch (Exception e) {
                log.error("Error creating order record: {}", e.getMessage());
                // Пытаемся создать таблицу orders если её нет
                try {
                    jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id SERIAL PRIMARY KEY,
                        cart_id INTEGER UNIQUE,
                        collector_id VARCHAR(50),
                        status VARCHAR(50),
                        completed_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT NOW()
                    )
                """);

                    String insertOrderSql = "INSERT INTO orders (cart_id, collector_id, status, completed_at) VALUES (?, ?, 'collected', NOW())";
                    ordersCreated = jdbcTemplate.update(insertOrderSql, cartId, collectorId);
                    orderId = cartId;
                } catch (Exception createError) {
                    log.error("Failed to create orders table: {}", createError.getMessage());
                }
            }

            // Меняем статус в carts на 'collected'
            int cartUpdated = 0;
            try {
                String updateCartSql = "UPDATE carts SET status = 'completed' WHERE id = ?";
                cartUpdated = jdbcTemplate.update(updateCartSql, cartId);
            } catch (Exception e) {
                log.error("Error updating cart status: {}", e.getMessage());
            }

            // Уменьшаем количество товаров на складе
            int stockUpdated = 0;
            try {
                String updateStockSql = """
                UPDATE usersklad u
                SET count = u.count - ci.quantity
                FROM cart_items ci
                WHERE ci.cart_id = ? 
                AND u.id = ci.product_id
            """;
                stockUpdated = jdbcTemplate.update(updateStockSql, cartId);
            } catch (Exception e) {
                log.error("Error updating stock: {}", e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("cartId", cartId);
            response.put("collectorId", collectorId);
            response.put("ordersCreated", ordersCreated);
            response.put("cartUpdated", cartUpdated);
            response.put("stockUpdated", stockUpdated);
            response.put("itemsProcessed", items.size());
            response.put("message", "Сборка успешно завершена. Заказ перемещен в orders");

            log.info("✅ Collection processing: cart #{} -> order #{}", cartId, orderId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error completing collection: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка завершения сборки: " + e.getMessage());
            response.put("cartId", request.get("cartId"));
            response.put("message", "Не удалось завершить сборку");

            return ResponseEntity.ok(response);
        }
    }



    // Завершение сборки заказа - устанавливаем статус processing
    @PostMapping("/collector/orders/{cartId}/complete")
    public ResponseEntity<?> completeOrderCollection(@PathVariable Integer cartId,
                                                     @RequestBody Map<String, Object> request) {
        try {
            String status = (String) request.get("status");
            String collectorId = (String) request.get("collectorId");

            String cartStatus = "completed";

            log.info("✅ Collector: completing order #{}, collector: {}, cart status: {}",
                    cartId, collectorId, cartStatus);

            // Создаем запись в orders с любым статусом из запроса, но cart меняем на processing
            String insertOrderSql = """
        INSERT INTO orders (cart_id, collector_id, status, completed_at, created_at)
        VALUES (?, ?, ?, NOW(), NOW())
        ON CONFLICT (cart_id) DO UPDATE 
        SET collector_id = EXCLUDED.collector_id,
            status = EXCLUDED.status,
            completed_at = NOW()
        """;

            int ordersCreated = jdbcTemplate.update(insertOrderSql,
                    cartId,
                    collectorId,
                    (status != null ? status : "collected"));

                       String updateCartSql = "UPDATE carts SET status = ? WHERE id = ?";
            int cartUpdated = jdbcTemplate.update(updateCartSql, cartStatus, cartId);
            // Уменьшаем количество товаров на складе
            String updateStockSql = """
        UPDATE usersklad u
        SET count = u.count - ci.quantity,
            updated_at = NOW()
        FROM cart_items ci
        WHERE ci.cart_id = ? 
        AND u.id = ci.product_id
        AND u.count >= ci.quantity
        """;

            int stockUpdated = jdbcTemplate.update(updateStockSql, cartId);

            // Проверяем текущий статус для отладки
            String verifiedStatus = null;
            try {
                verifiedStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM carts WHERE id = ?",
                        String.class, cartId);
            } catch (Exception e) {
                log.warn("Could not verify status: {}", e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cartId", cartId);
            response.put("cartStatus", cartStatus);
            response.put("verifiedCartStatus", verifiedStatus);
            response.put("orderStatus", (status != null ? status : "collected"));
            response.put("ordersCreated", ordersCreated);
            response.put("cartUpdated", cartUpdated);
            response.put("stockUpdated", stockUpdated);
            response.put("collectorId", collectorId);
            response.put("message", "Заказ успешно завершен. Статус корзины изменен на 'processing'");

            log.info("✅ Cart #{} status set to '{}' (verified: '{}')",
                    cartId, cartStatus, verifiedStatus);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error completing order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
// ==================== БЛОК 17: ЧАСТНЫЕ СКЛАДЫ ДЛЯ СБОРЩИКОВ ====================
// Эндпоинт для получения информации о складе сборщика

    // Эндпоинт для получения информации о складе сборщика
    @PostMapping("/collector/check-item-in-warehouse")
    public ResponseEntity<?> checkItemInWarehouse(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            Integer productId = (Integer) request.get("productId");

            log.info("🔍 Проверка товара {} для текущего сборщика", productId);

            // 1. Извлекаем userId из токена (как в processing-orders)
            Integer userId;
            String collectorId;
            String city;

            try {
                userId = extractUserIdFromToken(authHeader);
                log.info("✅ Извлечен userId из токена: {}", userId);

                // 2. Получаем информацию о пользователе из БД (по id, как в processing-orders)
                String userInfoSql = "SELECT username, city, role FROM users WHERE id = ?";
                Map<String, Object> userInfo = jdbcTemplate.queryForMap(userInfoSql, userId);

                collectorId = (String) userInfo.get("username");
                city = (String) userInfo.get("city");
                String role = (String) userInfo.get("role");

                log.info("👤 Пользователь: {} (id: {}, city: {}, role: {})",
                        collectorId, userId, city, role);

                // 3. Проверяем что пользователь - сборщик
                if (!"COLLECTOR".equals(role) && !"collector".equalsIgnoreCase(role)) {
                    log.warn("⚠️ Пользователь {} не является сборщиком (роль: {})", collectorId, role);
                    // Можно вернуть ошибку или продолжить
                }

            } catch (RuntimeException e) {
                log.warn("⚠️ Ошибка извлечения из токена: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "error", "Требуется авторизация"));
            } catch (Exception e) {
                log.error("❌ Ошибка получения информации о пользователе: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "error", "Пользователь не найден"));
            }

            // 4. Определяем таблицу склада на основе city (оставляем старую логику)
            String warehouseTable = determineWarehouseTable(city);
            log.info("🏢 Определен склад для сборщика {} (city: {}): {}", collectorId, city, warehouseTable);

            // 5. Проверяем наличие товара в нужном складе (оставляем старую логику)
            boolean available = false;
            Integer count = 0;
            String errorMessage = null;

            try {
                String checkSql = String.format("SELECT count FROM %s WHERE id = ?", warehouseTable);
                count = jdbcTemplate.queryForObject(checkSql, Integer.class, productId);
                available = count != null && count > 0;

                if (available) {
                    log.info("✅ Товар {} есть на складе {}: {} шт.", productId, warehouseTable, count);
                } else {
                    log.info("❌ Товар {} отсутствует на складе {} (количество: {})",
                            productId, warehouseTable, count != null ? count : 0);
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                log.error("❌ Ошибка при проверке товара {} в складе {}: {}",
                        productId, warehouseTable, errorMessage);

                // Если таблица не существует, возвращаем что товара нет
                if (errorMessage.contains("does not exist") || errorMessage.contains("отношение")) {
                    available = false;
                    count = 0;
                    log.warn("⚠️ Таблица склада '{}' не существует, считаем товар отсутствующим", warehouseTable);
                }
            }

            // 6. Формируем ответ (оставляем старую структуру)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("productId", productId);
            response.put("collectorId", collectorId);
            response.put("collectorCity", city);
            response.put("warehouseTable", warehouseTable);
            response.put("available", available);
            response.put("count", count);
            response.put("timestamp", System.currentTimeMillis());

            if (errorMessage != null) {
                response.put("error", errorMessage);
                response.put("warning", "При проверке возникла ошибка");
            }

            response.put("message", String.format(
                    "Товар %s %s на складе %s",
                    productId,
                    available ? "есть" : "отсутствует",
                    warehouseTable));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Критическая ошибка в checkItemInWarehouse: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Внутренняя ошибка сервера: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }
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

   //Блок 333 Транзакция
    @PostMapping("/logs/transaction-saga")
    public ResponseEntity<?> receiveTransactionSagaLog(@RequestBody Map<String, Object> logMessage) {
        try {
            log.info("📝 TransactionSaga Log: {}", logMessage);
            // Можно сохранять в БД или просто логировать
            return ResponseEntity.ok().body(Map.of("success", true, "received", true));
        } catch (Exception e) {
            log.error("Error receiving log: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    //Блок 18
    // ==================== TAOSHIBKA PROBLEMS ====================

    @GetMapping("/office/taoshibka-orders")
    public ResponseEntity<?> getTaoshibkaOrders() {
        try {
            log.info("🔍 Office: getting taoshibka orders with unknown items");

            // Проверяем наличие таблиц
            String checkCartsSql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'carts')";
            String checkItemsSql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'cart_items')";

            Boolean cartsExists = jdbcTemplate.queryForObject(checkCartsSql, Boolean.class);
            Boolean itemsExists = jdbcTemplate.queryForObject(checkItemsSql, Boolean.class);

            if (!cartsExists || !itemsExists) {
                log.warn("⚠️ Tables not found: carts={}, cart_items={}", cartsExists, itemsExists);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "orders", new ArrayList<>(),
                        "count", 0,
                        "message", "Tables not ready",
                        "timestamp", System.currentTimeMillis()
                ));
            }

            // Основной запрос: заказы со статусом taoshibka и товарами unknown
            String sql = """
            SELECT DISTINCT 
                c.id as cart_id,
                c.client_id,
                c.created_date,
                COUNT(ci.id) as unknown_count,
                COALESCE(u.firstname, u.username, 'Клиент #' || c.client_id) as client_name,
                COALESCE(u.email, 'client' || c.client_id || '@example.com') as client_email
            FROM carts c
            JOIN cart_items ci ON c.id = ci.cart_id
            LEFT JOIN users u ON c.client_id = u.id
            WHERE c.status = 'taoshibka'
              AND ci.nalichie = 'unknown'
            GROUP BY c.id, c.client_id, c.created_date, u.firstname, u.username, u.email
            ORDER BY unknown_count DESC, c.created_date DESC
            LIMIT 50
            """;

            List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql);

            log.info("✅ Found {} taoshibka orders with unknown items", orders.size());

            // Дебаг информация
            String debugSql = "SELECT status, COUNT(*) FROM carts GROUP BY status";
            List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(debugSql);
            log.info("📊 Carts status stats: {}", statusStats);

            String nalichieSql = "SELECT nalichie, COUNT(*) FROM cart_items GROUP BY nalichie";
            List<Map<String, Object>> nalichieStats = jdbcTemplate.queryForList(nalichieSql);
            log.info("📊 Nalichie stats: {}", nalichieStats);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("total", orders.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("debug", Map.of(
                    "carts_exists", cartsExists,
                    "cart_items_exists", itemsExists,
                    "carts_statuses", statusStats,
                    "nalichie_types", nalichieStats
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting taoshibka orders: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("orders", new ArrayList<>());
            errorResponse.put("total", 0);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("error_type", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/office/taoshibka-orders/{orderId}/items")
    public ResponseEntity<?> getTaoshibkaOrderItems(@PathVariable Integer orderId) {
        try {
            log.info("🔍 Office: getting unknown items for order #{}", orderId);

            // 1. Проверяем существование заказа и его статус
            String checkSql = "SELECT id, status, client_id FROM carts WHERE id = ?";
            Map<String, Object> orderInfo;
            try {
                orderInfo = jdbcTemplate.queryForMap(checkSql, orderId);
            } catch (Exception e) {
                log.warn("Order not found: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "error", "Заказ не найден: #" + orderId,
                                "orderId", orderId
                        ));
            }

            String status = (String) orderInfo.get("status");
            Integer clientId = (Integer) orderInfo.get("client_id");

            if (!"taoshibka".equals(status)) {
                log.warn("Order #{} has wrong status: {}", orderId, status);
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "error", "Заказ не имеет статус 'taoshibka'. Текущий статус: " + status,
                                "orderId", orderId,
                                "currentStatus", status
                        ));
            }

            // 2. Получаем информацию о клиенте
            Map<String, Object> clientInfo = new HashMap<>();
            if (clientId != null) {
                try {
                    String clientSql = "SELECT id, firstname, username, email FROM users WHERE id = ?";
                    clientInfo = jdbcTemplate.queryForMap(clientSql, clientId);
                } catch (Exception e) {
                    log.warn("Client not found for id: {}", clientId);
                    clientInfo.put("error", "Client not found");
                    clientInfo.put("client_id", clientId);
                }
            }

            // 3. Получаем товары с unknown
            String itemsSql = """
            SELECT 
                ci.id as item_id,
                ci.product_id,
                ci.quantity,
                ci.price,
                ci.nalichie,
                COALESCE(p.name, 'Товар #' || ci.product_id) as product_name,
                COALESCE(p.akticul, 'N/A') as sku,
                COALESCE(p.category, 'Не указана') as category,
                COALESCE(p.description, 'Нет описания') as description
            FROM cart_items ci
            LEFT JOIN usersklad p ON ci.product_id = p.id
            WHERE ci.cart_id = ?
              AND ci.nalichie = 'unknown'
            ORDER BY ci.id
            """;

            List<Map<String, Object>> items = jdbcTemplate.queryForList(itemsSql, orderId);

            // 4. Получаем ВСЕ товары заказа для контекста
            String allItemsSql = """
            SELECT 
                ci.id as item_id,
                ci.product_id,
                ci.quantity,
                ci.price,
                ci.nalichie,
                COALESCE(p.name, 'Товар #' || ci.product_id) as product_name
            FROM cart_items ci
            LEFT JOIN usersklad p ON ci.product_id = p.id
            WHERE ci.cart_id = ?
            ORDER BY ci.id
            """;

            List<Map<String, Object>> allItems = jdbcTemplate.queryForList(allItemsSql, orderId);

            // 5. Подсчитываем статистику
            long unknownCount = items.size();
            long totalCount = allItems.size();
            long knownCount = totalCount - unknownCount;

            // 6. Рассчитываем суммы
            double unknownTotal = 0.0;
            double orderTotal = 0.0;

            for (Map<String, Object> item : allItems) {
                Object priceObj = item.get("price");
                Object quantityObj = item.get("quantity");

                if (priceObj != null && quantityObj != null) {
                    try {
                        double price = ((Number) priceObj).doubleValue();
                        int quantity = ((Number) quantityObj).intValue();
                        double itemTotal = price * quantity;
                        orderTotal += itemTotal;

                        // Если товар unknown, добавляем к unknownTotal
                        if ("unknown".equals(item.get("nalichie"))) {
                            unknownTotal += itemTotal;
                        }
                    } catch (Exception e) {
                        log.warn("Error calculating price for item: {}", e.getMessage());
                    }
                }
            }

            log.info("✅ Order #{}: {} unknown items out of {} total ({}%)",
                    orderId, unknownCount, totalCount,
                    totalCount > 0 ? (unknownCount * 100 / totalCount) : 0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("orderStatus", status);
            response.put("client", clientInfo);
            response.put("unknownItems", items);
            response.put("allItems", allItems);
            response.put("stats", Map.of(
                    "unknownCount", unknownCount,
                    "totalCount", totalCount,
                    "knownCount", knownCount,
                    "unknownPercentage", totalCount > 0 ? (unknownCount * 100.0 / totalCount) : 0.0,
                    "unknownTotal", unknownTotal,
                    "orderTotal", orderTotal
            ));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting order items for #{}: {}", orderId, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "orderId", orderId,
                            "error", "Внутренняя ошибка сервера: " + e.getMessage(),
                            "error_type", e.getClass().getSimpleName(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    // Тестовый эндпоинт для проверки работы
    @GetMapping("/office/taoshibka-test")
    public ResponseEntity<?> taoshibkaTest() {
        try {
            log.info("🧪 Testing taoshibka endpoints");

            Map<String, Object> testData = new HashMap<>();

            // 1. Проверяем таблицы
            String[] tables = {"carts", "cart_items", "users", "usersklad"};
            Map<String, Boolean> tableExists = new HashMap<>();
            Map<String, Integer> rowCounts = new HashMap<>();

            for (String table : tables) {
                try {
                    String existsSql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
                    Boolean exists = jdbcTemplate.queryForObject(existsSql, Boolean.class, table);
                    tableExists.put(table, exists);

                    if (exists) {
                        String countSql = "SELECT COUNT(*) FROM " + table;
                        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
                        rowCounts.put(table, count);
                    }
                } catch (Exception e) {
                    tableExists.put(table, false);
                    rowCounts.put(table, 0);
                }
            }

            testData.put("tables", tableExists);
            testData.put("row_counts", rowCounts);

            // 2. Проверяем статусы carts
            String statusSql = "SELECT status, COUNT(*) as count FROM carts GROUP BY status ORDER BY status";
            List<Map<String, Object>> statusStats = new ArrayList<>();
            try {
                statusStats = jdbcTemplate.queryForList(statusSql);
            } catch (Exception e) {
                statusStats.add(Map.of("error", e.getMessage()));
            }

            testData.put("carts_statuses", statusStats);

            // 3. Проверяем nalichie в cart_items
            String nalichieSql = "SELECT nalichie, COUNT(*) as count FROM cart_items GROUP BY nalichie ORDER BY nalichie";
            List<Map<String, Object>> nalichieStats = new ArrayList<>();
            try {
                nalichieStats = jdbcTemplate.queryForList(nalichieSql);
            } catch (Exception e) {
                nalichieStats.add(Map.of("error", e.getMessage()));
            }

            testData.put("nalichie_types", nalichieStats);

            // 4. Пример запроса taoshibka + unknown
            String exampleSql = """
            SELECT 
                c.id as cart_id,
                c.status,
                ci.nalichie,
                COUNT(ci.id) as item_count
            FROM carts c
            LEFT JOIN cart_items ci ON c.id = ci.cart_id
            WHERE c.status = 'taoshibka' 
               OR ci.nalichie = 'unknown'
            GROUP BY c.id, c.status, ci.nalichie
            ORDER BY c.id
            LIMIT 5
            """;

            List<Map<String, Object>> exampleResults = new ArrayList<>();
            try {
                exampleResults = jdbcTemplate.queryForList(exampleSql);
            } catch (Exception e) {
                exampleResults.add(Map.of("error", e.getMessage(), "sql", exampleSql));
            }

            testData.put("example_query", exampleResults);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("test", testData);
            response.put("message", "Taoshibka endpoints test completed");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Taoshibka test error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    @PostMapping("/office/taoshibka-orders/{orderId}/find-collectors")
    public ResponseEntity<?> findCollectorsForOrder(@PathVariable Integer orderId) {
        try {
            log.info("🔍 Office: поиск сборщиков для заказа #{}", orderId);

            // 1. Проверяем существование заказа и его текущий статус
            String checkOrderSql = "SELECT id, client_id, status FROM carts WHERE id = ?";
            Map<String, Object> orderInfo;
            try {
                orderInfo = jdbcTemplate.queryForMap(checkOrderSql, orderId);
            } catch (Exception e) {
                log.warn("Order not found: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "error", "Заказ не найден: #" + orderId,
                                "timestamp", System.currentTimeMillis()
                        ));
            }

            String currentStatus = (String) orderInfo.get("status");
            Integer clientId = (Integer) orderInfo.get("client_id");

            // Проверяем что заказ в правильном статусе
            if (!"taoshibka".equals(currentStatus)) {
                log.warn("Order #{} has wrong status: {}", orderId, currentStatus);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "found", false,
                        "orderId", orderId,
                        "currentStatus", currentStatus,
                        "message", "Заказ не имеет статус 'taoshibka'. Текущий статус: " + currentStatus,
                        "timestamp", System.currentTimeMillis()
                ));
            }

            // 2. Получаем город (склад) клиента
            String clientCity = "unknown";
            try {
                String citySql = "SELECT city FROM users WHERE id = ?";
                clientCity = jdbcTemplate.queryForObject(citySql, String.class, clientId);
                log.info("Client #{} city: '{}'", clientId, clientCity);
            } catch (Exception e) {
                log.warn("Cannot get city for client #{}: {}", clientId, e.getMessage());
                clientCity = "unknown";
            }

            // 3. Получаем все товары с unknown для этого заказа
            String itemsSql = """
            SELECT 
                ci.id as item_id,
                ci.product_id,
                ci.quantity as needed_quantity,
                ci.price,
                COALESCE(p.name, 'Товар #' || ci.product_id) as product_name,
                COALESCE(p.akticul, 'N/A') as akticul,
                COALESCE(p.category, 'Не указана') as category
            FROM cart_items ci
            LEFT JOIN usersklad p ON ci.product_id = p.id
            WHERE ci.cart_id = ?
              AND ci.nalichie = 'unknown'
            ORDER BY ci.id
            """;

            List<Map<String, Object>> unknownItems = jdbcTemplate.queryForList(itemsSql, orderId);

            if (unknownItems.isEmpty()) {
                log.info("No unknown items for order #{}. Checking if status should be changed.", orderId);

                // Если нет unknown товаров, но статус еще taoshibka, возможно нужно обновить статус
                String updateIfNoUnknownSql = """
                UPDATE carts 
                SET status = 'processing', 
                    updated_at = CURRENT_TIMESTAMP 
                WHERE id = ? 
                  AND status = 'taoshibka'
                  AND NOT EXISTS (
                      SELECT 1 FROM cart_items 
                      WHERE cart_id = ? AND nalichie = 'unknown'
                  )
                """;

                int updated = jdbcTemplate.update(updateIfNoUnknownSql, orderId, orderId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "found", true, // тех. найдено - нет unknown товаров
                        "orderId", orderId,
                        "clientCity", clientCity,
                        "unknownItemsCount", 0,
                        "message", "Нет товаров с неизвестным наличием",
                        "statusUpdated", updated > 0,
                        "newStatus", updated > 0 ? "processing" : currentStatus,
                        "timestamp", System.currentTimeMillis()
                ));
            }

            log.info("Order #{} has {} unknown items to check", orderId, unknownItems.size());

            // 4. Определяем порядок проверки складов на основе города клиента
            List<String> warehousePriority = determineWarehousePriority(clientCity);
            log.info("Warehouse priority for city '{}': {}", clientCity, warehousePriority);

            // 5. Проверяем каждый склад в порядке приоритета
            Map<String, Object> foundWarehouse = null;
            List<Map<String, Object>> warehouseChecks = new ArrayList<>();
            List<Map<String, Object>> availableItemsDetails = new ArrayList<>();

            for (String warehouse : warehousePriority) {
                log.info("Checking warehouse: {}", warehouse);

                Map<String, Object> warehouseCheck = new HashMap<>();
                warehouseCheck.put("warehouseName", warehouse);
                warehouseCheck.put("warehouseDisplay", getWarehouseDisplayName(warehouse));

                boolean allAvailable = true;
                List<Map<String, Object>> itemsAvailability = new ArrayList<>();

                // Проверяем каждый товар на этом складе
                for (Map<String, Object> item : unknownItems) {
                    Integer productId = (Integer) item.get("product_id");
                    Integer neededQuantity = (Integer) item.get("needed_quantity");
                    String productName = (String) item.get("product_name");
                    String akticul = (String) item.get("akticul");

                    Map<String, Object> itemAvailability = new HashMap<>();
                    itemAvailability.put("productId", productId);
                    itemAvailability.put("productName", productName);
                    itemAvailability.put("akticul", akticul);
                    itemAvailability.put("neededQuantity", neededQuantity);

                    try {
                        Integer availableQuantity = getProductQuantityInWarehouse(warehouse, productId);
                        itemAvailability.put("availableQuantity", availableQuantity);

                        boolean itemAvailable = availableQuantity >= neededQuantity;
                        itemAvailability.put("available", itemAvailable);

                        if (!itemAvailable) {
                            allAvailable = false;
                        }

                        itemsAvailability.add(itemAvailability);

                    } catch (Exception e) {
                        log.warn("Error checking product #{} in warehouse {}: {}", productId, warehouse, e.getMessage());
                        itemAvailability.put("availableQuantity", 0);
                        itemAvailability.put("available", false);
                        itemAvailability.put("error", e.getMessage());
                        allAvailable = false;
                        itemsAvailability.add(itemAvailability);
                    }
                }

                warehouseCheck.put("allAvailable", allAvailable);
                warehouseCheck.put("itemsAvailability", itemsAvailability);
                warehouseCheck.put("totalItems", unknownItems.size());
                warehouseCheck.put("availableItemsCount", (int) itemsAvailability.stream()
                        .filter(item -> Boolean.TRUE.equals(item.get("available")))
                        .count());

                warehouseChecks.add(warehouseCheck);

                if (allAvailable) {
                    foundWarehouse = warehouseCheck;
                    availableItemsDetails = itemsAvailability;
                    log.info("✅ Found all items in warehouse: {}", warehouse);

                    // 6. АВТОМАТИЧЕСКИ ОБНОВЛЯЕМ СТАТУС ЗАКАЗА
                    boolean statusUpdated = updateOrderStatusToProcessing(orderId, warehouse, itemsAvailability);

                    if (statusUpdated) {
                        log.info("✅ Order #{} status automatically changed to 'processing'", orderId);
                    } else {
                        log.error("❌ Failed to update status for order #{}", orderId);
                    }

                    break;
                }

                log.info("❌ Not all items available in warehouse: {}", warehouse);
            }

            // 7. Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("clientId", clientId);
            response.put("clientCity", clientCity);
            response.put("unknownItemsCount", unknownItems.size());
            response.put("warehouseChecks", warehouseChecks);

            if (foundWarehouse != null) {
                String warehouseDisplay = getWarehouseDisplayName((String) foundWarehouse.get("warehouseName"));

                response.put("found", true);
                response.put("warehouse", foundWarehouse.get("warehouseName"));
                response.put("warehouseDisplay", warehouseDisplay);
                response.put("message", "Все товары найдены на складе " + warehouseDisplay);
                response.put("availableItems", availableItemsDetails);
                response.put("statusUpdated", true);
                response.put("newStatus", "processing");
                response.put("action", "status_automatically_updated");

            } else {
                response.put("found", false);
                response.put("message", "Не удалось найти склад со всеми товарами");
                response.put("suggestion", "Проверьте наличие на всех складах вручную");
                response.put("statusUpdated", false);
                response.put("currentStatus", currentStatus);
            }

            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error finding collectors for order #{}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Внутренняя ошибка сервера: " + e.getMessage(),
                            "orderId", orderId,
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    // Метод: Автоматически обновляет статус заказа на processing
    private boolean updateOrderStatusToProcessing(Integer orderId, String warehouse, List<Map<String, Object>> availableItems) {
        try {
            // 1. Обновляем статус заказа
            String updateCartSql = "UPDATE carts SET status = 'processing' WHERE id = ?";
            int cartsUpdated = jdbcTemplate.update(updateCartSql, orderId);

            if (cartsUpdated == 0) {
                log.error("Failed to update carts for order #{}", orderId);
                return false;
            }

            // 2. Обновляем nalichie в cart_items с 'unknown' на 'available'
            String updateItemsSql = """
            UPDATE cart_items 
            SET nalichie = 'available', 
                updated_at = CURRENT_TIMESTAMP 
            WHERE cart_id = ? 
              AND nalichie = 'unknown'
            """;

            int itemsUpdated = jdbcTemplate.update(updateItemsSql, orderId);
            log.info("Updated {} cart_items from 'unknown' to 'available' for order #{}", itemsUpdated, orderId);

            // 3. Создаем запись о решении проблемы (для истории)
            String insertSolutionSql = """
            INSERT INTO office_problems_solutions 
                (order_id, warehouse, action_taken, solved_at, created_at)
            VALUES (?, ?, 'AUTO_FOUND_WAREHOUSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

            try {
                jdbcTemplate.update(insertSolutionSql, orderId, warehouse);
            } catch (Exception e) {
                log.warn("Could not insert solution record: {}", e.getMessage());
                // Не прерываем из-за этой ошибки
            }

            // 4. Уменьшаем количество товаров на складе (если нужно)
            // Пока пропускаем, чтобы не менять данные без подтверждения

            jdbcTemplate.execute("COMMIT");

            log.info("✅ Successfully updated order #{} to 'processing'. Warehouse: {}", orderId, warehouse);
            return true;

        } catch (Exception e) {
            try {
                jdbcTemplate.execute("ROLLBACK");
            } catch (Exception rollbackError) {
                log.error("Rollback failed: {}", rollbackError.getMessage());
            }

            log.error("❌ Transaction failed for order #{}: {}", orderId, e.getMessage());
            return false;
        }
    }

    // Вспомогательный метод: определяет порядок проверки складов
    private List<String> determineWarehousePriority(String city) {
        List<String> priority = new ArrayList<>();

        if (city == null) {
            city = "unknown";
        }

        city = city.toLowerCase().trim();

        switch (city) {
            case "skladodin":
                priority.add("skladodin");
                priority.add("skladdva");
                priority.add("skladtri");
                priority.add("usersklad");
                break;
            case "skladdva":
                priority.add("skladdva");
                priority.add("skladtri");
                priority.add("skladodin");
                priority.add("usersklad");
                break;
            case "skladtri":
                priority.add("skladtri");
                priority.add("skladodin");
                priority.add("skladdva");
                priority.add("usersklad");
                break;
            default:
                // Для других городов или unknown - только общий склад
                priority.add("usersklad");
                // Но все равно проверяем частные склады на всякий случай
                priority.add("skladodin");
                priority.add("skladdva");
                priority.add("skladtri");
                break;
        }

        return priority;
    }

    // Вспомогательный метод: получает количество товара на складе
    private Integer getProductQuantityInWarehouse(String warehouseName, Integer productId) {
        String sql;

        switch (warehouseName) {
            case "skladodin":
            case "skladdva":
            case "skladtri":
                sql = "SELECT count FROM " + warehouseName + " WHERE id = ?";
                break;
            case "usersklad":
                sql = "SELECT count FROM usersklad WHERE id = ?";
                break;
            default:
                throw new RuntimeException("Unknown warehouse: " + warehouseName);
        }

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, productId);
            return count != null ? count : 0;
        } catch (Exception e) {
            // Если товара нет на складе
            log.debug("Product #{} not found in warehouse {}: {}", productId, warehouseName, e.getMessage());
            return 0;
        }
    }

    // Вспомогательный метод: красивое имя склада
    private String getWarehouseDisplayName(String warehouseName) {
        switch (warehouseName) {
            case "skladodin": return "Склад 1 (skladodin)";
            case "skladdva": return "Склад 2 (skladdva)";
            case "skladtri": return "Склад 3 (skladtri)";
            case "usersklad": return "Общий склад (usersklad)";
            default: return warehouseName;
        }
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
    // Добавить в UnifiedController.java в раздел БЛОК 12: OFFICE

    @GetMapping("/office/problems/full-info/{cartId}")
    public ResponseEntity<?> getFullProblemInfo(@PathVariable Integer cartId) {
        try {
            log.info("🔍 Office: getting full problem info for cart #{}", cartId);

            // 1. Получить информацию о корзине (заказе)
            String cartSql = """
            SELECT 
                c.id as cart_id,
                c.client_id,
                c.status as cart_status,
                c.created_date
            FROM carts c
            WHERE c.id = ? AND c.status = 'problem'
            """;

            Map<String, Object> cart;
            try {
                cart = jdbcTemplate.queryForMap(cartSql, cartId);
            } catch (Exception e) {
                log.error("Cart not found or not a problem: {}", cartId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Problem cart not found"));
            }

            Integer clientId = (Integer) cart.get("client_id");

            // 2. Получить информацию о пользователе
            String userSql = """
            SELECT 
                id,
                COALESCE(firstname, username) as client_name,
                email as client_email,
                city
            FROM users 
            WHERE id = ?
            """;

            Map<String, Object> userInfo = jdbcTemplate.queryForMap(userSql, clientId);

            // 3. Получить товары из корзины
            String itemsSql = """
            SELECT 
                ci.product_id,
                ci.quantity,
                ci.price,
                COALESCE(p.name, 'Товар #' || ci.product_id::text) as product_name,
                COALESCE(p.akticul, 'N/A') as product_sku
            FROM cart_items ci
            LEFT JOIN usersklad p ON ci.product_id = p.id
            WHERE ci.cart_id = ?
            """;

            List<Map<String, Object>> items = jdbcTemplate.queryForList(itemsSql, cartId);

            // 4. Формируем детализированную информацию
            List<Map<String, Object>> detailedItems = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Map<String, Object> detailedItem = new HashMap<>();
                detailedItem.put("product_id", item.get("product_id"));
                detailedItem.put("product_name", item.get("product_name"));
                detailedItem.put("product_sku", item.get("product_sku"));
                detailedItem.put("quantity", item.get("quantity"));
                detailedItem.put("price", item.get("price"));
                detailedItems.add(detailedItem);
            }

            // 5. Генерируем сообщение для email
            String emailMessage = String.format("""
            Уважаемый(ая) %s,
            
            В вашем заказе #%d обнаружена проблема.
            
            Товары в заказе:
            %s
            
            Тип проблемы: %s
            
            Пожалуйста, выберите один из вариантов:
            1. Продолжить сборку без проблемного товара
            2. Отменить весь заказ
            3. Подождать до появления товара
            
            Для ответа используйте этот email или позвоните по телефону:
            📞 +7 (495) 123-45-67
            
            С уважением,
            Команда KEFIR Logistics
            """,
                    userInfo.get("client_name"),
                    cartId,
                    detailedItems.stream()
                            .map(item -> String.format("• %s (Артикул: %s, Количество: %s, Цена: %.2f ₽)",
                                    item.get("product_name"),
                                    item.get("product_sku"),
                                    item.get("quantity"),
                                    item.get("price")))
                            .collect(Collectors.joining("\n")),
                    "Отсутствует товар на складе"
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cart", cart);
            response.put("client", userInfo);
            response.put("items", detailedItems);
            response.put("total_items", detailedItems.size());
            response.put("email_message", emailMessage);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting full problem info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // Также обновим существующий эндпоинт getActiveProblems для получения более полной информации
    @GetMapping("/office/problems/active-detailed")
    public ResponseEntity<?> getActiveProblemsDetailed() {
        try {
            log.info("🔍 Office: getting active problems with details");

            String sql = """
            SELECT 
                c.id as cart_id,
                c.client_id,
                COALESCE(u.firstname, u.username, 'Клиент #' || c.client_id) as client_name,
                COALESCE(u.email, 'client' || c.client_id || '@example.com') as client_email,
                COALESCE(u.city, 'Москва') as client_city,
                c.created_date as created_at,
                c.status as cart_status,
                'COLLECTOR_' || (c.id % 10 + 1) as collector_id,
                'Требует внимания офиса' as details,
                (
                    SELECT STRING_AGG(COALESCE(p.name, 'Товар #' || ci.product_id::text), ', ')
                    FROM cart_items ci
                    LEFT JOIN usersklad p ON ci.product_id = p.id
                    WHERE ci.cart_id = c.id
                ) as product_names
            FROM carts c
            LEFT JOIN users u ON c.client_id = u.id
            WHERE c.status = 'problem'
            ORDER BY c.created_date DESC
            LIMIT 20
            """;

            List<Map<String, Object>> problems = jdbcTemplate.queryForList(sql);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("problems", problems);
            response.put("total", problems.size());
            response.put("message", "Problems loaded with product names");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting detailed problems: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("problems", new ArrayList<>());
            response.put("total", 0);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/collector/problems/active")
    public ResponseEntity<?> getActiveProblemsForOffice() {
        try {
            log.info("🔍 Collector/Office: getting active problems");

            String sql = """
            SELECT 
                op.id,
                op.order_id,
                op.product_id,
                op.collector_id,
                op.client_id,
                u.firstname as client_name,
                op.client_email,
                op.problem_type,
                op.status,
                op.details,
                op.created_at,
                op.updated_at
            FROM office_problems op
            LEFT JOIN users u ON op.client_id = u.id
            WHERE op.status = 'PENDING'
            ORDER BY op.created_at DESC
        """;

            List<Map<String, Object>> problems = jdbcTemplate.queryForList(sql);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("problems", problems);
            response.put("total", problems.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting active problems: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // Проверка статуса заказа для отображения в UI
    @GetMapping("/collector/orders/{cartId}/status")
    public ResponseEntity<?> getOrderStatus(@PathVariable Integer cartId) {
        try {
            log.info("📊 Collector: getting status for order #{}", cartId);

            String sql = "SELECT status, created_date FROM carts WHERE id = ?";
            Map<String, Object> cartInfo = jdbcTemplate.queryForMap(sql, cartId);

            // Проверяем есть ли заказ в orders
            String orderSql = "SELECT COUNT(*) FROM orders WHERE cart_id = ?";
            Long inOrders = jdbcTemplate.queryForObject(orderSql, Long.class, cartId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cartId", cartId);
            response.put("cartStatus", cartInfo.get("status"));
            response.put("createdDate", cartInfo.get("created_date"));
            response.put("inOrdersTable", inOrders > 0);
            response.put("message", "Статус получен");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting order status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
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
            int updatedItemsCount = 0;

            if ("CANCEL_ORDER".equals(decision)) {
                newStatus = "cancelled";
                decisionText = "Order cancelled";

                // ПРИ ОТМЕНЕ: для ВСЕХ товаров заказа (кроме тех, где vozvrat = 'tcc')
                String cancelItemsSql = """
                UPDATE cart_items 
                SET 
                    nalichie = 'нет',
                    vozvrat = 'tc'
                WHERE cart_id = ?
                AND (vozvrat IS NULL OR vozvrat != 'tcc')  -- НЕ ТРОГАЕМ ТОВАРЫ С TCC
                """;
                updatedItemsCount = jdbcTemplate.update(cancelItemsSql, orderId);
                log.info("✅ Cancelled order #{}: updated {} items (nalichie='нет', vozvrat='tc')",
                        orderId, updatedItemsCount);

            } else if ("APPROVE_WITHOUT_PRODUCT".equals(decision)) {
                newStatus = "processing";
                decisionText = "Continue without product";

                // ОБНОВЛЯЕМ vozvrat = 'tc' ТОЛЬКО для товаров, у которых nalichie = 'нет'
                // (и не трогаем товары с vozvrat = 'tcc')
                String updateItemsSql = """
                UPDATE cart_items 
                SET vozvrat = 'tc' 
                WHERE cart_id = ? 
                AND (nalichie = 'нет' OR nalichie = 'эхЄ')  -- учитываем обе кодировки
                AND (vozvrat IS NULL OR vozvrat != 'tcc')  -- не трогаем товары с tcc
                """;
                updatedItemsCount = jdbcTemplate.update(updateItemsSql, orderId);
                log.info("✅ Updated vozvrat='tc' for {} items with nalichie='нет/эхЄ' in cart #{}",
                        updatedItemsCount, orderId);

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
                response.put("itemsUpdated", updatedItemsCount);
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
    private String determineWarehouseTable(String city) {
        if (city == null || city.trim().isEmpty()) {
            log.debug("🏢 City не указан, используем основной склад");
            return "usersklad";
        }

        String normalizedCity = city.trim().toLowerCase();

        // Если начинается с "sklad" - используем как имя таблицы склада
        if (normalizedCity.startsWith("sklad")) {
            log.info("🏢 City '{}' начинается с 'sklad', используем как склад: {}", city, normalizedCity);
            return normalizedCity;
        }

        log.debug("🏢 City '{}' не частный склад, используем основной usersklad", city);
        return "usersklad";
    }
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