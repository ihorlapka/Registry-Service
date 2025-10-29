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

    @ManyToOne
    @MapsId("deviceId")
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne
    @MapsId("ruleId")
    @JoinColumn(name = "rule_id")
    private AlertRule alertRule;
}
