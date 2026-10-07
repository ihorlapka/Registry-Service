package com.iot.devices.management.registry_service.persistence.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "devices_alert_rules")
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlertRule {

    @EmbeddedId
    private DeviceAlertRuleKey id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("deviceId")
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("ruleId")
    @JoinColumn(name = "rule_id", nullable = false)
    private AlertRule alertRule;
}
