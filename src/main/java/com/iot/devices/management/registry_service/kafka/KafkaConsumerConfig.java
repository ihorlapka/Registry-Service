package com.iot.devices.management.registry_service.kafka;

import com.iot.devices.management.registry_service.kafka.properties.KafkaConsumerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(KafkaConsumerProperties.class)
@Configuration
public class KafkaConsumerConfig {
}
