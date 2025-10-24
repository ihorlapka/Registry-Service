package com.iot.devices.management.registry_service.persistence.model;

import jakarta.persistence.Embeddable;
import lombok.Value;

import java.util.UUID;

@Embeddable
@Value(staticConstructor = "of")
public class DeviceAlertRuleKey {
    UUID deviceId;
    UUID ruleId;
}
