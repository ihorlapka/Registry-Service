package com.iot.devices.management.registry_service.kafka.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.iot.devices.management.registry_service.kafka.properties.KafkaConsumerProperties.PROPERTIES_PREFIX;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(PROPERTIES_PREFIX)
@RequiredArgsConstructor
public class KafkaConsumerProperties {

    final static String PROPERTIES_PREFIX = "kafka.consumer";

    private Map<String, Object> properties = new HashMap<>();

    @Value("${" + PROPERTIES_PREFIX + ".topic}")
    private String topic;


    @Value("${" + PROPERTIES_PREFIX + ".poll-timeout-ms}")
    private Long pollTimeoutMs;

    @PostConstruct
    private void logProperties() {
        log.info("kafka consumer properties: {}", properties);
    }
}
