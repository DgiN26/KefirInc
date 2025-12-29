package com.kefir.logistics.launcher_service.model.enums;

public enum ServiceState {
    STOPPED("stopped", "Остановлен", false),
    STARTING("starting", "Запускается", false),
    RUNNING("running", "Работает", true),
    FAILED("failed", "Ошибка", false),
    RESTARTING("restarting", "Перезапускается", false),
    HEALTHY("healthy", "Здоров", true),
    UNHEALTHY("unhealthy", "Не здоров", false),
    EXTERNAL("external", "Внешний процесс", true); // <-- ДОБАВЛЕНО!

    private final String code;
    private final String description;
    private final boolean active;

    ServiceState(String code, String description, boolean active) {
        this.code = code;
        this.description = description;
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isRunning() {
        return this == RUNNING || this == HEALTHY || this == EXTERNAL;
    }

    public boolean isStopped() {
        return this == STOPPED;
    }

    public boolean isFailed() {
        return this == FAILED || this == UNHEALTHY;
    }

    // Дополнительные методы для удобства
    public boolean isManaged() {
        return this != EXTERNAL;
    }

    public boolean isExternal() {
        return this == EXTERNAL;
    }

    public boolean isHealthyState() {
        return this == HEALTHY || this == RUNNING || this == EXTERNAL;
    }

    public static ServiceState fromCode(String code) {
        for (ServiceState state : values()) {
            if (state.getCode().equalsIgnoreCase(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown ServiceState code: " + code);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", code, description);
    }
}