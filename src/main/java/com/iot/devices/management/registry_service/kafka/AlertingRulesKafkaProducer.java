package com.iot.devices.management.registry_service.kafka;

import com.iot.alerts.AlertRule;
import com.iot.devices.management.registry_service.kafka.properties.AlertingRulesKafkaProducerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

import static java.util.stream.Collectors.toSet;
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

    public void sendOne(String key, AlertRule alertRule) {
        try {
            final Future<RecordMetadata> metadataFuture = kafkaProducerRunner.send(key, alertRule);
            RecordMetadata recordMetadata = metadataFuture.get();
            if (recordMetadata.hasOffset()) {
                log.info("Created Alert Rule with key: {}", key);
            } else {
                throw new RuntimeException("Unable to send message: " + alertRule);
            }
        } catch (Exception e) {
            log.error("Exception occurred during sending a message: {}", alertRule, e);
            throw new AlertRuleNotSentException(key);
        }
    }

    public void sendTransactionallyCreate(List<com.iot.devices.management.registry_service.persistence.model.AlertRule> alertRules, List<String> deviceIds) {
        final Set<AlertRule> mappedAlertRules = alertRules.stream()
                .map(alertRule -> mapAlertRule(alertRule, deviceIds))
                .collect(toSet());
        kafkaProducerRunner.sendTransactionally(mappedAlertRules, AlertRule::getRuleId);
    }

    public void sendTransactionallyPatch(Map<com.iot.devices.management.registry_service.persistence.model.AlertRule, List<UUID>> deviceIdsByAlertRule) {
        final Set<AlertRule> mappedAlertRules = deviceIdsByAlertRule.entrySet().stream()
                .map(entry -> mapAlertRule(entry.getKey(), entry.getValue().stream().map(UUID::toString).toList()))
                .collect(toSet());
        kafkaProducerRunner.sendTransactionally(mappedAlertRules, AlertRule::getRuleId);
    }

    private AlertRule mapAlertRule(com.iot.devices.management.registry_service.persistence.model.AlertRule alertRule, List<String> deviceIds) {
        return AlertRule.newBuilder()
                .setRuleId(alertRule.getRuleId().toString())
                .setDeviceIds(deviceIds)
                .setMetricName(com.iot.alerts.MetricType.valueOf(alertRule.getMetricType().name()))
                .setThresholdType(com.iot.alerts.ThresholdType.valueOf(alertRule.getThresholdType().name()))
                .setThresholdValue(alertRule.getThresholdValue())
                .setSeverity(com.iot.alerts.SeverityLevel.valueOf(alertRule.getSeverity().name()))
                .setIsEnabled(alertRule.isEnabled())
                .build();
    }
}
