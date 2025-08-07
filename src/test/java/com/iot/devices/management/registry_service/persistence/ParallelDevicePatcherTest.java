package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.*;
import com.iot.devices.management.registry_service.kafka.DeadLetterProducer;
import com.iot.devices.management.registry_service.metrics.KpiMetricLogger;
import com.iot.devices.management.registry_service.persistence.retry.RetriablePatcher;
import com.iot.devices.management.registry_service.persistence.retry.RetryProperties;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.SQLTransientException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletionException;

import static com.iot.devices.DoorState.OPEN;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
        classes = {
                ParallelDevicePatcher.class,
                RetriablePatcher.class,
                RetryProperties.class
        },
        properties = {"logging.level.com.iot.devices.management.registry_service.persistence=DEBUG"})
class ParallelDevicePatcherTest {

    public static final String TOPIC = "topic";
    public static final String KEY = "key";

    @MockitoBean
    DeadLetterProducer deadLetterProducer;
    @MockitoBean
    DeviceService deviceService;
    @MockitoBean
    KpiMetricLogger kpiMetricLogger;

    @Autowired
    ParallelDevicePatcher parallelDevicePatcher;


    @BeforeEach
    void setUp() throws SQLTransientException {
        when(deviceService.patchDoorSensorTelemetry(any())).thenAnswer(x -> {
            sleep(20);
            return 1;
        });
        when(deviceService.patchThermostatTelemetry(any())).thenAnswer(x -> {
            sleep(10);
            return 1;
        });
        when(deviceService.patchSmartPlugTelemetry(any())).thenAnswer(x -> {
            sleep(45);
            return 1;
        });
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deviceService, deadLetterProducer, kpiMetricLogger);
    }

    @Test
    void allMessagesInOnePartition() {
        String deviceId1 = UUID.randomUUID().toString();
        String deviceId2 = UUID.randomUUID().toString();
        String deviceId3 = UUID.randomUUID().toString();

        Instant nowTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DoorSensor doorSensor = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        Thermostat thermostat = new Thermostat(deviceId2, 26.6f, 24.0f, 10.0f,
                ThermostatMode.COOL, DeviceStatus.ONLINE, "2.123v", nowTime);

        SmartPlug smartPlug = new SmartPlug(deviceId3, true, 230f, 227f, 99f,
                DeviceStatus.MAINTENANCE, null, nowTime.minus(5, ChronoUnit.MINUTES));

        ConsumerRecord<String, SpecificRecord> record1 = new ConsumerRecord<>(TOPIC, 0, 0, KEY, doorSensor);
        ConsumerRecord<String, SpecificRecord> record2 = new ConsumerRecord<>(TOPIC, 0, 1, KEY, thermostat);
        ConsumerRecord<String, SpecificRecord> record3 = new ConsumerRecord<>(TOPIC, 0, 2, KEY, smartPlug);

        Map<String, ConsumerRecord<String, SpecificRecord>> recordsById = new HashMap<>(3);
        recordsById.put(deviceId1, record1);
        recordsById.put(deviceId2, record2);
        recordsById.put(deviceId3, record3);

        Optional<OffsetAndMetadata> offsetsToCommit = parallelDevicePatcher.patch(recordsById);

        assertTrue(offsetsToCommit.isPresent());
        assertEquals(3, offsetsToCommit.get().offset());
        verify(deviceService).patchDoorSensorTelemetry(any());
        verify(deviceService).patchThermostatTelemetry(any());
        verify(deviceService).patchSmartPlugTelemetry(any());
        verify(kpiMetricLogger).incActiveThreadsInParallelPatcher(3);
        verify(kpiMetricLogger, times(3)).recordDeviceUpdatingTime(anyString(), anyLong());
    }

    @Test
    void severalPartitions() {
        String deviceId1 = UUID.randomUUID().toString();
        String deviceId2 = UUID.randomUUID().toString();
        String deviceId3 = UUID.randomUUID().toString();
        String deviceId4 = UUID.randomUUID().toString();
        String deviceId5 = UUID.randomUUID().toString();
        String deviceId6 = UUID.randomUUID().toString();

        Instant nowTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DoorSensor doorSensor1 = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        Thermostat thermostat2 = new Thermostat(deviceId2, 26.6f, 24.0f, 10.0f,
                ThermostatMode.COOL, DeviceStatus.ONLINE, "2.123v", nowTime);

        SmartPlug smartPlug3 = new SmartPlug(deviceId3, true, 230f, 227f, 99f,
                DeviceStatus.MAINTENANCE, null, nowTime.minus(5, ChronoUnit.MINUTES));

        DoorSensor doorSensor4 = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        Thermostat thermostat5 = new Thermostat(deviceId2, 26.6f, 24.0f, 10.0f,
                ThermostatMode.COOL, DeviceStatus.ONLINE, "2.123v", nowTime);

        SmartPlug smartPlug6 = new SmartPlug(deviceId3, true, 230f, 227f, 99f,
                DeviceStatus.MAINTENANCE, null, nowTime.minus(5, ChronoUnit.MINUTES));

        ConsumerRecord<String, SpecificRecord> record1 = new ConsumerRecord<>(TOPIC, 0, 100, KEY, doorSensor1);
        ConsumerRecord<String, SpecificRecord> record2 = new ConsumerRecord<>(TOPIC, 0, 101, KEY, thermostat2);
        ConsumerRecord<String, SpecificRecord> record3 = new ConsumerRecord<>(TOPIC, 1, 2, KEY, smartPlug3);
        ConsumerRecord<String, SpecificRecord> record4 = new ConsumerRecord<>(TOPIC, 2, 10, KEY, doorSensor4);
        ConsumerRecord<String, SpecificRecord> record5 = new ConsumerRecord<>(TOPIC, 2, 8, KEY, thermostat5);
        ConsumerRecord<String, SpecificRecord> record6 = new ConsumerRecord<>(TOPIC, 2, 11, KEY, smartPlug6);

        Map<String, ConsumerRecord<String, SpecificRecord>> recordsById1 = new HashMap<>(3);
        Map<String, ConsumerRecord<String, SpecificRecord>> recordsById2 = new HashMap<>(3);
        Map<String, ConsumerRecord<String, SpecificRecord>> recordsById3 = new HashMap<>(3);
        recordsById1.put(deviceId1, record1);
        recordsById1.put(deviceId2, record2);
        recordsById2.put(deviceId3, record3);
        recordsById3.put(deviceId4, record4);
        recordsById3.put(deviceId5, record5);
        recordsById3.put(deviceId6, record6);

        List<Map<String, ConsumerRecord<String, SpecificRecord>>> recordsMaps = new ArrayList<>();
        recordsMaps.add(recordsById1);
        recordsMaps.add(recordsById2);
        recordsMaps.add(recordsById3);

        Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>(3);

        for (int i = 0; i < recordsMaps.size(); i++) {
            Optional<OffsetAndMetadata> offsetToCommit = parallelDevicePatcher.patch(recordsMaps.get(i));
            assertTrue(offsetToCommit.isPresent());
            offsetsToCommit.put(new TopicPartition(TOPIC, i), offsetToCommit.get());
        }

        assertEquals(102, offsetsToCommit.get(new TopicPartition(TOPIC, 0)).offset());
        assertEquals(3, offsetsToCommit.get(new TopicPartition(TOPIC, 1)).offset());
        assertEquals(12, offsetsToCommit.get(new TopicPartition(TOPIC, 2)).offset());
        verify(deviceService, times(2)).patchDoorSensorTelemetry(any());
        verify(deviceService, times(2)).patchThermostatTelemetry(any());
        verify(deviceService, times(2)).patchSmartPlugTelemetry(any());
        verify(kpiMetricLogger, times(3)).incActiveThreadsInParallelPatcher(anyInt());
        verify(kpiMetricLogger, times(6)).recordDeviceUpdatingTime(anyString(), anyLong());
    }

    @Test
    void allMessagesInOnePartitionWithRetries() {
        when(deviceService.patchDoorSensorTelemetry(any()))
                .thenThrow(new CannotAcquireLockException("some test error 1"),
                        new CannotAcquireLockException("some test error 2"),
                        new CannotAcquireLockException("some test error 3"))
                .thenAnswer(x -> {
                    sleep(20);
                    return 1;
                });

        String deviceId1 = UUID.randomUUID().toString();
        String deviceId2 = UUID.randomUUID().toString();
        String deviceId3 = UUID.randomUUID().toString();

        Instant nowTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DoorSensor doorSensor = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        Thermostat thermostat = new Thermostat(deviceId2, 26.6f, 24.0f, 10.0f,
                ThermostatMode.COOL, DeviceStatus.ONLINE, "2.123v", nowTime);

        SmartPlug smartPlug = new SmartPlug(deviceId3, true, 230f, 227f, 99f,
                DeviceStatus.MAINTENANCE, null, nowTime.minus(5, ChronoUnit.MINUTES));

        ConsumerRecord<String, SpecificRecord> record1 = new ConsumerRecord<>(TOPIC, 0, 0, KEY, doorSensor);
        ConsumerRecord<String, SpecificRecord> record2 = new ConsumerRecord<>(TOPIC, 0, 1, KEY, thermostat);
        ConsumerRecord<String, SpecificRecord> record3 = new ConsumerRecord<>(TOPIC, 0, 2, KEY, smartPlug);

        Map<String, ConsumerRecord<String, SpecificRecord>> recordsById = new HashMap<>(3);
        recordsById.put(deviceId1, record1);
        recordsById.put(deviceId2, record2);
        recordsById.put(deviceId3, record3);
        Optional<OffsetAndMetadata> offsetsToCommit = parallelDevicePatcher.patch(recordsById);

        assertTrue(offsetsToCommit.isPresent());
        assertEquals(3, offsetsToCommit.get().offset());
        verify(deviceService, times(4)).patchDoorSensorTelemetry(any());
        verify(deviceService).patchThermostatTelemetry(any());
        verify(deviceService).patchSmartPlugTelemetry(any());
        verify(kpiMetricLogger).incActiveThreadsInParallelPatcher(anyInt());
        verify(kpiMetricLogger, times(3)).recordDeviceUpdatingTime(anyString(), anyLong());
        verify(kpiMetricLogger, times(3)).incRetriesCount();
    }

    @Test
    void allMessagesInOnePartitionFailedAfterRetries() {
        when(deviceService.patchDoorSensorTelemetry(any())).thenThrow(new QueryTimeoutException("some error"));

        String deviceId1 = UUID.randomUUID().toString();
        String deviceId2 = UUID.randomUUID().toString();
        String deviceId3 = UUID.randomUUID().toString();

        Instant nowTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DoorSensor doorSensor = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        Thermostat thermostat = new Thermostat(deviceId2, 26.6f, 24.0f, 10.0f,
                ThermostatMode.COOL, DeviceStatus.ONLINE, "2.123v", nowTime);

        SmartPlug smartPlug = new SmartPlug(deviceId3, true, 230f, 227f, 99f,
                DeviceStatus.MAINTENANCE, null, nowTime.minus(5, ChronoUnit.MINUTES));

        ConsumerRecord<String, SpecificRecord> record1 = new ConsumerRecord<>(TOPIC, 0, 0, KEY, doorSensor);
        ConsumerRecord<String, SpecificRecord> record2 = new ConsumerRecord<>(TOPIC, 0, 1, KEY, thermostat);
        ConsumerRecord<String, SpecificRecord> record3 = new ConsumerRecord<>(TOPIC, 0, 2, KEY, smartPlug);

        Map<String, ConsumerRecord<String, SpecificRecord>> recordsById = new HashMap<>(3);
        recordsById.put(deviceId1, record1);
        recordsById.put(deviceId2, record2);
        recordsById.put(deviceId3, record3);

        CompletionException exception = Assertions.assertThrows(CompletionException.class, () -> parallelDevicePatcher.patch(recordsById));
        assertInstanceOf(QueryTimeoutException.class, exception.getCause());

        verify(deviceService, times(5)).patchDoorSensorTelemetry(any());
        verify(deviceService).patchThermostatTelemetry(any());
        verify(deviceService).patchSmartPlugTelemetry(any());
        verify(kpiMetricLogger).incActiveThreadsInParallelPatcher(3);
        verify(kpiMetricLogger, times(2)).recordDeviceUpdatingTime(anyString(), anyLong());
        verify(kpiMetricLogger, times(5)).incRetriesCount();
    }

    @Test
    void allMessagesInOnePartitionNonRetriableError() {
        when(deviceService.patchDoorSensorTelemetry(any())).thenThrow(new NullPointerException("some error"));

        String deviceId1 = UUID.randomUUID().toString();
        String deviceId2 = UUID.randomUUID().toString();
        String deviceId3 = UUID.randomUUID().toString();

        Instant nowTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DoorSensor doorSensor = new DoorSensor(deviceId1, OPEN, 85, false,
                DeviceStatus.OFFLINE, nowTime, "1.0.2v", nowTime);

        Thermostat thermostat = new Thermostat(deviceId2, 26.6f, 24.0f, 10.0f,
                ThermostatMode.COOL, DeviceStatus.ONLINE, "2.123v", nowTime);

        SmartPlug smartPlug = new SmartPlug(deviceId3, true, 230f, 227f, 99f,
                DeviceStatus.MAINTENANCE, null, nowTime.minus(5, ChronoUnit.MINUTES));

        ConsumerRecord<String, SpecificRecord> record1 = new ConsumerRecord<>(TOPIC, 0, 0, doorSensor.getDeviceId(), doorSensor);
        ConsumerRecord<String, SpecificRecord> record2 = new ConsumerRecord<>(TOPIC, 0, 1, thermostat.getDeviceId(), thermostat);
        ConsumerRecord<String, SpecificRecord> record3 = new ConsumerRecord<>(TOPIC, 0, 2, smartPlug.getDeviceId(), smartPlug);

        Map<String, ConsumerRecord<String, SpecificRecord>> recordsById = new HashMap<>(3);
        recordsById.put(deviceId1, record1);
        recordsById.put(deviceId2, record2);
        recordsById.put(deviceId3, record3);

        Optional<OffsetAndMetadata> offsetToCommit = parallelDevicePatcher.patch(recordsById);

        assertTrue(offsetToCommit.isPresent());
        verify(deviceService).patchDoorSensorTelemetry(any());
        verify(deviceService).patchThermostatTelemetry(any());
        verify(deviceService).patchSmartPlugTelemetry(any());
        verify(kpiMetricLogger).incActiveThreadsInParallelPatcher(3);
        verify(kpiMetricLogger, times(2)).recordDeviceUpdatingTime(anyString(), anyLong());
        verify(kpiMetricLogger).incNonRetriableErrorsCount(NullPointerException.class.getSimpleName());
        verify(deadLetterProducer).send(doorSensor.getDeviceId(), doorSensor);
    }
}