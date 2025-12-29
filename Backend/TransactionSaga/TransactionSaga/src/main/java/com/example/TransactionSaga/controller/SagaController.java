package com.example.TransactionSaga.controller;

import com.example.TransactionSaga.model.entity.SagaStepEntity;
import com.example.TransactionSaga.repository.SagaStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saga")
public class SagaController {

    private final SagaStepRepository sagaStepRepository;

    @Autowired
    public SagaController(SagaStepRepository sagaStepRepository) {
        this.sagaStepRepository = sagaStepRepository;
    }

    @GetMapping("/steps/{transactionId}")
    public ResponseEntity<List<SagaStepEntity>> getTransactionSteps(
            @PathVariable String transactionId) {
        List<SagaStepEntity> steps = sagaStepRepository.findByTransactionId(transactionId);
        return ResponseEntity.ok(steps);
    }

    @PostMapping("/steps/{stepId}/retry")
    public ResponseEntity<String> retryStep(@PathVariable Long stepId) {
        SagaStepEntity step = sagaStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));

        if (step.canRetry()) {
            step.setStatus("PENDING");
            step.incrementRetryCount();
            sagaStepRepository.save(step);
            return ResponseEntity.ok("Step queued for retry");
        }

        return ResponseEntity.badRequest().body("Step cannot be retried");
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Saga orchestrator is running");
    }
}