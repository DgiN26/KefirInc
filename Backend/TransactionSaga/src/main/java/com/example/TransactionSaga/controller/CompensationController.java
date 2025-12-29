package com.example.TransactionSaga.controller;

import com.example.TransactionSaga.model.entity.CompensationLogEntity;
import com.example.TransactionSaga.service.CompensationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compensation")
public class CompensationController {

    private final CompensationService compensationService;

    @Autowired
    public CompensationController(CompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @PostMapping("/{transactionId}/initiate")
    public ResponseEntity<String> initiateCompensation(
            @PathVariable String transactionId,
            @RequestParam String reason,
            @RequestParam(required = false) String details) {

        compensationService.initiateCompensation(transactionId, reason, details);
        return ResponseEntity.ok("Compensation initiated");
    }

    @GetMapping("/history/{transactionId}")
    public ResponseEntity<List<CompensationLogEntity>> getCompensationHistory(
            @PathVariable String transactionId) {
        List<CompensationLogEntity> history =
                compensationService.getCompensationHistory(transactionId);
        return ResponseEntity.ok(history);
    }
}