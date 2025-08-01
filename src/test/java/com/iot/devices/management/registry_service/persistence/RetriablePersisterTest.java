package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.*;
import com.iot.devices.management.registry_service.mapping.DoorSensorTelemetry;
import com.iot.devices.management.registry_service.metrics.KpiMetricLogger;
import com.iot.devices.management.registry_service.persistence.retry.RetriablePersister;
import com.iot.devices.management.registry_service.persistence.retry.RetryProperties;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.iot.devices.DoorState.OPEN;
import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
        classes = {
                RetriablePersister.class,
                RetryProperties.class
        }, properties = {"logging.level.com.iot.devices.management.registry_service.persistence=DEBUG"})
class RetriablePersisterTest {

    public static final String TOPIC = "topic";
    public static final String KEY = "key";

    @MockitoBean
    DeviceService deviceService;
    @MockitoBean
    KpiMetricLogger kpiMetricLogger;

    @Autowired
    RetriablePersister retriablePersister;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deviceService, kpiMetricLogger);
    }

    @Test
    void successAfterRetries() {
        when(deviceService.patchDoorSensorTelemetry(any(DoorSensorTelemetry.class)))
                .thenThrow(new CannotAcquireLockException("some test error 1"),
                        new CannotAcquireLockException("some test error 2"),
                        new CannotAcquireLockException("some test error 3"))
                .thenAnswer(x -> {
                    sleep(20);
                    return 1;
                });

        String deviceId1 = UUID.randomUUID().toString();
        Instant nowTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DoorSensor doorSensor = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        ConsumerRecord<String, SpecificRecord> record1 = new ConsumerRecord<>(TOPIC, 0, 0, KEY, doorSensor);
        retriablePersister.persistWithRetries(record1);
        verify(deviceService, times(4)).patchDoorSensorTelemetry(any());
        verify(kpiMetricLogger, times(3)).incRetriesCount();
        verify(kpiMetricLogger).recordDeviceUpdatingTime(anyString(), anyLong());
    }

    @Test
    void failedAfterRetries() {
        when(deviceService.patchDoorSensorTelemetry(any(DoorSensorTelemetry.class)))
                .thenThrow(new CannotAcquireLockException("some test error 1"),
                        new CannotAcquireLockException("some test error 2"),
                        new CannotAcquireLockException("some test error 3"),
                        new CannotAcquireLockException("some test error 4"),
                        new CannotAcquireLockException("some test error 5"));

        String deviceId1 = UUID.randomUUID().toString();
        Instant nowTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DoorSensor doorSensor = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        ConsumerRecord<String, SpecificRecord> record = new ConsumerRecord<>(TOPIC, 0, 0, KEY, doorSensor);
        Assertions.assertThrows(RuntimeException.class, () -> retriablePersister.persistWithRetries(record));
        verify(deviceService, times(5)).patchDoorSensorTelemetry(any());
        verify(kpiMetricLogger, times(5)).incRetriesCount();
    }

    @Test
    void nonRetriableError() {
        when(deviceService.patchDoorSensorTelemetry(any(DoorSensorTelemetry.class)))
                .thenThrow(new IllegalArgumentException("some test error 1"));

        String deviceId1 = UUID.randomUUID().toString();
        Instant nowTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DoorSensor doorSensor = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        ConsumerRecord<String, SpecificRecord> record = new ConsumerRecord<>(TOPIC, 0, 0, KEY, doorSensor);
        Assertions.assertThrows(RuntimeException.class, () -> retriablePersister.persistWithRetries(record));
        verify(deviceService, times(1)).patchDoorSensorTelemetry(any(DoorSensorTelemetry.class));
        verify(kpiMetricLogger).incNonRetriableErrorsCount();
    }
}