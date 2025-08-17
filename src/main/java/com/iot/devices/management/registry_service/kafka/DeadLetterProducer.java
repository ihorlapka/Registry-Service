package com.iot.devices.management.registry_service.kafka;

import com.iot.devices.management.registry_service.kafka.properties.KafkaProducerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterProducer {

    private final KafkaProducerProperties producerProperties;
    private final MeterRegistry meterRegistry;

    private KafkaProducer<String, SpecificRecord> kafkaProducer;
    private KafkaClientMetrics kafkaClientMetrics;

    @PostConstruct
    private void init() {
        Properties properties = new Properties();
        properties.putAll(producerProperties.getProperties());
        kafkaProducer = new KafkaProducer<>(properties);
        kafkaClientMetrics = new KafkaClientMetrics(kafkaProducer);
        kafkaClientMetrics.bindTo(meterRegistry);
    }

    public void send(String key, SpecificRecord telemetry) {
        log.info("Sending to dead-letter-topic events={}", telemetry);
        final ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>(producerProperties.getTopic(), key, telemetry);
        kafkaProducer.send(record, getCallback(telemetry));
    }

    private Callback getCallback(SpecificRecord telemetry) {
        return (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send telemetry to Kafka dead-letter topic: telemetry={}, error={}", telemetry, exception.getMessage(), exception);
            } else {
                log.debug("Successfully sent telemetry to dead-letter topic: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        };
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down DeadLetterProducer...");
        if (kafkaProducer != null) {
            try {
                kafkaProducer.flush();
                kafkaProducer.close(Duration.ofMillis(producerProperties.getExecutorTerminationTimeoutMs()));
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
