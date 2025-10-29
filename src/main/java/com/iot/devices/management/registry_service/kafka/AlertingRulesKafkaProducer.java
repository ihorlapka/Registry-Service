package com.iot.devices.management.registry_service.kafka;

import com.iot.alerts.AlertRule;
import com.iot.devices.management.registry_service.kafka.properties.AlertingRulesKafkaProducerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static com.iot.devices.management.registry_service.controller.errors.AlertRulesException.AlertRuleNotSentException;

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

    public void sendOneBlocking(UUID key, @Nullable com.iot.devices.management.registry_service.persistence.model.AlertRule alertRule, Set<UUID> deviceIds) {
        try {
            final Future<RecordMetadata> metadataFuture = kafkaProducerRunner.send(key.toString(), mapAlertRule(alertRule, deviceIds));
            final RecordMetadata recordMetadata = metadataFuture.get();
            if (!recordMetadata.hasOffset()) {
                throw new RuntimeException("Unable to send message: " + alertRule);
            }
        } catch (Exception e) {
            log.error("Exception occurred during sending a message: {}", alertRule, e);
            throw new AlertRuleNotSentException(key);
        }
    }

    public void sendTransactionally(Map<com.iot.devices.management.registry_service.persistence.model.AlertRule, Set<UUID>> deviceIdsByAlertRule,
                                    Set<UUID> alertRulesToBeRemoved) {
        final Map<String, AlertRule> alertRulesByRuleId = deviceIdsByAlertRule.entrySet().stream()
                .map(entry -> mapAlertRule(entry.getKey(), entry.getValue()))
                .collect(toMap(AlertRule::getRuleId, Function.identity()));

        alertRulesToBeRemoved.forEach(alertRuleIdToRemove -> alertRulesByRuleId.put(alertRuleIdToRemove.toString(), null));
        kafkaProducerRunner.sendTransactionally(alertRulesByRuleId);
    }

    private AlertRule mapAlertRule(com.iot.devices.management.registry_service.persistence.model.AlertRule alertRule, Set<UUID> deviceIds) {
        if (alertRule == null) {
            return null;
        }
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
