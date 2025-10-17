package com.iot.devices.management.registry_service.persistence.services;

import com.iot.alerts.RuleCompoundKey;
import com.iot.devices.management.registry_service.controller.errors.AlertRuleNotFoundException;
import com.iot.devices.management.registry_service.controller.util.CreateAlertRuleRequest;
import com.iot.devices.management.registry_service.controller.util.PatchAlertRuleRequest;
import com.iot.devices.management.registry_service.kafka.AlertingRulesKafkaProducer;
import com.iot.devices.management.registry_service.persistence.model.AlertRule;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.repos.AlertRulesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertRuleService {

    private final AlertRulesRepository alertRulesRepository;
    private final AlertingRulesKafkaProducer alertRulesProducer;


    @Transactional
    public AlertRule saveAndSend(CreateAlertRuleRequest request, User user) {
        try {
            final AlertRule saved = alertRulesRepository.save(mapNewAlertRule(request, user));
            alertRulesProducer.send(buildKey(saved), buildAlertRuleMessage(saved));
            return saved;
        } catch (RuntimeException e) {
            log.warn("Unable to save or send Alert Rule, transaction will be rolled back if it was committed");
            throw e;
        }
    }

    //dirty checking
    @Transactional
    public AlertRule patch(PatchAlertRuleRequest request, @Nullable User user) {
        Optional<AlertRule> alertRule = alertRulesRepository.findById(request.ruleId());
        if (alertRule.isEmpty()) {
            throw new AlertRuleNotFoundException(request.ruleId());
        }
        try {
            final AlertRule patched = patchAlertRule(request, alertRule.get(), user);
            alertRulesProducer.send(buildKey(patched), buildAlertRuleMessage(patched));
            return patched;
        } catch (RuntimeException e) {
            log.warn("Unable to patch or send Alert Rule, transaction will be rolled back if it was committed");
            throw e;
        }
    }

    @Transactional
    public void removeAndSendTombstone(UUID ruleId) {
        final Optional<AlertRule> alertRule = alertRulesRepository.findById(ruleId);
        if (alertRule.isPresent()) {
            try {
                alertRulesRepository.deleteById(ruleId);
                alertRulesProducer.send(buildKey(alertRule.get()), null);
            } catch (RuntimeException e) {
                log.warn("Unable to delete or send Alert Rule, transaction will be rolled back if it was committed");
                throw e;
            }
        } else {
            log.warn("No alert rule present for removing, ruleId={}", ruleId);
        }
    }

    public List<AlertRule> findRulesByUsername(String username) {
        return alertRulesRepository.findAlertRulesByUsername(username);
    }

    private RuleCompoundKey buildKey(AlertRule alertRule) {
        return RuleCompoundKey.newBuilder()
                .setRuleId(alertRule.getRuleId().toString())
                .setDeviceId(alertRule.getDeviceId().toString())
                .build();
    }

    private com.iot.alerts.AlertRule buildAlertRuleMessage(AlertRule alertRule) {
        return com.iot.alerts.AlertRule.newBuilder()
                .setRuleId(alertRule.getRuleId().toString())
                .setDeviceId(alertRule.getDeviceId().toString())
                .setMetricName(com.iot.alerts.MetricType.valueOf(alertRule.getMetricType().name()))
                .setThresholdType(com.iot.alerts.ThresholdType.valueOf(alertRule.getThresholdType().name()))
                .setThresholdValue(alertRule.getThresholdValue())
                .setSeverity(com.iot.alerts.SeverityLevel.valueOf(alertRule.getSeverity().name()))
                .setIsEnabled(alertRule.isEnabled())
                .build();
    }

    private AlertRule mapNewAlertRule(CreateAlertRuleRequest request, @Nullable User user) {
        return new AlertRule(null, request.deviceId(), request.metricType(), request.thresholdType(),
                request.thresholdValue(), request.severity(), request.isEnabled(), getUsername(user));
    }

    private AlertRule patchAlertRule(PatchAlertRuleRequest request, AlertRule alertRule, @Nullable User user) {
        ofNullable(request.deviceId()).ifPresent(alertRule::setDeviceId);
        ofNullable(request.metricType()).ifPresent(alertRule::setMetricType);
        ofNullable(request.thresholdType()).ifPresent(alertRule::setThresholdType);
        ofNullable(request.thresholdValue()).ifPresent(alertRule::setThresholdValue);
        ofNullable(request.severity()).ifPresent(alertRule::setSeverity);
        ofNullable(request.isEnabled()).ifPresent(alertRule::setEnabled);
        ofNullable(getUsername(user)).ifPresent(alertRule::setUsername);
        return alertRule;
    }

    private String getUsername(User user) {
        return ofNullable(user).map(User::getUsername).orElse(null);
    }
}
