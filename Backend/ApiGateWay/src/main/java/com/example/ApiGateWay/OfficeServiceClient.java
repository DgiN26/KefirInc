package com.example.ApiGateWay;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "office-service", url = "http://localhost:8085")
public interface OfficeServiceClient {

    @PostMapping("/api/office/accept-return-from-collector")
    Map<String, Object> acceptReturnFromCollector(@RequestBody Map<String, Object> returnRequest);

    @PostMapping("/api/office/give-return-to-client")
    Map<String, Object> giveReturnToClient(@RequestBody Map<String, Object> returnRequest);

    @PostMapping("/api/office/send-return-to-collector")
    Map<String, Object> sendReturnToCollector(@RequestBody Map<String, Object> returnRequest);

    @GetMapping("/api/office/returns")
    List<Map<String, Object>> getAllReturns();

    @GetMapping("/api/office/returns/{id}")
    Map<String, Object> getReturnById(@PathVariable("id") Long id);

    @GetMapping("/api/office/returns/client/{clientId}")
    List<Map<String, Object>> getReturnsByClientId(@PathVariable("clientId") String clientId);
}