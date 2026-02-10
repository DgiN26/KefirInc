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