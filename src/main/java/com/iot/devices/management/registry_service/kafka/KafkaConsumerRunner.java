package com.iot.devices.management.registry_service.kafka;

import com.iot.devices.management.registry_service.kafka.properties.KafkaConsumerProperties;
import com.iot.devices.management.registry_service.persistence.ParallelDevicePatcher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerRunner {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Collection<TopicPartition> partitions = new ArrayList<>();
    private volatile boolean isShutdown = false;
    private volatile boolean isSubscribed = false;

    private final ParallelDevicePatcher parallelDevicePatcher;
    private final KafkaConsumerProperties consumerProperties;

    private KafkaConsumer<String, SpecificRecord> kafkaConsumer;


    @PostConstruct
    public void pollMessages() {
        executorService.submit(this::runConsumer);
    }

    private void runConsumer() {
        while (!isShutdown) {
            try {
                if (!isSubscribed) {
                    subscribe();
                }
                final ConsumerRecords<String, SpecificRecord> records = kafkaConsumer.poll(Duration.of(consumerProperties.getPollTimeoutMs(), MILLIS));
                final Map<String, ConsumerRecord<String, SpecificRecord>> filteredRecordById = filterDeprecatedRecords(records);
                final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = parallelDevicePatcher.patch(filteredRecordById);
                if (!offsetsToCommit.isEmpty()) {
                    kafkaConsumer.commitAsync(offsetsToCommit, getOffsetCommitCallback());
                }
            } catch (WakeupException e) {
                log.info("Consumer poll woken up");
            } catch (Exception e) {
                log.error("Unexpected exception in consumer loop ", e);
                closeConsumer();
            }
        }
        log.info("Exited kafka consumer loop");
        closeConsumer();
    }

    private void subscribe() {
        final Properties properties = new Properties(consumerProperties.getProperties().size());
        properties.putAll(consumerProperties.getProperties());
        kafkaConsumer = new KafkaConsumer<>(properties);
        kafkaConsumer.subscribe(List.of(consumerProperties.getTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                log.info("Partitions revoked");
                partitions.clear();
                isSubscribed = false;
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                log.info("Partitions assigned: {}", collection);
                partitions.addAll(collection);
                isSubscribed = true;
            }
        });
    }

    private Map<String, ConsumerRecord<String, SpecificRecord>> filterDeprecatedRecords(ConsumerRecords<String, SpecificRecord> records) {
        final Map<String, ConsumerRecord<String, SpecificRecord>> filteredRecords = new ConcurrentHashMap<>();
        for (ConsumerRecord<String, SpecificRecord> record : records) {
            filteredRecords.compute(record.key(), (k, v) -> {
                if (v == null) {
                    return record;
                } else if (record.timestamp() > v.timestamp()) {
                    log.debug("Current record is filtered as deprecated: {}", v.value());
                    return record;
                }
                log.debug("New Record is filtered as deprecated: {}", record.value());
                return v;
            });
        }
        return filteredRecords;
    }

    private OffsetCommitCallback getOffsetCommitCallback() {
        return (committedOffsets, ex) -> {
            if (ex == null) {
                log.info("Async commit successful for offsets: {}", committedOffsets);
            } else {
                log.error("Async commit failed for offsets: {}. Error: {}", committedOffsets, ex.getMessage());
                if (ex instanceof KafkaException) {
                    log.error("Kafka commit error: {}", ex.getMessage());
                }
            }
        };
    }

    private void closeConsumer() {
        try {
            if (kafkaConsumer != null) {
                log.warn("Closing kafka consumer");
                kafkaConsumer.close();
                log.info("Kafka consumer is closed");
                isSubscribed = false;
            }
            if (!isShutdown) {
                log.info("Waiting {} ms before consumer restart", consumerProperties.getRestartTimeoutMs());
                Thread.sleep(consumerProperties.getRestartTimeoutMs());
            }
        } catch (InterruptedException e) {
            log.error("Failed to wait for consumer restart because thread was interrupted", e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void shutdown() throws InterruptedException {
        isShutdown = true;
        executorService.shutdown();
        if (!executorService.awaitTermination(consumerProperties.getExecutorTerminationTimeoutMs(), MILLISECONDS)) {
            executorService.shutdownNow();
            log.info("Kafka consumer executor shutdown forced");
        } else {
            log.info("Kafka consumer executor shutdown gracefully");
        }
    }
}
