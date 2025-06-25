package com.iot.devices.management.registry_service.controller.errors;

import java.util.UUID;

public class DeviceExceptions {
    public static class DeviceNotFoundException extends RuntimeException {
        public DeviceNotFoundException(UUID id) {
            super("Device with id: " + id + " not found.");
        }
        public DeviceNotFoundException(String serialNumber) {
            super("Device with serial number: " + serialNumber + " not found.");
        }
    }

    public static class DuplicateDeviceException extends RuntimeException {
        public DuplicateDeviceException(String serialNumber) {
            super("Device with serial number: '" + serialNumber + "' already exists.");
        }
    }
}
