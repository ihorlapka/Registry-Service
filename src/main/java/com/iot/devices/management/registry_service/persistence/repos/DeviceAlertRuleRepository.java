package com.iot.devices.management.registry_service.persistence.repos;

import com.iot.devices.management.registry_service.persistence.model.AlertRule;
import com.iot.devices.management.registry_service.persistence.model.DeviceAlertRule;
import com.iot.devices.management.registry_service.persistence.model.DeviceAlertRuleKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface DeviceAlertRuleRepository extends JpaRepository<DeviceAlertRule, DeviceAlertRuleKey> {

    @Modifying
    @Query("DELETE FROM DeviceAlertRule dar WHERE dar.id IN :ids")
    int removeAllByIds(@Param("ids") List<DeviceAlertRuleKey> ids);

    @EntityGraph(attributePaths = {"device", "alertRule"})
    Set<DeviceAlertRule> findAllByDeviceId(@Param("deviceId") UUID deviceId);

    @Query("""
            SELECT dar FROM DeviceAlertRule dar
            JOIN FETCH dar.device
            WHERE dar.alertRule.ruleId = :ruleId
            """)
    Set<DeviceAlertRule> findAllByAlertRuleWithDevices(@Param("ruleId") UUID alertRuleId);

    Set<DeviceAlertRule> findByAlertRule(AlertRule alertRule);

    @EntityGraph(attributePaths = {"device", "alertRule"})
    Set<DeviceAlertRule> findAllByAlertRuleIn(List<AlertRule> alertRules);
}
