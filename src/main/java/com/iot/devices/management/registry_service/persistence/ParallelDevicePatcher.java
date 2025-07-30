package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.management.registry_service.kafka.DeadLetterProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class ParallelDevicePatcher {

    private static final String PROPERTIES_PREFIX = "parallel-persister";

    private final ExecutorService executorService;
    private final DeadLetterProducer deadLetterProducer;
    private final RetriablePersister retriablePersister;

    public ParallelDevicePatcher(@Value("${" + PROPERTIES_PREFIX + ".threads-amount}") int threadsAmount,
                                 DeadLetterProducer deadLetterProducer, RetriablePersister retriablePersister) {
        this.executorService = Executors.newFixedThreadPool(threadsAmount);
        this.deadLetterProducer = deadLetterProducer;
        this.retriablePersister = retriablePersister;
    }


    public Map<TopicPartition, OffsetAndMetadata> patch(Map<String, ConsumerRecord<String, SpecificRecord>> recordById) {
        final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new ConcurrentHashMap<>();
        final List<CompletableFuture<Void>> futures = new ArrayList<>(recordById.size());
        for (Map.Entry<String, ConsumerRecord<String, SpecificRecord>> entry : recordById.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                final ConsumerRecord<String, SpecificRecord> record = entry.getValue();
                long newOffsetToReadFrom = record.offset() + 1;
                try {
                    retriablePersister.persistWithRetries(record);
                    offsetsToCommit.compute(new TopicPartition(record.topic(), record.partition()), (k, v) -> {
                        if (v == null || v.offset() < newOffsetToReadFrom) {
                            return new OffsetAndMetadata(newOffsetToReadFrom);
                        } else {
                            return v;
                        }
                    });
                } catch (Exception e) {
                    if (!isRetriableException(e)) {
                        log.error("Failed to update device with id={} after retries, sending message to dead-letter-topic, offset={} will be committed",
                                record.value(), record.offset(), e);
                        deadLetterProducer.send(record.value());
                        offsetsToCommit.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset()));
                    } else {
                        log.error("Failed to update device with id={} after retries, offset={} will be retried after consumer restart",
                                record.value(), record.offset(), e);
                        throw new RuntimeException(e);
                    }
                }
            }, executorService));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return offsetsToCommit;
    }

    private boolean isRetriableException(Exception e) {
        return e instanceof SQLTransientException
                || e instanceof SQLRecoverableException
                || e instanceof TransientDataAccessException; //TODO: maybe there are more retriable exceptions
    }
}
