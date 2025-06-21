package com.iot.devices.management.registry_service.controller.errors;

public class UserExceptions {

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(Long id) {
            super("User with id: " + id + " not found.");
        }
        public UserNotFoundException(String email) {
            super("User with email: " + email + " not found.");
        }
    }

    public static class DuplicateUserException extends RuntimeException {
        public DuplicateUserException(String email) {
            super("User with email: '" + email + "' already exists.");
        }
    }

    public static class PermissionDeniedException extends RuntimeException {
        public PermissionDeniedException(String message) {
            super(message);
        }
    }
}
