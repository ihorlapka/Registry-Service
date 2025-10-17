package com.iot.devices.management.registry_service.controller.dto;

import com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType;

import java.util.UUID;

public record AlertRuleDto(UUID ruleId,
                           UUID deviceId,
                           MetricType metricType,
                           ThresholdType thresholdType,
                           Float thresholdValue,
                           SeverityLevel severity,
                           boolean isEnabled) {
}
