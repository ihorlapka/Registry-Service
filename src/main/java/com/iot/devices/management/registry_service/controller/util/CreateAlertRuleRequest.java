package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType;
import lombok.NonNull;

import java.util.UUID;

public record CreateAlertRuleRequest(
        @NonNull
        UUID deviceId,
        @NonNull
        MetricType metricType,
        @NonNull
        ThresholdType thresholdType,
        Float thresholdValue,
        @NonNull
        SeverityLevel severity,
        @NonNull
        Boolean isEnabled,
        String username
) {
}
