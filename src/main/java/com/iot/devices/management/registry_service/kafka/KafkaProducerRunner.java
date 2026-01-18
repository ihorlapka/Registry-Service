package com.iot.devices.management.registry_service.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;
import static com.iot.devices.management.registry_service.controller.errors.AlertRulesException.AlertRuleNotSentException;

@Slf4j
public class KafkaProducerRunner<K, V> {

    private final long executorTerminationTimeoutMs;
    private final KafkaProducer<K, V> kafkaProducer;
    private final KafkaClientMetrics kafkaClientMetrics;
    private final String topic;
    private final ExecutorService executorService;


    public KafkaProducerRunner(Map<String, String> producerProperties, long executorTerminationTimeoutMs,
                               MeterRegistry meterRegistry, String topic, boolean isTransactional) {
        this.executorTerminationTimeoutMs = executorTerminationTimeoutMs;
        this.topic = topic;
        this.kafkaProducer = new KafkaProducer<>(getProperties(producerProperties));
        this.kafkaClientMetrics = new KafkaClientMetrics(kafkaProducer);
        this.kafkaClientMetrics.bindTo(meterRegistry);
        this.executorService = isTransactional ? Executors.newSingleThreadExecutor() : null;
    }

    public void initTransactions() {
        kafkaProducer.initTransactions();
    }

    public void send(K key, V value) {
        log.info("Sending to topic={}, key={}, message={}", topic, key, value);
        final ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, value);
        kafkaProducer.send(record, getCallback(value));
    }

    public void sendTransactionally(Map<K, V> alertRulesByRuleId) {
        executorService.submit(() -> doSendTransactionally(alertRulesByRuleId));
    }

    public void doSendTransactionally(Map<K, V> alertRulesByRuleId) {
        try {
            kafkaProducer.beginTransaction();
            for (Map.Entry<K,V> entry : alertRulesByRuleId.entrySet()) {
                final ProducerRecord<K, V> record = new ProducerRecord<>(topic, entry.getKey(), entry.getValue());
                kafkaProducer.send(record, getCallback(entry.getValue()));
            }
            kafkaProducer.commitTransaction();
            log.info("messages were sent to topic={}, {}", topic, alertRulesByRuleId.values());
        } catch (Exception e) {
            try {
                kafkaProducer.abortTransaction();
            } catch (Exception ex) {
                log.error("Failed to abort transaction after a general KafkaException", ex);
            }
            final Set<String> keys = alertRulesByRuleId.keySet().stream()
                    .map(Object::toString)
                    .collect(toSet());
            log.error("Kafka transaction failed and aborted: {}", keys, e);
            throw new AlertRuleNotSentException(keys);
        }
    }

    private Callback getCallback(V message) {
        return (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send record to topic={}, message={}, error={}", topic, message, exception.getMessage(), exception);
            } else {
                log.debug("Successfully sent record to topic={}, partition={}, offset={}", topic, metadata.partition(), metadata.offset());
            }
        };
    }

    private Properties getProperties(Map<String, String> producerProperties) {
        Properties properties = new Properties();
        properties.putAll(producerProperties);
        return properties;
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Shutting down KafkaProducer...");
        if (executorService != null) {
            executorService.shutdown();
            if (!executorService.awaitTermination(executorTerminationTimeoutMs, MILLISECONDS)) {
                executorService.shutdownNow();
                log.info("Kafka Producer executor shutdown forced");
            } else {
                log.info("Kafka Producer executor shutdown gracefully");
            }
        }
        if (kafkaProducer != null) {
            try {
                kafkaProducer.flush();
                kafkaProducer.close(Duration.ofMillis(executorTerminationTimeoutMs));
                log.info("KafkaProducer closed successfully.");
            } catch (Exception e) {
                log.warn("Exception during KafkaProducer shutdown: {}", e.getMessage(), e);
            }
        }
        if (kafkaClientMetrics != null) {
            kafkaClientMetrics.close();
            log.info("KafkaClientMetrics are closed");
        }
    }
}
