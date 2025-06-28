package com.iot.devices.management.registry_service.kafka;

import com.iot.devices.management.registry_service.kafka.properties.KafkaConsumerProperties;
import com.iot.devices.management.registry_service.persistence.ParallelPersister;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerRunner {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Collection<TopicPartition> partitions = new ArrayList<>();
    private volatile boolean isShutdown = false;

    private final ParallelPersister parallelPersister;
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
                final Set<String> dataSet = filterDeprecatedRecords(records);
                parallelPersister.persistInParallel(dataSet);

                kafkaConsumer.commitSync();
            } catch (Exception e) {
                log.error("Unexpected exception in consumer loop ", e);
            } finally {
                log.warn("Closing kafka consumer");
                kafkaConsumer.close();
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

    private Set<String> filterDeprecatedRecords(ConsumerRecords<String, String> records) {
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
        return filteredRecords.values().stream()
                .map(ConsumerRecord::value)
                .collect(toSet());
    }

    @PreDestroy
    private void shutdown() throws InterruptedException {
        isShutdown = true;
        executorService.shutdown();
        if (!executorService.awaitTermination(5L, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            System.out.println("Kafka consumer executor shutdown forced");
        } else {
            System.out.println("Kafka consumer executor shutdown gracefully");
        }
    }
}
