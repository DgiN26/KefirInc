package ru.kefir.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProcessManagerService {

    private final Map<Integer, Process> runningProcesses = new HashMap<>();
    private final Map<Integer, Integer> portToPidMap = new HashMap<>();

    public Process startService(String command, int port) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        runningProcesses.put(port, process);

        // Сохраняем PID (нужно для Windows)
        try {
            // Получаем PID процесса
            long pid = process.pid();
            portToPidMap.put(port, (int) pid);
        } catch (UnsupportedOperationException e) {
            // На некоторых системах pid() не поддерживается
            portToPidMap.put(port, -1);
        }

        return process;
    }

    public boolean stopService(int port) {
        Process process = runningProcesses.get(port);
        if (process != null) {
            process.destroy();
            try {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                // Игнорируем
            }
            runningProcesses.remove(port);
            portToPidMap.remove(port);
            return true;
        }
        return false;
    }

    public void stopAllServices() {
        for (Integer port : runningProcesses.keySet()) {
            stopService(port);
        }
        runningProcesses.clear();
        portToPidMap.clear();
    }

    public Map<Integer, Process> getRunningProcesses() {
        return new HashMap<>(runningProcesses);
    }
}