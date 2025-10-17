package com.iot.devices.management.registry_service.controller.errors;

import java.util.UUID;

public class AlertRuleNotFoundException extends RuntimeException {

    public AlertRuleNotFoundException(UUID ruleId) {
        super("No alert rule present with ruleId=" + ruleId);
    }
}
