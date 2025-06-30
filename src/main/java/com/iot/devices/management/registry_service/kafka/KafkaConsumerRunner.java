package com.iot.devices.management.registry_service.kafka;

import com.iot.devices.management.registry_service.kafka.properties.KafkaConsumerProperties;
import com.iot.devices.management.registry_service.persistence.ParallelDevicePatcher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
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

    private final ParallelDevicePatcher parallelDevicePatcher;
    private final KafkaConsumerProperties consumerProperties;

    private KafkaConsumer<String, String> kafkaConsumer;


    @PostConstruct
    public void pollMessages() {
        executorService.submit(this::runConsumer);
    }

    private void runConsumer() {
        while (!isShutdown) {
            try {
                subscribe();
                final ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.of(consumerProperties.getPollTimeoutMs(), MILLIS));
                final Map<String, ConsumerRecord<String, String>> filteredRecordById = filterDeprecatedRecords(records);
                final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = parallelDevicePatcher.patch(filteredRecordById);
                if (!offsetsToCommit.isEmpty()) {
                    kafkaConsumer.commitAsync(offsetsToCommit, getOffsetCommitCallback());
                }
            } catch (Exception e) {
                log.error("Unexpected exception in consumer loop ", e);
            } finally {
                closeConsumer();
            }
        }
        log.info("Exited kafka consumer loop");
    }

    private void subscribe() {
        kafkaConsumer = new KafkaConsumer<>(consumerProperties.getProperties());
        kafkaConsumer.subscribe(List.of(consumerProperties.getTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                log.info("Partitions revoked");
                partitions.clear();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                log.info("Partitions assigned: {}", collection);
                partitions.addAll(collection);
            }
        });
    }

    private Map<String, ConsumerRecord<String, String>> filterDeprecatedRecords(ConsumerRecords<String, String> records) {
        final Map<String, ConsumerRecord<String, String>> filteredRecords = new ConcurrentHashMap<>();
        for (ConsumerRecord<String, String> record : records) {
            filteredRecords.compute(record.key(), (k, v) -> {
                if (v == null) {
                    return record;
                }
                else if (record.timestamp() > v.timestamp()) {
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
                System.out.println("Async commit successful for offsets: " + committedOffsets);
            } else {
                System.err.println("Async commit failed for offsets: " + committedOffsets + ". Error: " + ex.getMessage());
                if (ex instanceof KafkaException) {
                    System.err.println("Kafka commit error: " + ex.getMessage());
                }
            }
        };
    }

    private void closeConsumer() {
        try {
            log.warn("Closing kafka consumer");
            kafkaConsumer.close();
            log.info("Kafka consumer is closed");
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
            System.out.println("Kafka consumer executor shutdown forced");
        } else {
            System.out.println("Kafka consumer executor shutdown gracefully");
        }
    }
}
