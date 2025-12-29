package com.kefir.logistics.launcher_service.exception;

public class ServiceLaunchException extends RuntimeException {

    private final String serviceName;
    private final String errorCode;

    public ServiceLaunchException(String serviceName, String message) {
        super("Failed to launch service '" + serviceName + "': " + message);
        this.serviceName = serviceName;
        this.errorCode = "LAUNCH_FAILED";
    }

    public ServiceLaunchException(String serviceName, String message, Throwable cause) {
        super("Failed to launch service '" + serviceName + "': " + message, cause);
        this.serviceName = serviceName;
        this.errorCode = "LAUNCH_FAILED";
    }

    public ServiceLaunchException(String serviceName, String message, String errorCode) {
        super("Failed to launch service '" + serviceName + "': " + message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}