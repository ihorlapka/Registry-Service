package com.iot.devices.management.registry_service.controller.errors;

import com.iot.devices.management.registry_service.controller.util.UserErrorResponse;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.iot.devices.management.registry_service.controller.errors.UserExceptions.*;
import static com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.*;
import static java.util.Collections.emptyMap;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<UserErrorResponse> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        final UserErrorResponse response = UserErrorResponse.of(
                NOT_FOUND,
                ex.getMessage(),
                "Unable to find user!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, NOT_FOUND);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<UserErrorResponse> handleDuplicateUserException(DuplicateUserException ex, WebRequest request) {
        final UserErrorResponse response = UserErrorResponse.of(
                CONFLICT,
                ex.getMessage(),
                "Duplicate user!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, CONFLICT);
    }

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<UserErrorResponse> handleDeviceNotFoundException(DeviceNotFoundException ex, WebRequest request) {
        final UserErrorResponse response = UserErrorResponse.of(
                NOT_FOUND,
                ex.getMessage(),
                "Unable to find device!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, NOT_FOUND);
    }

    @ExceptionHandler(DuplicateDeviceException.class)
    public ResponseEntity<UserErrorResponse> handleDuplicateDeviceException(DuplicateDeviceException ex, WebRequest request) {
        final UserErrorResponse response = UserErrorResponse.of(
                CONFLICT,
                ex.getMessage(),
                "Duplicate device!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, CONFLICT);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status, WebRequest request) {
        final Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        final UserErrorResponse response = UserErrorResponse.of(
                BAD_REQUEST,
                ex.getMessage(),
                "Validation failed for one or more fields!",
                URI.create(request.getDescription(false)),
                errors);
        return new ResponseEntity<>(response, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UserErrorResponse> handleAllUncaughtException(Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred: " + ex.getMessage(), ex);

        final UserErrorResponse response = UserErrorResponse.of(
                INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                "Validation failed for one or more fields!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, INTERNAL_SERVER_ERROR);
    }
}
