package com.example.collector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CollectorService {

    @Autowired
    private CollectorRepository collectorRepository;

    @Autowired
    private CollectorTaskRepository taskRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    public Collector createCollector(Collector collector) {
        if (collectorRepository.existsByCollectorId(collector.getCollectorId())) {
            throw new RuntimeException("Collector with ID " + collector.getCollectorId() + " already exists");
        }
        return collectorRepository.save(collector);
    }

    public List<Collector> getAllCollectors() {
        return collectorRepository.findAll();
    }

    public Optional<Collector> getCollectorById(String collectorId) {
        return collectorRepository.findByCollectorId(collectorId);
    }

    public Collector updateCollectorStatus(String collectorId, String status) {
        Collector collector = collectorRepository.findByCollectorId(collectorId)
                .orElseThrow(() -> new RuntimeException("Collector not found"));
        collector.setStatus(status);
        return collectorRepository.save(collector);
    }

    public Collector updateCollectorLocation(String collectorId, String location) {
        Collector collector = collectorRepository.findByCollectorId(collectorId)
                .orElseThrow(() -> new RuntimeException("Collector not found"));
        collector.setCurrentLocation(location);
        return collectorRepository.save(collector);
    }

    public CollectorTask createTask(CollectorTask task) {
        if (!collectorRepository.existsByCollectorId(task.getCollectorId())) {
            throw new RuntimeException("Collector " + task.getCollectorId() + " not found");
        }
        return taskRepository.save(task);
    }

    public List<CollectorTask> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<CollectorTask> getTask(String taskId) {
        return taskRepository.findByTaskId(taskId);
    }

    public List<CollectorTask> getCollectorTasks(String collectorId) {
        return taskRepository.findByCollectorId(collectorId);
    }

    public List<CollectorTask> getPendingTasks() {
        return taskRepository.findByStatus("NEW");
    }

    public CollectorTask updateTaskStatus(String taskId, String status) {
        CollectorTask task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(status);
        return taskRepository.save(task);
    }

    @Transactional
    public Map<String, Object> processOrderTransaction(Map<String, Object> transactionRequest) {
        String taskId = (String) transactionRequest.get("taskId");
        String collectorId = (String) transactionRequest.get("collectorId");
        
        CollectorTask task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Начинаем транзакцию
        task.setStatus("PROCESSING");
        task.setStartedDate(LocalDateTime.now());
        taskRepository.save(task);
        
        try {
            // Проверяем наличие товаров
            boolean allItemsAvailable = checkItemsAvailability(transactionRequest);
            
            if (allItemsAvailable) {
                // Все товары есть - успешное завершение
                return completeTransactionSuccess(task, "Все товары доступны");
            } else {
                // Товаров нет - создаем возврат в офисе
                return completeTransactionWithReturn(transactionRequest, task);
            }
        } catch (Exception e) {
            // Откат транзакции при ошибке
            task.setStatus("FAILED");
            task.setComments("Ошибка транзакции: " + e.getMessage());
            taskRepository.save(task);
            throw new RuntimeException("Транзакция отменена: " + e.getMessage(), e);
        }
    }
    
    private boolean checkItemsAvailability(Map<String, Object> transactionRequest) {
        // Реальная логика проверки наличия товаров
        // Заглушка: 60% вероятность что товары есть
        return Math.random() > 0.4;
    }
    
    private Map<String, Object> completeTransactionSuccess(CollectorTask task, String message) {
        task.setStatus("COMPLETED");
        task.setCompletedDate(LocalDateTime.now());
        task.setComments(message);
        taskRepository.save(task);
        
        return Map.of(
            "success", true,
            "message", message,
            "taskId", task.getTaskId(),
            "status", "COMPLETED",
            "transactionStatus", "COMMITTED",
            "timestamp", LocalDateTime.now()
        );
    }
    
    private Map<String, Object> completeTransactionWithReturn(Map<String, Object> transactionRequest, CollectorTask task) {
        try {
            // Создаем запрос на возврат в офисе
            Map<String, Object> officeRequest = Map.of(
                "requestId", "RETURN_" + task.getTaskId(),
                "collectorId", task.getCollectorId(),
                "clientId", transactionRequest.get("clientId"),
                "productId", transactionRequest.get("productId"),
                "reason", "Товар отсутствует на складе",
                "quantity", transactionRequest.get("quantity"),
                "officeId", "OFFICE_001",
                "taskId", task.getTaskId()
            );
            
            // Отправляем в офис
            String officeUrl = "http://localhost:8085/api/office/accept-return-from-collector";
            Map<String, Object> officeResponse = restTemplate.postForObject(officeUrl, officeRequest, Map.class);
            
            if (officeResponse != null && officeResponse.containsKey("id")) {
                // Успешно создан возврат
                task.setStatus("COMPLETED_WITH_RETURN");
                task.setCompletedDate(LocalDateTime.now());
                task.setComments("Создан РОЗТ: " + officeResponse.get("id"));
                taskRepository.save(task);
                
                return Map.of(
                    "success", true,
                    "message", "Транзакция завершена с созданием РОЗТ",
                    "taskId", task.getTaskId(),
                    "status", "COMPLETED_WITH_RETURN",
                    "transactionStatus", "COMMITTED_WITH_RETURN",
                    "returnId", officeResponse.get("id"),
                    "timestamp", LocalDateTime.now()
                );
            } else {
                throw new RuntimeException("Не удалось создать возврат в офисе");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании возврата: " + e.getMessage(), e);
        }
    }

    public CollectorTask reportProblem(String taskId, String problemType, String comments) {
        CollectorTask task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setProblemType(problemType);
        task.setComments(comments);
        task.setStatus("PROBLEM");
        task.setPriority("HIGH");
        return taskRepository.save(task);
    }

    public List<CollectorTask> getProblemTasks() {
        return taskRepository.findByProblemTypeIsNotNull();
    }

    public CollectorTask completeTask(String taskId) {
        CollectorTask task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus("COMPLETED");
        return taskRepository.save(task);
    }
}