package com.iot.devices.management.registry_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PrometheusKpiLogger implements KpiMetricLogger {

    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final ConcurrentMap<String, Counter> notUpdatedDevicesCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> severalUpdatedDevicesCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DistributionSummary> deviceUpdatingTimeSummaries = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;
    private final Counter retriesCounter;
    private final Counter nonRetriableErrorsCounter;

    public PrometheusKpiLogger(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.retriesCounter = Counter.builder("not_updated_devices_count")
                .description("The number of retries during patching device")
                .register(meterRegistry);

        this.nonRetriableErrorsCounter = Counter.builder("non_retriable_errors_count")
                .description("The number of non-retriable exceptions")
                .register(meterRegistry);

        Gauge.builder("parallel_persister_active_threads", activeThreads, AtomicInteger::get)
                .description("The number of threads currently executing tasks")
                .register(meterRegistry);
    }

    @Override
    public void incNotUpdatedDevices(String deviceType) {
        notUpdatedDevicesCounters.computeIfAbsent(deviceType, (k) ->
                        Counter.builder("not_updated_devices_count")
                                .description("The number of persisting queries due to which there were no updated devices")
                                .tag("deviceType", k)
                                .register(meterRegistry))
                .increment();
    }

    @Override
    public void recordDeviceUpdatingTime(String deviceType, long timeMs) {
        deviceUpdatingTimeSummaries.computeIfAbsent(deviceType, k ->
                        DistributionSummary.builder("device_updating_time")
                                .description("The time during which patch operation finished successfully")
                                .tag("deviceType", deviceType)
                                .register(meterRegistry))
                .record(timeMs);
    }

    @Override
    public void incSeveralUpdatedDevices(String deviceType) {
        severalUpdatedDevicesCounters.computeIfAbsent(deviceType, (k) ->
                        Counter.builder("several_updated_devices_count")
                                .description("The number of patch operations due to which several records were updated")
                                .tag("deviceType", k)
                                .register(meterRegistry))
                .increment();
    }

    @Override
    public void incRetriesCount() {
        retriesCounter.increment();
    }

    @Override
    public void incNonRetriableErrorsCount() {
        nonRetriableErrorsCounter.increment();
    }

    @Override
    public void incActiveThreadsInParallelPatcher(int activeThreadsCount) {
        activeThreads.set(activeThreadsCount);
    }
}
