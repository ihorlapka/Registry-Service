package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

import java.util.UUID;

public record CreateAlertRuleRequest(
        @NonNull
        @NotBlank(message = "Device id is required")
        UUID deviceId,
        @NonNull
        @NotBlank(message = "Metric type is required")
        MetricType metricType,
        @NonNull
        @NotBlank(message = "Threshold type is required")
        ThresholdType thresholdType,
        Float thresholdValue,
        @NonNull
        @NotBlank(message = "Severity level is required")
        SeverityLevel severity,
        @NonNull
        @NotBlank(message = "IsEnabled param is required")
        Boolean isEnabled) {
}
