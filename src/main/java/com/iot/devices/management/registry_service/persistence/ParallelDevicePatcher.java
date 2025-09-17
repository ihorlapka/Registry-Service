package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.management.registry_service.kafka.DeadLetterProducer;
import com.iot.devices.management.registry_service.metrics.KpiMetricLogger;
import com.iot.devices.management.registry_service.persistence.retry.RetriablePatcher;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@Component
public class ParallelDevicePatcher {

    private static final String PROPERTIES_PREFIX = "parallel.patcher";

    private final ThreadPoolExecutor executorService;
    private final int executorTerminationTimeMs;
    private final DeadLetterProducer deadLetterProducer;
    private final RetriablePatcher retriablePatcher;
    private final KpiMetricLogger kpiMetricLogger;

    public ParallelDevicePatcher(@Value("${" + PROPERTIES_PREFIX + ".threads.amount}") int threadsAmount,
                                 @Value("${" + PROPERTIES_PREFIX + ".threads.virtual}") boolean useVirtualThreads,
                                 @Value("${" + PROPERTIES_PREFIX + ".executor.termination.time.ms}") int executorTerminationTimeMs,
                                 DeadLetterProducer deadLetterProducer, RetriablePatcher retriablePatcher, KpiMetricLogger kpiMetricLogger) {
        this.executorService = (ThreadPoolExecutor) createExecutorService(threadsAmount, useVirtualThreads);
        this.executorTerminationTimeMs = executorTerminationTimeMs;
        this.deadLetterProducer = deadLetterProducer;
        this.retriablePatcher = retriablePatcher;
        this.kpiMetricLogger = kpiMetricLogger;
    }


    public Optional<OffsetAndMetadata> patch(Map<String, ConsumerRecord<String, SpecificRecord>> recordById) {
        final Set<OffsetAndMetadata> offsetsToCommit = new ConcurrentSkipListSet<>(comparingLong(OffsetAndMetadata::offset));
        final List<CompletableFuture<Void>> futures = new ArrayList<>(recordById.size());
        for (ConsumerRecord<String, SpecificRecord> record : sortRecordsByOffsets(recordById)) {
            futures.add(CompletableFuture.runAsync(() -> {
                final long newOffsetToReadFrom = record.offset() + 1;
                try {
                    retriablePatcher.patchWithRetries(record);
                    offsetsToCommit.add(new OffsetAndMetadata(newOffsetToReadFrom));
                } catch (SQLTransientException | SQLRecoverableException | TransientDataAccessException e) {
                    log.error("Failed to update device {} after retries, offset={} will be retried after consumer restart",
                            record.value(), record.offset(), e);
                    throw new CompletionException(e);
                } catch (NullPointerException | IllegalArgumentException | NonTransientDataAccessException e ) {
                    deadLetterProducer.send(record.key(), record.value());
                    log.error("Non-retriable error, failed to update {}, sending message to dead-letter-topic, offset={} will be committed",
                            record.value(), record.offset(), e);
                    offsetsToCommit.add(new OffsetAndMetadata(newOffsetToReadFrom));
                    kpiMetricLogger.incNonRetriableErrorsCount(e.getClass().getSimpleName());
                } catch (Exception e) {
                    log.error("Failed to patch device", e);
                    throw new CompletionException(e);
                }
            }, executorService));
        }
        kpiMetricLogger.recordActiveThreadsInParallelPatcher(executorService.getActiveCount());
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return offsetsToCommit.stream().max(comparingLong(OffsetAndMetadata::offset));
    }

    private ExecutorService createExecutorService(int threadsAmount, boolean useVirtualThreads) {
        return (useVirtualThreads) ?Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(threadsAmount);
    }

    private List<ConsumerRecord<String, SpecificRecord>> sortRecordsByOffsets(Map<String, ConsumerRecord<String, SpecificRecord>> recordById) {
        return recordById.values().stream()
                .sorted(comparingLong(ConsumerRecord::offset))
                .toList();
    }

    @PreDestroy
    private void shutdown() throws InterruptedException {
        executorService.shutdown();
        if (!executorService.awaitTermination(executorTerminationTimeMs, MILLISECONDS)) {
            executorService.shutdownNow();
            log.info("Executor shutdown forced");
        } else {
            log.info("Executor shutdown gracefully");
        }
    }
}
