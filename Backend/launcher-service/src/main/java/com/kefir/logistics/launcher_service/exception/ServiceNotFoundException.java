package com.kefir.logistics.launcher_service.exception;

public class ServiceNotFoundException extends RuntimeException {

    private final String serviceName;

    public ServiceNotFoundException(String serviceName) {
        super("Service not found: " + serviceName);
        this.serviceName = serviceName;
    }

    public ServiceNotFoundException(String serviceName, String message) {
        super("Service '" + serviceName + "' not found: " + message);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}