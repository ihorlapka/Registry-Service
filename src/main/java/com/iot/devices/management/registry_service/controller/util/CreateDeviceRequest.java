package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateDeviceRequest(
        @NonNull
        @NotBlank(message = "device name is required")
        String name,
        @NonNull
        @NotBlank(message = "serial number is required")
        String serialNumber,
        @NonNull
        DeviceManufacturer deviceManufacturer,
        String model,
        @NonNull
        DeviceType deviceType,
        String location,
        BigDecimal latitude,
        BigDecimal longitude,
        UUID ownerId,
        @NonNull
        DeviceStatus status,
        OffsetDateTime lastActiveAt,
        String firmwareVersion,
        BigDecimal batteryLevel,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
