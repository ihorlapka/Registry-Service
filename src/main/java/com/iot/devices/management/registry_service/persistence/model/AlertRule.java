package com.iot.devices.management.registry_service.persistence.model;

import com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel;
import com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

import static jakarta.persistence.GenerationType.AUTO;

@Getter
@Setter
@Entity
@Table(name = "alert_rules")
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(columnDefinition = "uuid", name = "rule_id", updatable = false, nullable = false)
    private UUID ruleId;

    @Column(name = "metric_type", columnDefinition = "metric_types", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MetricType metricType;

    @Column(name = "threshold_type", columnDefinition = "threshold_types", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ThresholdType thresholdType;

    @Column(name = "threshold_value")
    private Float thresholdValue;

    @Column(name = "severity_level", columnDefinition = "severity_levels", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SeverityLevel severity;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    @Column(name = "username")
    private String username;
}
