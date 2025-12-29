package com.example.TransactionSaga.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductServiceClient {

    @PostMapping("/api/products/check-stock")
    Map<String, Object> checkStock(@RequestBody Map<String, Object> request);

    @PostMapping("/api/products/reserve")
    Map<String, Object> reserveProducts(@RequestBody Map<String, Object> request);

    @PostMapping("/api/products/release")
    Map<String, Object> releaseReservation(@RequestBody Map<String, Object> request);

    @GetMapping("/api/products/{productId}/availability")
    Map<String, Object> getProductAvailability(@PathVariable String productId);
}