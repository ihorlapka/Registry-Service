package com.iot.devices.management.registry_service.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

@Slf4j
public class KafkaProducerRunner<T> {

    private final long executorTerminationTimeoutMs;
    private final KafkaProducer<T, SpecificRecord> kafkaProducer;
    private final KafkaClientMetrics kafkaClientMetrics;
    private final String topic;

    public KafkaProducerRunner(Map<String, String> producerProperties, long executorTerminationTimeoutMs,
                               MeterRegistry meterRegistry, String topic) {
        this.executorTerminationTimeoutMs = executorTerminationTimeoutMs;
        this.topic = topic;
        this.kafkaProducer = new KafkaProducer<>(getProperties(producerProperties));
        this.kafkaClientMetrics = new KafkaClientMetrics(kafkaProducer);
        this.kafkaClientMetrics.bindTo(meterRegistry);
    }


    public Future<RecordMetadata> send(T key, SpecificRecord telemetry) {
        log.info("Sending to topic={}, events={}", topic, telemetry);
        final ProducerRecord<T, SpecificRecord> record = new ProducerRecord<>(topic, key, telemetry);
        return kafkaProducer.send(record, getCallback(telemetry));
    }

    private Callback getCallback(SpecificRecord telemetry) {
        return (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send message to topic={}, telemetry={}, error={}", topic, telemetry, exception.getMessage(), exception);
            } else {
                log.debug("Successfully sent telemetry to topic={}, partition={}, offset={}", topic, metadata.partition(), metadata.offset());
            }
        };
    }

    private Properties getProperties(Map<String, String> producerProperties) {
        Properties properties = new Properties();
        properties.putAll(producerProperties);
        return properties;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down KafkaProducer...");
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
