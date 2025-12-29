package com.example.collector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/collector")
public class CollectorController {

    @Autowired
    private CollectorService collectorService;

    @PostMapping("/collectors")
    public ResponseEntity<Collector> createCollector(@RequestBody Collector collector) {
        Collector created = collectorService.createCollector(collector);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/collectors")
    public ResponseEntity<List<Collector>> getAllCollectors() {
        List<Collector> collectors = collectorService.getAllCollectors();
        return ResponseEntity.ok(collectors);
    }

    @GetMapping("/collectors/{collectorId}")
    public ResponseEntity<Collector> getCollector(@PathVariable String collectorId) {
        Optional<Collector> collector = collectorService.getCollectorById(collectorId);
        return collector.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/collectors/{collectorId}/status")
    public ResponseEntity<Collector> updateCollectorStatus(
            @PathVariable String collectorId,
            @RequestParam String status) {
        Collector collector = collectorService.updateCollectorStatus(collectorId, status);
        return ResponseEntity.ok(collector);
    }

    @PutMapping("/collectors/{collectorId}/location")
    public ResponseEntity<Collector> updateCollectorLocation(
            @PathVariable String collectorId,
            @RequestParam String location) {
        Collector collector = collectorService.updateCollectorLocation(collectorId, location);
        return ResponseEntity.ok(collector);
    }

    @PostMapping("/tasks")
    public ResponseEntity<CollectorTask> createTask(@RequestBody CollectorTask task) {
        CollectorTask created = collectorService.createTask(task);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<CollectorTask>> getAllTasks() {
        List<CollectorTask> tasks = collectorService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<CollectorTask> getTask(@PathVariable String taskId) {
        Optional<CollectorTask> task = collectorService.getTask(taskId);
        return task.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tasks/collector/{collectorId}")
    public ResponseEntity<List<CollectorTask>> getCollectorTasks(@PathVariable String collectorId) {
        List<CollectorTask> tasks = collectorService.getCollectorTasks(collectorId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/pending")
    public ResponseEntity<List<CollectorTask>> getPendingTasks() {
        List<CollectorTask> tasks = collectorService.getPendingTasks();
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/tasks/{taskId}/status")
    public ResponseEntity<CollectorTask> updateTaskStatus(
            @PathVariable String taskId,
            @RequestParam String status) {
        CollectorTask task = collectorService.updateTaskStatus(taskId, status);
        return ResponseEntity.ok(task);
    }

    // НОВЫЙ ЭНДПОИНТ ДЛЯ ТРАНЗАКЦИЙ
    @PostMapping("/transactions/process-order")
    public ResponseEntity<Map<String, Object>> processOrderTransaction(@RequestBody Map<String, Object> transactionRequest) {
        try {
            Map<String, Object> result = collectorService.processOrderTransaction(transactionRequest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "taskId", transactionRequest.get("taskId")
            ));
        }
    }

    @PostMapping("/tasks/{taskId}/report-problem")
    public ResponseEntity<CollectorTask> reportProblem(
            @PathVariable String taskId,
            @RequestParam String problemType,
            @RequestParam String comments) {
        CollectorTask task = collectorService.reportProblem(taskId, problemType, comments);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/tasks/problems")
    public ResponseEntity<List<CollectorTask>> getProblemTasks() {
        List<CollectorTask> tasks = collectorService.getProblemTasks();
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/tasks/{taskId}/complete")
    public ResponseEntity<CollectorTask> completeTask(@PathVariable String taskId) {
        CollectorTask task = collectorService.completeTask(taskId);
        return ResponseEntity.ok(task);
    }
}