package com.iot.devices.management.registry_service.controller.dto;

import com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceDTO(
        UUID id,
        String name,
        String serialNumber,
        DeviceManufacturer deviceManufacturer,
        String model,
        DeviceType deviceType,
        String location,
        BigDecimal latitude,
        BigDecimal longitude,
        UUID ownerId,
        DeviceStatus status,
        OffsetDateTime lastActiveAt,
        String firmwareVersion,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
