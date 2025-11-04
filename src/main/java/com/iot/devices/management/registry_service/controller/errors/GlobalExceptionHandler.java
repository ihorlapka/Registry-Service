package com.iot.devices.management.registry_service.controller.errors;

import com.iot.devices.management.registry_service.controller.util.ErrorResponse;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
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
import static com.iot.devices.management.registry_service.controller.errors.AlertRulesException.*;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        final ErrorResponse response = ErrorResponse.of(
                NOT_FOUND,
                ex.getMessage(),
                "Unable to find user!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, NOT_FOUND);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUserException(DuplicateUserException ex, WebRequest request) {
        final ErrorResponse response = ErrorResponse.of(
                CONFLICT,
                ex.getMessage(),
                "Duplicate user!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, CONFLICT);
    }

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDeviceNotFoundException(DeviceNotFoundException ex, WebRequest request) {
        final ErrorResponse response = ErrorResponse.of(
                NOT_FOUND,
                ex.getMessage(),
                "Unable to find device!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, NOT_FOUND);
    }

    @ExceptionHandler(DuplicateDeviceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDeviceException(DuplicateDeviceException ex, WebRequest request) {
        final ErrorResponse response = ErrorResponse.of(
                CONFLICT,
                ex.getMessage(),
                "Duplicate device!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, CONFLICT);
    }

    @ExceptionHandler(AlertRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAlertRuleNotFoundException(AlertRuleNotFoundException ex, WebRequest request) {
        final ErrorResponse response = ErrorResponse.of(
                NOT_FOUND,
                ex.getMessage(),
                "Unable to find alert rule!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, NOT_FOUND);
    }

    @ExceptionHandler(AlertRuleNotSentException.class)
    public ResponseEntity<ErrorResponse> handleAlertRuleNotSentException(AlertRuleNotSentException ex, WebRequest request) {
        final ErrorResponse response = ErrorResponse.of(
                NOT_FOUND,
                ex.getMessage(),
                "Unable to send alert rule to telemetries events processor!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, NOT_FOUND);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationExceptionException(AuthenticationException ex, WebRequest request) {
        final ErrorResponse response = ErrorResponse.of(
                FORBIDDEN,
                ex.getMessage(),
                "Authentication exception!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, FORBIDDEN);
    }

    @ExceptionHandler(UnableToCreateAlertRuleException.class)
    public ResponseEntity<ErrorResponse> handleAlertRuleNotSentException(UnableToCreateAlertRuleException ex, WebRequest request) {
        final ErrorResponse response = ErrorResponse.of(
                INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                "Unexpected exception occurred while creating alert rule!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, INTERNAL_SERVER_ERROR);
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
        final ErrorResponse response = ErrorResponse.of(
                BAD_REQUEST,
                ex.getMessage(),
                "Validation failed for one or more fields!",
                URI.create(request.getDescription(false)),
                errors);
        return new ResponseEntity<>(response, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred: " + ex.getMessage(), ex);

        final ErrorResponse response = ErrorResponse.of(
                INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                "Server error!",
                URI.create(request.getDescription(false)),
                emptyMap());
        return new ResponseEntity<>(response, INTERNAL_SERVER_ERROR);
    }
}
