package com.iot.devices.management.registry_service.controller.errors;

import java.util.Set;
import java.util.UUID;

public class AlertRulesException {

    public static class AlertRuleNotFoundException extends RuntimeException {

        public AlertRuleNotFoundException(UUID ruleId) {
            super("No alert rule present with ruleId=" + ruleId);
        }
    }

    public static class AlertRuleNotSentException extends RuntimeException {

        public AlertRuleNotSentException(UUID ruleId) {
            super("Unable to send alert rule with ruleId=" + ruleId);
        }

        public AlertRuleNotSentException(Set<String> ruleIds) {
            super("Unable to send alert rules with ruleIds=[" + ruleIds + "]");
        }
    }

    public static class UnableToCreateAlertRuleException extends RuntimeException {

        public UnableToCreateAlertRuleException(String msg, Exception e) {
            super(msg, e);
        }
    }

    public static class UnableToPatchAlertRuleException extends RuntimeException {

        public UnableToPatchAlertRuleException(String msg, Exception e) {
            super(msg, e);
        }
    }

    public static class UnableToRemoveAlertRuleException extends RuntimeException {

        public UnableToRemoveAlertRuleException(String msg, Exception e) {
            super(msg, e);
        }
    }
}
