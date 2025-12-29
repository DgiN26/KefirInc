package com.kefir.logistics.launcher_service.service;

import com.kefir.logistics.launcher_service.model.dto.LogEntryDTO;
import com.kefir.logistics.launcher_service.model.entity.ServiceLogEntity;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogStreamingService {

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (\\w+) (\\[.*?\\]) (\\[.*?\\]) (.*)"
    );

    private static final DateTimeFormatter LOG_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public List<LogEntryDTO> getServiceLogs(String serviceName, int maxLines) {
        List<LogEntryDTO> logs = new ArrayList<>();
        File logFile = new File("./logs/" + serviceName + ".log");

        if (!logFile.exists()) {
            // Если файла нет, создаем запись об этом
            LogEntryDTO entry = LogEntryDTO.builder()
                    .timestamp(LocalDateTime.now())
                    .level("WARN")
                    .serviceName(serviceName)
                    .message("Log file not found: " + logFile.getAbsolutePath())
                    .build();
            logs.add(entry);
            return logs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            List<String> lines = new ArrayList<>();
            String line;

            // Читаем все строки
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            // Берем последние maxLines строк
            int startIndex = Math.max(0, lines.size() - maxLines);
            for (int i = startIndex; i < lines.size(); i++) {
                LogEntryDTO logEntry = parseLogLine(lines.get(i), serviceName);
                if (logEntry != null) {
                    logs.add(logEntry);
                }
            }

        } catch (IOException e) {
            LogEntryDTO errorEntry = LogEntryDTO.builder()
                    .timestamp(LocalDateTime.now())
                    .level("ERROR")
                    .serviceName(serviceName)
                    .message("Error reading log file: " + e.getMessage())
                    .build();
            logs.add(errorEntry);
        }

        return logs;
    }

    public List<LogEntryDTO> searchLogs(String serviceName, String searchTerm,
                                        LocalDateTime from, LocalDateTime to) {
        List<LogEntryDTO> results = new ArrayList<>();
        File logFile = new File("./logs/" + serviceName + ".log");

        if (!logFile.exists()) {
            return results;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                LogEntryDTO logEntry = parseLogLine(line, serviceName);
                if (logEntry != null) {
                    // Фильтрация по времени
                    boolean timeMatch = true;
                    if (from != null && logEntry.getTimestamp().isBefore(from)) {
                        timeMatch = false;
                    }
                    if (to != null && logEntry.getTimestamp().isAfter(to)) {
                        timeMatch = false;
                    }

                    // Фильтрация по поисковому запросу
                    boolean textMatch = true;
                    if (searchTerm != null && !searchTerm.isEmpty()) {
                        textMatch = logEntry.getMessage().toLowerCase()
                                .contains(searchTerm.toLowerCase());
                    }

                    if (timeMatch && textMatch) {
                        results.add(logEntry);
                    }
                }
            }

        } catch (IOException e) {
            // Логируем ошибку, но не падаем
            System.err.println("Error searching logs: " + e.getMessage());
        }

        return results;
    }

    private LogEntryDTO parseLogLine(String logLine, String serviceName) {
        try {
            Matcher matcher = LOG_PATTERN.matcher(logLine);
            if (matcher.matches()) {
                LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), LOG_DATE_FORMAT);
                String level = matcher.group(2);
                String thread = matcher.group(3).replace("[", "").replace("]", "");
                String logger = matcher.group(4).replace("[", "").replace("]", "");
                String message = matcher.group(5);

                return LogEntryDTO.builder()
                        .timestamp(timestamp)
                        .level(level)
                        .serviceName(serviceName)
                        .thread(thread)
                        .logger(logger)
                        .message(message)
                        .build();
            }
        } catch (Exception e) {
            // Если не удалось распарсить, возвращаем простую запись
            return LogEntryDTO.builder()
                    .timestamp(LocalDateTime.now())
                    .level("INFO")
                    .serviceName(serviceName)
                    .message(logLine)
                    .build();
        }

        return null;
    }

    public ServiceLogEntity convertToEntity(LogEntryDTO dto) {
        ServiceLogEntity entity = new ServiceLogEntity();
        entity.setServiceId(dto.getServiceName());
        entity.setLogLevel(dto.getLevel());
        entity.setMessage(dto.getMessage());
        entity.setTimestamp(dto.getTimestamp());
        entity.setThreadName(dto.getThread());
        entity.setLoggerName(dto.getLogger());
        entity.setStackTrace(dto.getStackTrace());
        return entity;
    }

    public LogEntryDTO convertToDTO(ServiceLogEntity entity) {
        return LogEntryDTO.builder()
                .timestamp(entity.getTimestamp())
                .level(entity.getLogLevel())
                .serviceName(entity.getServiceId())
                .thread(entity.getThreadName())
                .logger(entity.getLoggerName())
                .message(entity.getMessage())
                .stackTrace(entity.getStackTrace())
                .build();
    }
}