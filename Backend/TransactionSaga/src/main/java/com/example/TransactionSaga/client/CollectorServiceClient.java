package com.example.TransactionSaga.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "collector-service", url = "${services.collector.url}")
public interface CollectorServiceClient {

    @PostMapping("/api/collector/tasks")
    Map<String, Object> createTask(@RequestBody Map<String, Object> task);

    @PutMapping("/api/collector/tasks/{taskId}/status")
    Map<String, Object> updateTaskStatus(@PathVariable String taskId,
                                         @RequestParam String status);

    @PostMapping("/api/collector/notify")
    Map<String, Object> notifyCollector(@RequestBody Map<String, Object> notification);

    @PostMapping("/api/collector/cancel-task")
    Map<String, Object> cancelTask(@RequestBody Map<String, Object> request);
}