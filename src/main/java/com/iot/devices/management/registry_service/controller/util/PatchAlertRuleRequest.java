package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType;
import lombok.Builder;
import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.util.Set;
import java.util.UUID;

@Builder(toBuilder = true)
public record PatchAlertRuleRequest(
        @NonNull UUID ruleId,
        @Nullable Set<UUID> deviceIdsToAdd,
        @Nullable Set<UUID> deviceIdsToRemove,
        @Nullable MetricType metricType,
        @Nullable ThresholdType thresholdType,
        @Nullable Float thresholdValue,
        @Nullable SeverityLevel severity,
        @Nullable Boolean isEnabled,
        @Nullable String username) {
}
