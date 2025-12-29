package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:8097")
public interface AuthServiceClient {

    @PostMapping("/api/auth/login")
    ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request);

    @PostMapping("/api/auth/validate")
    Map<String, Object> validateToken(@RequestBody Map<String, String> request);

    @GetMapping("/api/auth/check")
    Map<String, Object> check();
}