package com.example.TransactionSaga.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "office-service", url = "${services.office.url}")
public interface OfficeServiceClient {

    @PostMapping("/api/office/notify-problem")
    Map<String, Object> notifyOfficeAboutProblem(@RequestBody Map<String, Object> request);

    @PostMapping("/api/office/client-notification")
    Map<String, Object> notifyClient(@RequestBody Map<String, Object> notification);

    @GetMapping("/api/office/check-availability/{productId}")
    Map<String, Object> checkProductAvailability(@PathVariable String productId);
}