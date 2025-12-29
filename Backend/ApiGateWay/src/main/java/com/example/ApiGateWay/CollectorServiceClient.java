package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "collector-service", url = "http://localhost:8084")
public interface CollectorServiceClient {

    // Существующие методы...
    @PostMapping("/api/collector/collectors")
    Map<String, Object> createCollector(@RequestBody Map<String, Object> collector);

    @GetMapping("/api/collector/collectors")
    List<Map<String, Object>> getAllCollectors();

    @GetMapping("/api/collector/collectors/{collectorId}")
    Map<String, Object> getCollector(@PathVariable String collectorId);

    @PutMapping("/api/collector/collectors/{collectorId}/status")
    Map<String, Object> updateCollectorStatus(@PathVariable String collectorId, @RequestParam String status);

    @PutMapping("/api/collector/collectors/{collectorId}/location")
    Map<String, Object> updateCollectorLocation(@PathVariable String collectorId, @RequestParam String location);

    @PostMapping("/api/collector/tasks")
    Map<String, Object> createTask(@RequestBody Map<String, Object> task);

    @GetMapping("/api/collector/tasks")
    List<Map<String, Object>> getAllTasks();

    @GetMapping("/api/collector/tasks/{taskId}")
    Map<String, Object> getTask(@PathVariable String taskId);

    @GetMapping("/api/collector/tasks/collector/{collectorId}")
    List<Map<String, Object>> getCollectorTasks(@PathVariable String collectorId);

    @GetMapping("/api/collector/tasks/pending")
    List<Map<String, Object>> getPendingTasks();

    @PutMapping("/api/collector/tasks/{taskId}/status")
    Map<String, Object> updateTaskStatus(@PathVariable String taskId, @RequestParam String status);

    @PostMapping("/api/collector/tasks/{taskId}/report-problem")
    Map<String, Object> reportProblem(@PathVariable String taskId,
                                      @RequestParam String problemType,
                                      @RequestParam String comments);

    @GetMapping("/api/collector/tasks/problems")
    List<Map<String, Object>> getProblemTasks();

    @PutMapping("/api/collector/tasks/{taskId}/complete")
    Map<String, Object> completeTask(@PathVariable String taskId);

    // НОВЫЙ МЕТОД ДЛЯ ТРАНЗАКЦИЙ
    @PostMapping("/api/collector/transactions/process-order")
    Map<String, Object> processOrderTransaction(@RequestBody Map<String, Object> transactionRequest);
}