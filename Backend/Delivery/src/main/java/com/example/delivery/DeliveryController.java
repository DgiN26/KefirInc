package com.example.delivery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    
    @Autowired
    private DeliveryService deliveryService;
    
    // Создание новой доставки
    @PostMapping
    public ResponseEntity<Delivery> createDelivery(@RequestBody Map<String, Object> deliveryRequest) {
        try {
            Integer orderId = (Integer) deliveryRequest.get("orderId");
            Integer clientId = (Integer) deliveryRequest.get("clientId");
            String address = (String) deliveryRequest.get("deliveryAddress");
            String phone = (String) deliveryRequest.get("deliveryPhone");
            
            Delivery delivery = deliveryService.createDelivery(orderId, clientId, address, phone);
            return ResponseEntity.ok(delivery);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Назначение курьера
    @PostMapping("/{deliveryId}/assign")
    public ResponseEntity<Delivery> assignCourier(
            @PathVariable Integer deliveryId,
            @RequestBody Map<String, Object> request) {
        try {
            Integer courierId = (Integer) request.get("courierId");
            Delivery delivery = deliveryService.assignCourier(deliveryId, courierId);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Обновление статуса доставки
    @PostMapping("/{deliveryId}/status")
    public ResponseEntity<Delivery> updateStatus(
            @PathVariable Integer deliveryId,
            @RequestBody Map<String, Object> request) {
        try {
            String statusStr = (String) request.get("status");
            DeliveryStatus status = DeliveryStatus.valueOf(statusStr.toUpperCase());
            Delivery delivery = deliveryService.updateStatus(deliveryId, status);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Получение всех доставок клиента
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Delivery>> getClientDeliveries(@PathVariable Integer clientId) {
        List<Delivery> deliveries = deliveryService.getClientDeliveries(clientId);
        return ResponseEntity.ok(deliveries);
    }
    
    // Получение доставок курьера
    @GetMapping("/courier/{courierId}")
    public ResponseEntity<List<Delivery>> getCourierDeliveries(@PathVariable Integer courierId) {
        List<Delivery> deliveries = deliveryService.getCourierDeliveries(courierId);
        return ResponseEntity.ok(deliveries);
    }
    
    // Получение активных доставок
    @GetMapping("/active")
    public ResponseEntity<List<Delivery>> getActiveDeliveries() {
        List<Delivery> deliveries = deliveryService.getActiveDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    // ✅ НОВЫЙ МЕТОД: Получение ВСЕХ доставок
    @GetMapping
    public ResponseEntity<List<Delivery>> getAllDeliveries() {
        List<Delivery> deliveries = deliveryService.getAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }
    
    // Получение доставок по ID заказа
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Delivery>> getDeliveriesByOrderId(@PathVariable Integer orderId) {
        List<Delivery> deliveries = deliveryService.getDeliveriesByOrderId(orderId);
        return ResponseEntity.ok(deliveries);
    }
    
    // Получение первой доставки по orderId
    @GetMapping("/order/{orderId}/first")
    public ResponseEntity<Delivery> getFirstDeliveryByOrderId(@PathVariable Integer orderId) {
        Optional<Delivery> delivery = deliveryService.getFirstDeliveryByOrderId(orderId);
        return delivery.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    // Отмена доставки
    @PostMapping("/{deliveryId}/cancel")
    public ResponseEntity<Delivery> cancelDelivery(@PathVariable Integer deliveryId) {
        try {
            Delivery delivery = deliveryService.cancelDelivery(deliveryId);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Получение доставки по ID
    @GetMapping("/{deliveryId}")
    public ResponseEntity<Delivery> getDelivery(@PathVariable Integer deliveryId) {
        Optional<Delivery> delivery = deliveryService.getDeliveryById(deliveryId);
        return delivery.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
}