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
    private Device device;

    @ManyToOne
    @MapsId("ruleId")
    private AlertRule alertRule;
}
