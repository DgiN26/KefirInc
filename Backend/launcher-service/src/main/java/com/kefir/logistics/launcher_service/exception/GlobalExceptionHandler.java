package com.kefir.logistics.launcher_service.exception;

import com.kefir.logistics.launcher_service.model.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceLaunchException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceLaunchException(ServiceLaunchException ex) {
        logger.error("Service launch error: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = new ErrorResponseDTO(
                "SERVICE_LAUNCH_FAILED",
                ex.getMessage(),
                ex.getServiceName()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceNotFoundException(ServiceNotFoundException ex) {
        logger.warn("Service not found: {}", ex.getMessage());

        ErrorResponseDTO error = new ErrorResponseDTO(
                "SERVICE_NOT_FOUND",
                ex.getMessage(),
                ex.getServiceName()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());

        ErrorResponseDTO error = new ErrorResponseDTO(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                null
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = new ErrorResponseDTO(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                null
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}