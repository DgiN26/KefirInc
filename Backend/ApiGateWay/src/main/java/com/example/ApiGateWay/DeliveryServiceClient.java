package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "delivery-service", url = "http://localhost:8083")
public interface DeliveryServiceClient {

    @PostMapping("/api/deliveries")
    Object createDelivery(@RequestBody Map<String, Object> deliveryRequest);

    @PostMapping("/api/deliveries/{deliveryId}/assign")
    Object assignCourier(@PathVariable Integer deliveryId,
                         @RequestBody Map<String, Object> request);

    @PostMapping("/api/deliveries/{deliveryId}/status")
    Object updateDeliveryStatus(@PathVariable Integer deliveryId,
                                @RequestBody Map<String, Object> request);

    @GetMapping("/api/deliveries/client/{clientId}")
    List<Object> getClientDeliveries(@PathVariable Integer clientId);

    @GetMapping("/api/deliveries/courier/{courierId}")
    List<Object> getCourierDeliveries(@PathVariable Integer courierId);

    @GetMapping("/api/deliveries/active")
    List<Object> getActiveDeliveries();

    @GetMapping("/api/deliveries")
    List<Object> getAllDeliveries();

    @GetMapping("/api/deliveries/order/{orderId}")
    List<Object> getDeliveriesByOrderId(@PathVariable Integer orderId);

    @GetMapping("/api/deliveries/order/{orderId}/first")
    Object getFirstDeliveryByOrderId(@PathVariable Integer orderId);

    @PostMapping("/api/deliveries/{deliveryId}/cancel")
    Object cancelDelivery(@PathVariable Integer deliveryId);

    @GetMapping("/api/deliveries/{deliveryId}")
    Object getDelivery(@PathVariable Integer deliveryId);


}