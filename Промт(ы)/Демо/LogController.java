package com.kefir.logistics.launcher.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {

    @GetMapping("/{serviceName}")
    public ResponseEntity<List<String>> getServiceLogs(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "100") int lines) {

        try {
            Path logFile = Paths.get("./logs/" + serviceName + ".log");
            if (Files.exists(logFile)) {
                List<String> allLines = Files.readAllLines(logFile);
                int startIndex = Math.max(0, allLines.size() - lines);
                List<String> lastLines = allLines.subList(startIndex, allLines.size());
                return ResponseEntity.ok(lastLines);
            } else {
                List<String> message = new ArrayList<>();
                message.add("Log file not found for service: " + serviceName);
                return ResponseEntity.ok(message);
            }
        } catch (IOException e) {
            List<String> error = new ArrayList<>();
            error.add("Error reading logs: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{serviceName}/download")
    public ResponseEntity<Resource> downloadLogFile(@PathVariable String serviceName) {
        try {
            Path logFile = Paths.get("./logs/" + serviceName + ".log");
            if (Files.exists(logFile)) {
                Resource resource = new UrlResource(logFile.toUri());

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + serviceName + "-logs.log\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{serviceName}")
    public ResponseEntity<String> clearServiceLogs(@PathVariable String serviceName) {
        try {
            Path logFile = Paths.get("./logs/" + serviceName + ".log");
            if (Files.exists(logFile)) {
                Files.write(logFile, new byte[0]);
                return ResponseEntity.ok("Logs cleared for service: " + serviceName);
            } else {
                return ResponseEntity.ok("Log file not found for service: " + serviceName);
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Error clearing logs: " + e.getMessage());
        }
    }
}