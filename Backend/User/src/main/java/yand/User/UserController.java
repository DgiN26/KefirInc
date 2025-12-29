package yand.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/clients")
public class UserController {

    @Autowired
    private UserRep clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // GET всех клиентов
    @GetMapping
    public List<User> getAllClients() {
        return clientRepository.findAll();
    }

    // GET по ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getClient(@PathVariable int id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Проверка доступности email
    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "available", false,
                                "message", "Email не указан"
                        ));
            }

            // Проверка формата email
            if (!email.contains("@")) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "available", false,
                        "message", "Некорректный формат email",
                        "valid", false
                ));
            }

            // Проверка существования в БД
            boolean exists = clientRepository.findByEmail(email).isPresent();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "available", !exists,
                    "exists", exists,
                    "message", exists ? "Email уже используется" : "Email свободен",
                    "valid", true
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "available", false,
                            "message", "Ошибка при проверке email"
                    ));
        }
    }

    // Проверка доступности username
    @PostMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "available", false,
                                "message", "Логин не указан"
                        ));
            }

            // Проверка минимальной длины
            if (username.length() < 3) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "available", false,
                        "message", "Логин должен быть не менее 3 символов",
                        "valid", false
                ));
            }

            // Проверка существования в БД
            boolean exists = clientRepository.findByUsername(username).isPresent();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "available", !exists,
                    "exists", exists,
                    "message", exists ? "Логин уже занят" : "Логин свободен",
                    "valid", true
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "available", false,
                            "message", "Ошибка при проверке логина"
                    ));
        }
    }

    // Единый endpoint для проверки всех полей
    @PostMapping("/validate")
    public ResponseEntity<?> validateFields(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);

            String email = request.get("email");
            String username = request.get("username");

            // Проверка email
            if (email != null) {
                boolean emailExists = false;
                boolean emailValid = email.contains("@");

                if (emailValid) {
                    emailExists = clientRepository.findByEmail(email).isPresent();
                }

                result.put("email", Map.of(
                        "available", !emailExists && emailValid,
                        "exists", emailExists,
                        "valid", emailValid,
                        "message", emailValid ?
                                (emailExists ? "Email уже используется" : "Email свободен") :
                                "Некорректный формат email"
                ));
            }

            // Проверка username
            if (username != null) {
                boolean usernameExists = false;
                boolean usernameValid = username.length() >= 3;

                if (usernameValid) {
                    usernameExists = clientRepository.findByUsername(username).isPresent();
                }

                result.put("username", Map.of(
                        "available", !usernameExists && usernameValid,
                        "exists", usernameExists,
                        "valid", usernameValid,
                        "message", usernameValid ?
                                (usernameExists ? "Логин уже занят" : "Логин свободен") :
                                "Логин должен быть не менее 3 символов"
                ));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Ошибка валидации"
                    ));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createClient(@RequestBody User client) {
        try {
            // 1. Проверка обязательных полей
            if (client.getUsername() == null || client.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username is required"));
            }

            if (client.getPassword() == null || client.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password is required"));
            }

            // 2. Проверка уникальности username
            if (clientRepository.findByUsername(client.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Username already exists"));
            }

            // 3. Хеширование пароля
            String encodedPassword = passwordEncoder.encode(client.getPassword());
            client.setPassword(encodedPassword);

            // 4. Установка дефолтных значений
            if (client.getStatus() == null) {
                client.setStatus("active");
            }

            if (client.getRole() == null) {
                client.setRole("client");
            }

            // 5. Сохранение
            User savedClient = clientRepository.save(client);

            // 6. Убираем пароль из ответа
            savedClient.setPassword(null);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "User created successfully",
                            "user", savedClient
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    // PUT - обновление клиента (ИСПРАВЛЕННЫЙ)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateClient(@PathVariable int id, @RequestBody User clientDetails) {
        return clientRepository.findById(id)
                .map(client -> {
                    // Обновляем поля
                    if (clientDetails.getUsername() != null) {
                        client.setUsername(clientDetails.getUsername());
                    }

                    if (clientDetails.getEmail() != null) {
                        client.setEmail(clientDetails.getEmail());
                    }

                    if (clientDetails.getFirstname() != null) {
                        client.setFirstname(clientDetails.getFirstname());
                    }

                    if (clientDetails.getCity() != null) {
                        client.setCity(clientDetails.getCity());
                    }

                    if (clientDetails.getRole() != null) {
                        client.setRole(clientDetails.getRole());
                    }

                    if (clientDetails.getStatus() != null) {
                        client.setStatus(clientDetails.getStatus());
                    }

                    // ВАЖНО: Обновляем пароль, если он предоставлен
                    if (clientDetails.getPassword() != null &&
                            !clientDetails.getPassword().isEmpty()) {
                        // Хешируем новый пароль
                        String encodedPassword = passwordEncoder.encode(clientDetails.getPassword());
                        client.setPassword(encodedPassword);
                    }

                    User updated = clientRepository.save(client);
                    updated.setPassword(null); // Убираем пароль из ответа
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable int id) {
        return clientRepository.findById(id)
                .map(client -> {
                    clientRepository.delete(client);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> userData) {
        try {
            System.out.println("=== USER SERVICE DEBUG ===");
            System.out.println("Получен JSON: " + new ObjectMapper().writeValueAsString(userData));

            // 1. Извлечение данных
            String username = (String) userData.get("username");
            String password = (String) userData.get("password");
            String email = (String) userData.get("email");
            String firstname = (String) userData.get("firstname");

            System.out.println("Извлечено:");
            System.out.println("  username: " + username);
            System.out.println("  password: " + (password != null ? "[PRESENT]" : "null"));
            System.out.println("  email: " + email);
            System.out.println("  firstname: " + firstname);

            // 2. Проверяем кодировку пароля
            if (password != null) {
                System.out.println("  Длина пароля: " + password.length());
                System.out.println("  Первые 10 chars пароля: " + password.substring(0, Math.min(10, password.length())));
                try {
                    System.out.println("  Пароль в bytes: " + Arrays.toString(password.getBytes("UTF-8")));
                } catch (Exception e) {
                    System.out.println("  Ошибка кодировки пароля: " + e.getMessage());
                }
            }

            // 3. Проверка уникальности
            System.out.println("Проверяем уникальность username...");
            boolean usernameExists = clientRepository.findByUsername(username).isPresent();
            System.out.println("  username существует: " + usernameExists);

            System.out.println("Проверяем уникальность email...");
            boolean emailExists = clientRepository.findByEmail(email).isPresent();
            System.out.println("  email существует: " + emailExists);

            // 4. Создаем пользователя
            User user = new User();
            user.setUsername(username);
            user.setFirstname(firstname);
            user.setEmail(email);

            System.out.println("Перед хешированием пароля...");
            String encodedPassword = passwordEncoder.encode(password);
            System.out.println("  Пароль захеширован, длина хеша: " + encodedPassword.length());
            user.setPassword(encodedPassword);

            user.setRole("client");
            user.setStatus("active");

            // 5. Сохраняем
            System.out.println("Сохраняем в БД...");
            User savedUser = clientRepository.save(user);
            System.out.println("✅ Сохранено! ID: " + savedUser.getId());
            System.out.println("  Автогенерация ID: " + savedUser.getId());

            // 6. Сразу проверим, что сохранено
            System.out.println("Проверяем сохранение...");
            Optional<User> verified = clientRepository.findById(savedUser.getId());
            if (verified.isPresent()) {
                User dbUser = verified.get();
                System.out.println("✅ Найдено в БД:");
                System.out.println("  ID: " + dbUser.getId());
                System.out.println("  Username: " + dbUser.getUsername());
                System.out.println("  Email: " + dbUser.getEmail());
                System.out.println("  Firstname: " + dbUser.getFirstname());
                System.out.println("  Role: " + dbUser.getRole());
                System.out.println("  Status: " + dbUser.getStatus());
                System.out.println("  CreatedAt: " + dbUser.getCreatedAt());
            } else {
                System.out.println("❌ Не найдено в БД после сохранения!");
            }

            // 7. Подготовка ответа
            System.out.println("Подготавливаем ответ...");
            savedUser.setPassword(null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Регистрация успешна");
            response.put("user", Map.of(
                    "id", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "email", savedUser.getEmail(),
                    "firstname", savedUser.getFirstname(),
                    "role", savedUser.getRole(),
                    "status", savedUser.getStatus(),
                    "createdAt", savedUser.getCreatedAt()
            ));

            System.out.println("✅ Отправляем ответ: " + response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("❌ ИСКЛЮЧЕНИЕ в UserService:");
            System.err.println("  Тип: " + e.getClass().getName());
            System.err.println("  Сообщение: " + e.getMessage());
            System.err.println("  Причина: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));

            // Выводим полный stack trace
            e.printStackTrace();

            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = "Unknown error";
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Ошибка сервера при регистрации: " + errorMsg
                    ));
        }
    }
}