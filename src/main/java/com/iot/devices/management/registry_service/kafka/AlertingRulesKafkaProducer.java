package com.iot.devices.management.registry_service.kafka;

import com.iot.alerts.AlertRule;
import com.iot.alerts.RuleCompoundKey;
import com.iot.devices.management.registry_service.controller.errors.AlertRuleNotSentException;
import com.iot.devices.management.registry_service.kafka.properties.AlertingRulesKafkaProducerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

@Slf4j
@Component
public class AlertingRulesKafkaProducer {

    private final KafkaProducerRunner<RuleCompoundKey> kafkaProducerRunner;


    public AlertingRulesKafkaProducer(AlertingRulesKafkaProducerProperties producerProperties, MeterRegistry meterRegistry) {
        this.kafkaProducerRunner = new KafkaProducerRunner<>(
                producerProperties.getProperties(),
                producerProperties.getExecutorTerminationTimeoutMs(),
                meterRegistry,
                producerProperties.getTopic());
    }

    public void send(RuleCompoundKey key, AlertRule alertRule) {
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
            throw new AlertRuleNotSentException(key.getRuleId());
        }
    }
}
