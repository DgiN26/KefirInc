package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:8097")
public interface AuthServiceClient {

    @PostMapping("/api/auth/login")
    ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request);

    // ✅ ИСПРАВЛЕНО: POST с параметром в query string
    @PostMapping("/api/auth/validate")
    Map<String, Object> validateToken(@RequestParam("clientToken") String clientToken);

    // Альтернативный endpoint (тоже работает)
    @GetMapping("/api/sessions/validate/{clientToken}")
    Map<String, Object> validateSession(@PathVariable("clientToken") String clientToken);

    @GetMapping("/api/auth/me")
    Map<String, Object> getCurrentUser(@RequestParam("clientToken") String clientToken);

    @GetMapping("/api/auth/check")
    Map<String, Object> check();
}