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
    private final AtomicInteger recordsInOnePoll = new AtomicInteger(0);
    private final ConcurrentMap<String, Counter> notUpdatedDevicesCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> severalUpdatedDevicesCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> nonRetriableErrorsCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DistributionSummary> deviceUpdatingTimeSummaries = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;
    private final Counter retriesCounter;

    public PrometheusKpiLogger(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.retriesCounter = Counter.builder("rs_patch_retries_count")
                .description("The number of retries during patching device")
                .register(meterRegistry);

        Gauge.builder("rs_records_per_poll_gauge", recordsInOnePoll, AtomicInteger::get)
                .description("The number of records received in one poll")
                .register(meterRegistry);

        Gauge.builder("rs_parallel_persister_active_threads", activeThreads, AtomicInteger::get)
                .description("The number of threads currently executing tasks")
                .register(meterRegistry);
    }

    @Override
    public void incNotUpdatedDevices(String deviceType) {
        notUpdatedDevicesCounters.computeIfAbsent(deviceType, (k) ->
                        Counter.builder("rs_not_updated_devices_count")
                                .description("The number of persisting queries due to which there were no updated devices")
                                .tag("deviceType", k)
                                .register(meterRegistry))
                .increment();
    }

    @Override
    public void recordDeviceUpdatingTime(String deviceType, long timeMs) {
        deviceUpdatingTimeSummaries.computeIfAbsent(deviceType, k ->
                        DistributionSummary.builder("rs_device_updating_time")
                                .description("The time during which patch operation finished successfully")
                                .tag("deviceType", deviceType)
                                .publishPercentiles(0.5, 0.9, 0.99)
                                .register(meterRegistry))
                .record(timeMs);
    }

    @Override
    public void incSeveralUpdatedDevices(String deviceType) {
        severalUpdatedDevicesCounters.computeIfAbsent(deviceType, (k) ->
                        Counter.builder("rs_several_updated_devices_count")
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
    public void incNonRetriableErrorsCount(String errorName) {
        nonRetriableErrorsCounters.computeIfAbsent(errorName, (error) ->
                        Counter.builder("rs_non_retriable_errors_count")
                                .description("The number of non-retriable exceptions")
                                .tag("error", error)
                                .register(meterRegistry))
                .increment();
    }

    @Override
    public void recordActiveThreadsInParallelPatcher(int activeThreadsCount) {
        activeThreads.set(activeThreadsCount);
    }

    @Override
    public void recordRecordsInOnePoll(int recordsCount) {
        recordsInOnePoll.set(recordsCount);
    }
}
