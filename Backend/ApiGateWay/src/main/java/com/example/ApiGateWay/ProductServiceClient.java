package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service", url = "http://localhost:8082/api")
public interface ProductServiceClient {

    @GetMapping("/products")
    List<Map<String, Object>> getAllProducts();

    @PostMapping("/products")
    Map<String, Object> createProduct(@RequestBody Map<String, Object> product);

    @GetMapping("/products/{id}")
    Map<String, Object> getProduct(@PathVariable int id);

    @PutMapping("/products/{id}")
    Map<String, Object> updateProduct(@PathVariable int id, @RequestBody Map<String, Object> product);

    @DeleteMapping("/products/{id}")
    ResponseEntity<Void> deleteProduct(@PathVariable int id);
}