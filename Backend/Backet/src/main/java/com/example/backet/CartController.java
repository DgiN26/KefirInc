package com.example.backet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository; // ← Добавить!

    @PostMapping("/client/{clientId}")
    public Cart createCart(@PathVariable int clientId) {
        Cart activeCart = cartRepository.findByClientIdAndStatus(clientId, "active");
        if (activeCart != null) {
            return activeCart;
        }
        Cart cart = new Cart(clientId, "active");
        return cartRepository.save(cart);
    }

    @PostMapping("/{cartId}/add")
    public CartItem addToCart(@PathVariable int cartId,
                              @RequestParam int productId,
                              @RequestParam int quantity,
                              @RequestParam double price) {
        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cartId, productId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            return cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem(cartId, productId, quantity, price);
            return cartItemRepository.save(newItem);
        }
    }

    @GetMapping("/client/{clientId}")
    public List<Cart> getClientCarts(@PathVariable int clientId) {
        return cartRepository.findByClientId(clientId);
    }

    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<Map<String, Object>> checkoutCart(@PathVariable int cartId) {
        try {
            // 1. Получить корзину
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new RuntimeException("Корзина не найдена: " + cartId));

            // 2. Получить товары из корзины
            List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

            // 3. Проверить, что корзина не пустая
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Корзина пуста");
            }

            // 4. Рассчитать общую сумму
            double totalAmount = cartItems.stream()
                    .mapToDouble(item -> item.getQuantity() * item.getPrice())
                    .sum();

            // 5. Обновить статус корзины
            cart.setStatus("completed");
            cartRepository.save(cart);

            // 6. СОХРАНИТЬ ЗАКАЗ В БД (НОВОЕ!)
            String orderNumber = "ORD-" + System.currentTimeMillis();
            Order order = new Order(cartId, orderNumber, totalAmount, "CREATED");
            Order savedOrder = orderRepository.save(order); // ← Сохраняем в БД

            // 7. Создать ответ
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderNumber);
            response.put("cartId", cartId);
            response.put("status", "CREATED");
            response.put("totalAmount", totalAmount);
            response.put("message", "Заказ успешно оформлен");
            response.put("itemsCount", cartItems.size());
            response.put("timestamp", new Date());
            response.put("dbOrderId", savedOrder.getId()); // ← Добавить ID из БД

            // 8. Добавить информацию о товарах
            List<Map<String, Object>> itemsResponse = new ArrayList<>();
            for (CartItem item : cartItems) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productId", item.getProductId());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("price", item.getPrice());
                itemsResponse.add(itemMap);
            }
            response.put("items", itemsResponse);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", new Date());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 9. Добавить метод для получения сохраненных заказов
    @GetMapping("/orders/{cartId}")
    public ResponseEntity<?> getOrdersByCart(@PathVariable int cartId) {
        try {
            List<Order> orders = orderRepository.findByCartId(cartId);
            if (orders.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Заказы не найдены", "cartId", cartId));
            }
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // CartController.java в cart-service (8085) - если его нет
    @GetMapping("/{cartId}/items")
    public List<Map<String, Object>> getCartItems(@PathVariable int cartId) {
        List<CartItem> items = cartItemRepository.findByCartId(cartId);

        return items.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("cartId", item.getCartId());
            map.put("productId", item.getProductId());
            map.put("quantity", item.getQuantity());
            map.put("price", item.getPrice());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber);
            if (order == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Заказ не найден", "orderNumber", orderNumber));
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}