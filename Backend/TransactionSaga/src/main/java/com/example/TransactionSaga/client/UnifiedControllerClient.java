package com.example.TransactionSaga.client;

import com.example.TransactionSaga.dto.LogMessageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "unified-controller", url = "${services.unified-controller.url:http://localhost:8097}")
public interface UnifiedControllerClient {

    @PostMapping("/api/logs/transaction-saga")
    ResponseEntity<Void> sendLog(@RequestBody LogMessageDto logMessage);
}