package com.iot.devices.management.registry_service.metrics;

public interface KpiMetricLogger {
    void incNotUpdatedDevices(String deviceType);
    void recordDeviceUpdatingTime(String deviceType, long l);
    void incSeveralUpdatedDevices(String deviceType);
    void incRetriesCount();
    void incNonRetriableErrorsCount(String errorName);
    void recordActiveThreadsInParallelPatcher(int activeThreadsCount);
    void recordRecordsInOnePoll(int recordsCount);
}
