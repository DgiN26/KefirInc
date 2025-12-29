// Обновленный CartServiceClient.java
package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "cart-service", url = "http://localhost:8085")
public interface CartServiceClient {

    // Существующие методы для корзин
    @PostMapping("/api/cart/client/{clientId}")
    Map<String, Object> createCart(@PathVariable int clientId);

    @PostMapping("/api/cart/{cartId}/add")
    Map<String, Object> addToCart(@PathVariable int cartId,
                                  @RequestParam int productId,
                                  @RequestParam int quantity,
                                  @RequestParam double price);

    @GetMapping("/api/cart/client/{clientId}")
    List<Map<String, Object>> getClientCarts(@PathVariable int clientId);

    @PostMapping("/api/cart/{cartId}/checkout")
    Map<String, Object> checkoutCart(@PathVariable int cartId);

    @GetMapping("/api/cart/{cartId}/items")
    List<Map<String, Object>> getCartItems(@PathVariable int cartId);

    // НОВЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С ЗАКАЗАМИ

    // Создать заказ из корзины
    @PostMapping("/api/cart/{cartId}/create-order")
    Map<String, Object> createOrderFromCart(@PathVariable int cartId);

    // Получить заказы клиента
    @GetMapping("/api/orders/client/{clientId}")
    List<Map<String, Object>> getClientOrders(@PathVariable int clientId);

    // Получить заказ по ID
    @GetMapping("/api/orders/{orderId}")
    Map<String, Object> getOrder(@PathVariable int orderId);

    // Обновить статус заказа
    @PutMapping("/api/orders/{orderId}/status")
    Map<String, Object> updateOrderStatus(@PathVariable int orderId,
                                          @RequestBody Map<String, Object> statusRequest);

    // Получить полную информацию о заказах клиента
    @GetMapping("/api/orders/client/{clientId}/full")
    List<Map<String, Object>> getClientOrdersFull(@PathVariable int clientId);
}