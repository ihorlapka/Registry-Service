package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType;
import lombok.NonNull;

import java.util.UUID;

public record PatchAlertRuleRequest(
        @NonNull
        UUID ruleId,
        UUID deviceId,
        MetricType metricType,
        ThresholdType thresholdType,
        Float thresholdValue,
        SeverityLevel severity,
        Boolean isEnabled,
        String username) {
}
