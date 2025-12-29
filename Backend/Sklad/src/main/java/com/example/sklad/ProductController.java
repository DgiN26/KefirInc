package com.example.sklad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private SkladRep productRepository;

    // ==================== CRUD ДЛЯ ПРОДУКТОВ ====================

    /**
     * Создать новый продукт
     */
    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> productData) {
        try {
            log.info("Создание нового товара из данных: {}", productData);

            // Преобразуем Map в UserSkld
            UserSkld product = new UserSkld();

            // УБЕДИТЕСЬ, ЧТО ВСЕ toString() ВЫЗЫВАЮТСЯ ТОЛЬКО ПОСЛЕ ПРОВЕРКИ НА NULL

            if (productData.containsKey("name")) {
                Object nameObj = productData.get("name");
                if (nameObj != null) {
                    product.setName(nameObj.toString());
                } else {
                    return ResponseEntity.badRequest()
                            .body("Название товара не может быть null");
                }
            } else {
                return ResponseEntity.badRequest()
                        .body("Название товара обязательно");
            }

            if (productData.containsKey("price")) {
                Object priceObj = productData.get("price");
                if (priceObj != null) {
                    try {
                        product.setPrice(Double.parseDouble(priceObj.toString()));
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest()
                                .body("Цена должна быть числом");
                    }
                } else {
                    return ResponseEntity.badRequest()
                            .body("Цена не может быть null");
                }
            } else {
                return ResponseEntity.badRequest()
                        .body("Цена обязательна");
            }

            if (productData.containsKey("count")) {
                Object countObj = productData.get("count");
                if (countObj != null) {
                    try {
                        product.setCount(Integer.parseInt(countObj.toString()));
                    } catch (NumberFormatException e) {
                        product.setCount(0); // Значение по умолчанию
                    }
                } else {
                    product.setCount(0); // Если значение null, устанавливаем 0
                }
            } else {
                product.setCount(0);
            }

            if (productData.containsKey("category")) {
                Object categoryObj = productData.get("category");
                if (categoryObj != null) {
                    product.setCategory(categoryObj.toString());
                } else {
                    return ResponseEntity.badRequest()
                            .body("Категория не может быть null");
                }
            } else {
                return ResponseEntity.badRequest()
                        .body("Категория обязательна");
            }

            // Для необязательных полей используем проверку на null
            if (productData.containsKey("akticul")) {
                Object akticulObj = productData.get("akticul");
                if (akticulObj != null) {
                    product.setAkticul(akticulObj.toString());
                }
                // Если null - оставляем поле пустым (null)
            }

            if (productData.containsKey("description")) {
                Object descriptionObj = productData.get("description");
                if (descriptionObj != null) {
                    product.setDescription(descriptionObj.toString());
                }
            }

            if (productData.containsKey("supplier")) {
                Object supplierObj = productData.get("supplier");
                if (supplierObj != null) {
                    product.setSupplier(supplierObj.toString());
                }
            }

            // Проверка уникальности артикула
            if (product.getAkticul() != null && !product.getAkticul().trim().isEmpty()) {
                Optional<UserSkld> existing = productRepository.findByAkticul(product.getAkticul());
                if (existing.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body("Товар с таким артикулом уже существует");
                }
            }

            // Сохраняем
            UserSkld savedProduct = productRepository.save(product);
            log.info("Товар создан: ID {}", savedProduct.getId());

            // Возвращаем Map
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedProduct.getId());
            response.put("name", savedProduct.getName());
            response.put("price", savedProduct.getPrice());
            response.put("count", savedProduct.getCount());
            response.put("category", savedProduct.getCategory());
            response.put("akticul", savedProduct.getAkticul() != null ? savedProduct.getAkticul() : "");
            response.put("description", savedProduct.getDescription() != null ? savedProduct.getDescription() : "");
            response.put("supplier", savedProduct.getSupplier() != null ? savedProduct.getSupplier() : "");
            response.put("createdAt", savedProduct.getCreatedAt());
            response.put("updatedAt", savedProduct.getUpdatedAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Ошибка при создании товара: {}", e.getMessage(), e);

            // Более информативное сообщение об ошибке
            String errorMessage;
            if (e instanceof NullPointerException) {
                errorMessage = "NullPointerException: Одно из обязательных полей имеет значение null";
            } else {
                errorMessage = e.getMessage();
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Ошибка при создании товара",
                            "message", errorMessage,
                            "type", e.getClass().getSimpleName()
                    ));
        }
    }
    /**
     * Обновить продукт
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @RequestBody Map<String, Object> productData) {
        try {
            log.info("Обновление товара с ID: {}", id);

            Optional<UserSkld> existingProductOpt = productRepository.findById(id);
            if (existingProductOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Товар с id " + id + " не найден"));
            }

            UserSkld existingProduct = existingProductOpt.get();
            boolean hasChanges = false;

            // Обновление полей
            if (productData.containsKey("name") && productData.get("name") != null) {
                existingProduct.setName(productData.get("name").toString());
                hasChanges = true;
            }

            if (productData.containsKey("price") && productData.get("price") != null) {
                try {
                    existingProduct.setPrice(Double.parseDouble(productData.get("price").toString()));
                    hasChanges = true;
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Цена должна быть числом"));
                }
            }

            if (productData.containsKey("count") && productData.get("count") != null) {
                try {
                    existingProduct.setCount(Integer.parseInt(productData.get("count").toString()));
                    hasChanges = true;
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Количество должно быть целым числом"));
                }
            }

            if (productData.containsKey("category") && productData.get("category") != null) {
                existingProduct.setCategory(productData.get("category").toString());
                hasChanges = true;
            }

            if (productData.containsKey("akticul")) {
                existingProduct.setAkticul(productData.get("akticul") != null ?
                        productData.get("akticul").toString() : null);
                hasChanges = true;
            }

            if (productData.containsKey("description")) {
                existingProduct.setDescription(productData.get("description") != null ?
                        productData.get("description").toString() : null);
                hasChanges = true;
            }

            if (productData.containsKey("supplier")) {
                existingProduct.setSupplier(productData.get("supplier") != null ?
                        productData.get("supplier").toString() : null);
                hasChanges = true;
            }

            if (hasChanges) {
                existingProduct.setUpdatedAt(LocalDateTime.now());
                UserSkld updatedProduct = productRepository.save(existingProduct);
                log.info("Товар обновлен: ID {}", updatedProduct.getId());

                // Возвращаем Map
                Map<String, Object> response = new HashMap<>();
                response.put("id", updatedProduct.getId());
                response.put("name", updatedProduct.getName());
                response.put("price", updatedProduct.getPrice());
                response.put("count", updatedProduct.getCount());
                response.put("category", updatedProduct.getCategory());
                response.put("akticul", updatedProduct.getAkticul());
                response.put("description", updatedProduct.getDescription());
                response.put("supplier", updatedProduct.getSupplier());
                response.put("createdAt", updatedProduct.getCreatedAt());
                response.put("updatedAt", updatedProduct.getUpdatedAt());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of("message", "Нет изменений для обновления"));
            }

        } catch (Exception e) {
            log.error("Ошибка при обновлении товара: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при обновлении товара: " + e.getMessage()));
        }
    }

    /**
     * Получить все продукты (возвращает Map)
     */
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        try {
            log.info("Получение всех товаров");

            List<UserSkld> products = productRepository.findAll();
            List<Map<String, Object>> response = new ArrayList<>();

            for (UserSkld product : products) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", product.getId());
                productMap.put("name", product.getName());
                productMap.put("price", product.getPrice());
                productMap.put("count", product.getCount());
                productMap.put("category", product.getCategory());
                productMap.put("akticul", product.getAkticul());
                productMap.put("description", product.getDescription());
                productMap.put("supplier", product.getSupplier());
                productMap.put("createdAt", product.getCreatedAt());
                productMap.put("updatedAt", product.getUpdatedAt());
                response.add(productMap);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при получении товаров: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении товаров: " + e.getMessage()));
        }
    }

    /**
     * Получить продукт по ID (возвращает Map)
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Integer id) {
        try {
            log.info("Получение товара с ID: {}", id);

            Optional<UserSkld> productOpt = productRepository.findById(id);
            if (productOpt.isPresent()) {
                UserSkld product = productOpt.get();

                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", product.getId());
                productMap.put("name", product.getName());
                productMap.put("price", product.getPrice());
                productMap.put("count", product.getCount());
                productMap.put("category", product.getCategory());
                productMap.put("akticul", product.getAkticul());
                productMap.put("description", product.getDescription());
                productMap.put("supplier", product.getSupplier());
                productMap.put("createdAt", product.getCreatedAt());
                productMap.put("updatedAt", product.getUpdatedAt());

                return ResponseEntity.ok(productMap);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Товар с id " + id + " не найден"));
            }

        } catch (Exception e) {
            log.error("Ошибка при получении товара: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении товара: " + e.getMessage()));
        }
    }
}