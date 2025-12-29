package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "client-service", url = "http://localhost:8081")
public interface ClientServiceClient {

    // Публичная регистрация (создает только клиентов с role="client", status="active")
    @PostMapping("/api/clients/register")
    Map<String, Object> registerUser(@RequestBody Map<String, Object> userData);

    // Получение пользователя по username
    @GetMapping("/clients/username/{username}")
    Map<String, Object> getUserByUsername(@PathVariable String username);

    // Проверка email на уникальность
    @PostMapping("/api/clients/check-email")
    Map<String, Object> checkEmail(@RequestBody Map<String, String> request);

    // Проверка username на уникальность
    @PostMapping("/api/clients/check-username")
    Map<String, Object> checkUsername(@RequestBody Map<String, String> request);

    // Единая валидация полей
    @PostMapping("/api/clients/validate")
    Map<String, Object> validateFields(@RequestBody Map<String, String> request);

    // Админские методы (полный контроль)
    @PostMapping("/api/clients")
    Map<String, Object> createClient(@RequestBody Map<String, Object> clientData);

    @GetMapping("/api/clients")
    List<Map<String, Object>> getAllClients();

    @GetMapping("/api/clients/{id}")
    Map<String, Object> getClient(@PathVariable("id") int id);

    @PutMapping("/api/clients/{id}")
    Map<String, Object> updateClient(@PathVariable("id") int id, @RequestBody Map<String, Object> client);

    @DeleteMapping("/api/clients/{id}")
    Map<String, Object> deleteClient(@PathVariable("id") int id);

}