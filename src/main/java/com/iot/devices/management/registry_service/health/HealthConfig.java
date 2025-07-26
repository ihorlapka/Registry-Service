package com.iot.devices.management.registry_service.health;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
public class HealthConfig {

    @Bean
    public AtomicBoolean kafkaConsumerStatusMonitor() {
        return new AtomicBoolean();
    }
}
