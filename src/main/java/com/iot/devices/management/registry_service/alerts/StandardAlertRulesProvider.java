package com.iot.devices.management.registry_service.alerts;

import com.iot.devices.management.registry_service.persistence.model.AlertRule;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class StandardAlertRulesProvider {

    public Set<AlertRule> getAlertRules(DeviceType deviceType, User owner) {

        return Set.of();
    }
}
