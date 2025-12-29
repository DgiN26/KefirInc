package com.kefir.logistics.launcher_service.model.enums;

public enum ServiceType {
    AUTH_SERVICE("AUTH", 8097, "Auth", "Authentication Service"),
    USER_SERVICE("USER", 8081, "User", "User Management Service"),
    SKLAD_SERVICE("Sklad", 8082, "Sklad", "Warehouse Service"),
    BACKET_SERVICE("backet-service", 8083, "Backet", "Shopping Cart Service"),
    OFFICE_SERVICE("Office", 8085, "Office", "Office Management Service"),
    COLLECTOR_SERVICE("COLLECTOR", 8086, "Collector", "Collector Service"),
    DELIVERY_SERVICE("Delivery", 8088, "Delivery", "Delivery Service"),
    SAGA_SERVICE("TransactionSaga", 8090, "TransactionSaga", "Transaction Saga Service"),
    API_GATEWAY("ApiGateWay", 8080, "ApiGateWay", "API Gateway");

    private String id;
    private int port;
    private String directory;
    private String displayName;

    ServiceType(String id, int port, String directory, String displayName) {
        this.id = id;
        this.port = port;
        this.directory = directory;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public int getPort() { return port; }
    public int getDefaultPort() { return port; }  // ДОБАВЬТЕ ЭТОТ МЕТОД
    public String getDirectory() { return directory; }
    public String getDisplayName() { return displayName; }

    public static ServiceType fromId(String id) {
        for (ServiceType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown service: " + id);
    }

    public static ServiceType fromPort(int port) {
        for (ServiceType type : values()) {
            if (type.port == port) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown port: " + port);
    }
}