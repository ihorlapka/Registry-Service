package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record PatchDeviceRequest(
        @NonNull UUID id,
        @Nullable String name,
        @Nullable String model,
        @Nullable DeviceType deviceType,
        @Nullable String location,
        @Nullable BigDecimal latitude,
        @Nullable BigDecimal longitude,
        @Nullable UUID ownerId,
        @Nullable DeviceStatus status,
        @Nullable OffsetDateTime lastActiveAt,
        @Nullable String firmwareVersion,
        @Nullable OffsetDateTime updatedAt,
        @Nullable Set<UUID> alertRulesToAdd,
        @Nullable Set<UUID> alertRulesToRemove) {
}
