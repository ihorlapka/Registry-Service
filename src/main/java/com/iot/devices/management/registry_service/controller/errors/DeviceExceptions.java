package com.iot.devices.management.registry_service.controller.errors;

import java.util.UUID;

public class DeviceExceptions {

    public static class DeviceNotFoundException extends RuntimeException {
        public DeviceNotFoundException(UUID id) {
            super("Device with id: " + id + " not found.");
        }

        public DeviceNotFoundException(String msg) {
            super(msg);
        }
    }

    public static class DuplicateDeviceException extends RuntimeException {
        public DuplicateDeviceException(String serialNumber) {
            super("Device with serial number: '" + serialNumber + "' already exists.");
        }
    }

    public static class UnableToCreateDeviceException extends RuntimeException {
        public UnableToCreateDeviceException(String msg, Exception e) {
            super(msg, e);
        }
    }

    public static class UnableToPatchDeviceException extends RuntimeException {
        public UnableToPatchDeviceException(String msg, Exception e) {
            super(msg, e);
        }
    }

    public static class UnableToRemoveDeviceException extends RuntimeException {
        public UnableToRemoveDeviceException(String msg, Exception e) {
            super(msg, e);
        }
    }
}
