package com.kefir.logistics.launcher_service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class PowerShellHelper {
    private static final Logger logger = LoggerFactory.getLogger(PowerShellHelper.class);

    /**
     * Освобождает порт с помощью PowerShell скрипта
     * Использует ваш ИДЕАЛЬНЫЙ скрипт
     */
    public boolean releasePortWithPowerShell(int port) {
        logger.info("🔧 Освобождаю порт {} с помощью PowerShell", port);

        String command = buildPowerShellCommand(port);

        try {
            logger.debug("Выполняю команду: {}", command);

            Process process = Runtime.getRuntime().exec(command);

            // Читаем вывод скрипта
            BufferedReader outputReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "CP866")); // Windows CP866
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), "CP866"));

            StringBuilder output = new StringBuilder();
            String line;

            while ((line = outputReader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug("PowerShell: {}", line);
            }

            while ((line = errorReader.readLine()) != null) {
                logger.error("PowerShell Error: {}", line);
            }

            int exitCode = process.waitFor();
            logger.info("PowerShell скрипт завершился с кодом: {}", exitCode);

            // Проверяем результат
            boolean success = (exitCode == 0);

            if (success) {
                logger.info("✅ PowerShell успешно освободил порт {}", port);
            } else {
                logger.warn("⚠️ PowerShell не смог освободить порт {}", port);
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ Ошибка при выполнении PowerShell скрипта: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Освобождает несколько портов за один вызов
     */
    public boolean releasePortsWithPowerShell(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) {
            logger.info("Нет портов для освобождения");
            return true;
        }

        logger.info("🔧 Освобождаю {} портов с помощью PowerShell: {}", ports.size(), ports);

        String command = buildMultiPortPowerShellCommand(ports);

        try {
            Process process = Runtime.getRuntime().exec(command);

            // Читаем вывод
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "CP866"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("✅") || line.contains("свободен")) {
                        logger.info("PowerShell: {}", line);
                    } else if (line.contains("❌") || line.contains("ошибка")) {
                        logger.error("PowerShell: {}", line);
                    } else {
                        logger.debug("PowerShell: {}", line);
                    }
                }
            }

            int exitCode = process.waitFor();
            boolean success = (exitCode == 0);

            logger.info("PowerShell скрипт для {} портов завершился: {}",
                    ports.size(), success ? "УСПЕШНО" : "С ОШИБКОЙ");

            return success;

        } catch (Exception e) {
            logger.error("❌ Ошибка при выполнении PowerShell скрипта для портов: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Получает PID процесса, занимающего порт (PowerShell версия)
     */
    public String getProcessIdOnPortPowerShell(int port) {
        String command = String.format(
                "powershell.exe -Command \"Get-NetTCPConnection -LocalPort %d -ErrorAction SilentlyContinue | " +
                        "Select-Object -ExpandProperty OwningProcess | Select-Object -First 1\"",
                port
        );

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String pid = reader.readLine();
            process.waitFor();

            if (pid != null && !pid.trim().isEmpty()) {
                return pid.trim();
            }

        } catch (Exception e) {
            logger.debug("Не удалось получить PID через PowerShell для порта {}: {}", port, e.getMessage());
        }

        return null;
    }

    /**
     * Проверяет, свободен ли порт (PowerShell версия)
     */
    public boolean isPortFreePowerShell(int port) {
        String command = String.format(
                "powershell.exe -Command \"$conn = Get-NetTCPConnection -LocalPort %d -ErrorAction SilentlyContinue; " +
                        "if ($conn) { Write-Output 'OCCUPIED' } else { Write-Output 'FREE' }\"",
                port
        );

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String result = reader.readLine();
            process.waitFor();

            return "FREE".equals(result);

        } catch (Exception e) {
            logger.debug("Не удалось проверить порт через PowerShell: {}", e.getMessage());
            return false;
        }
    }

    private String buildPowerShellCommand(int port) {
        // Ваш ИДЕАЛЬНЫЙ скрипт в одну строку
        return String.format(
                "powershell.exe -Command \"$port = %d; " +
                        "$connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue; " +
                        "if ($connection) { " +
                        "    Write-Host 'станавливаю процесс на порту $port (PID: ' $connection.OwningProcess ')' -ForegroundColor Yellow; " +
                        "    Stop-Process -Id $connection.OwningProcess -Force; " +
                        "    Write-Host '✅ Порт $port освобожден' -ForegroundColor Green; " +
                        "} else { " +
                        "    Write-Host '✅ Порт $port свободен' -ForegroundColor Green; " +
                        "}\"",
                port
        );
    }

    private String buildMultiPortPowerShellCommand(List<Integer> ports) {
        StringBuilder sb = new StringBuilder();
        sb.append("powershell.exe -Command \"");

        for (Integer port : ports) {
            sb.append(String.format(
                    "$port = %d; " +
                            "$connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue; " +
                            "if ($connection) { " +
                            "    Write-Host 'станавливаю процесс на порту $port (PID: ' $connection.OwningProcess ')' -ForegroundColor Yellow; " +
                            "    Stop-Process -Id $connection.OwningProcess -Force; " +
                            "    Write-Host '✅ Порт $port освобожден' -ForegroundColor Green; " +
                            "} else { " +
                            "    Write-Host '✅ Порт $port свободен' -ForegroundColor Green; " +
                            "}; ",
                    port
            ));
        }

        sb.append("\"");
        return sb.toString();
    }
}