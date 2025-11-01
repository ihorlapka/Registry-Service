package com.iot.devices.management.registry_service.kafka;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.iot.alerts.AlertRule;
import com.iot.devices.management.registry_service.kafka.properties.AlertingRulesKafkaProducerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
public class AlertingRulesKafkaProducer {

    private final KafkaProducerRunner<String, AlertRule> kafkaProducerRunner;


    public AlertingRulesKafkaProducer(AlertingRulesKafkaProducerProperties producerProperties, MeterRegistry meterRegistry) {
        this.kafkaProducerRunner = new KafkaProducerRunner<>(
                producerProperties.getProperties(),
                producerProperties.getExecutorTerminationTimeoutMs(),
                meterRegistry,
                producerProperties.getTopic());
        kafkaProducerRunner.initTransactions();
    }

    public void sendTransactionally(Map<com.iot.devices.management.registry_service.persistence.model.AlertRule, Set<UUID>> deviceIdsByAlertRule,
                                    Set<UUID> alertRulesToBeRemoved) {
        final Set<UUID> intersection = Sets.intersection(deviceIdsByAlertRule.keySet().stream()
                .map(com.iot.devices.management.registry_service.persistence.model.AlertRule::getRuleId)
                .collect(toSet()), alertRulesToBeRemoved);
        Preconditions.checkArgument(intersection.isEmpty(), "The same alertRuleIds are present in add and remove params! %s", intersection);

        final Map<String, AlertRule> alertRulesByRuleId = deviceIdsByAlertRule.entrySet().stream()
                .map(entry -> mapAlertRule(entry.getKey(), entry.getValue()))
                .collect(toMap(AlertRule::getRuleId, Function.identity()));

        alertRulesToBeRemoved.forEach(alertRuleIdToRemove -> alertRulesByRuleId.put(alertRuleIdToRemove.toString(), null));
        kafkaProducerRunner.sendTransactionally(alertRulesByRuleId);
    }

    private AlertRule mapAlertRule(com.iot.devices.management.registry_service.persistence.model.AlertRule alertRule, Set<UUID> deviceIds) {
        return AlertRule.newBuilder()
                .setRuleId(alertRule.getRuleId().toString())
                .setDeviceIds(deviceIds.stream().map(UUID::toString).toList())
                .setMetricName(com.iot.alerts.MetricType.valueOf(alertRule.getMetricType().name()))
                .setThresholdType(com.iot.alerts.ThresholdType.valueOf(alertRule.getThresholdType().name()))
                .setThresholdValue(alertRule.getThresholdValue())
                .setSeverity(com.iot.alerts.SeverityLevel.valueOf(alertRule.getSeverity().name()))
                .setIsEnabled(alertRule.isEnabled())
                .build();
    }
}
