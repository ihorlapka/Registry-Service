package com.iot.devices.management.registry_service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterProducer {


    public void send(SpecificRecord value) {
        log.info("Sending to dead-letter-topic msg={}", value);
    }
}
