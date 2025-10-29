package com.iot.devices.management.registry_service.persistence.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;


@Getter
@Setter
@ToString
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlertRuleKey {
    private UUID deviceId;
    private UUID ruleId;
}
