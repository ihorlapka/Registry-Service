package com.iot.devices.management.registry_service.controller.errors;

public class AlertRuleNotSentException extends RuntimeException {

    public AlertRuleNotSentException(String ruleId) {
        super("Unable to send alert rule with ruleId=" + ruleId);
    }
}
