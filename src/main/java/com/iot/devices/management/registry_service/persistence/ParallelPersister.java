package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ParallelPersister {

    private static final String PROPERTIES_PREFIX = "parallel-persister";

    private final ExecutorService executorService;
    private final DeviceService deviceService;

    public ParallelPersister(@Value("${" + PROPERTIES_PREFIX + "threads-amount}") int threadsAmount,
                             DeviceService deviceService) {
        this.executorService = Executors.newFixedThreadPool(threadsAmount);
        this.deviceService = deviceService;
    }


    public void persistInParallel(Set<String> dataSet) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String data : dataSet) {
            futures.add(CompletableFuture.runAsync(() -> persistWithRetries(data), executorService));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenAccept(v -> log.debug("All futures are done"))
                .join();
    }

    @Retry(name = "updateDeviceRetry", fallbackMethod = "updateFallback")
    private void persistWithRetries(String data) {
        deviceService.patch(data);
    }

    public void updateFallback(String data, Throwable t) {
        log.error("Retry failed for: {}", data, t);
        throw new RuntimeException("Update failed after retries!");
    }
}
