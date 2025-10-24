package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType;
import jakarta.validation.constraints.NotEmpty;
import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.util.Set;
import java.util.UUID;

public record CreateAlertRuleRequest(
        @NonNull
        @NotEmpty Set<UUID> deviceIds,
        @NonNull MetricType metricType,
        @NonNull ThresholdType thresholdType,
        @Nullable Float thresholdValue,
        @NonNull SeverityLevel severity,
        @NonNull Boolean isEnabled,
        @Nullable String username) {
}
