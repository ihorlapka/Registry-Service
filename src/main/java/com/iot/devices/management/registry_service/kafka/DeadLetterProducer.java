package com.iot.devices.management.registry_service.kafka;

import com.iot.devices.management.registry_service.kafka.properties.DeadLetterKafkaProducerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterProducer {

    private final KafkaProducerRunner<String> kafkaProducerRunner;


    public DeadLetterProducer(DeadLetterKafkaProducerProperties producerProperties, MeterRegistry meterRegistry) {
        this.kafkaProducerRunner = new KafkaProducerRunner<>(
                producerProperties.getProperties(),
                producerProperties.getExecutorTerminationTimeoutMs(),
                meterRegistry,
                producerProperties.getTopic());
    }

    public void send(String key, SpecificRecord telemetry) {
        kafkaProducerRunner.send(key, telemetry);
    }
}
