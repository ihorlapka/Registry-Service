package com.iot.devices.management.registry_service.persistence;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
public class RetryConfig {

    @Bean
    public RegistryEventConsumer<Retry> myRetryRegistryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                final Retry retry = entryAddedEvent.getAddedEntry();
                retry.getEventPublisher().onRetry(RetryConfig::log);
                log.info("Registered event listener for new Retry instance: {}", retry.getName());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemovedEvent) {
                log.info("Removed Retry instance: {}", entryRemovedEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
                Retry newRetry = entryReplacedEvent.getNewEntry();
                newRetry.getEventPublisher().onRetry(RetryConfig::log);
                log.info("Replaced Retry instance: {} with new configuration.", newRetry.getName());
            }
        };
    }

    private static void log(RetryOnRetryEvent event) {
        log.warn("Resilience4j Retry Event - Instance '{}': Call '{}' failed with exception: '{}'. Retrying...",
                event.getName(),
                event.getNumberOfRetryAttempts(),
                event.getLastThrowable().getMessage());
    }
}
