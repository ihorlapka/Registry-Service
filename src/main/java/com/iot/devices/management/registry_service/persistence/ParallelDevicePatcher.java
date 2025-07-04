package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.*;
import com.iot.devices.management.registry_service.kafka.DeadLetterProducer;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import io.github.resilience4j.retry.annotation.Retry;
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

import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.*;

@Slf4j
@Component
public class ParallelDevicePatcher {

    private static final String PROPERTIES_PREFIX = "parallel-persister";

    private final ExecutorService executorService;
    private final DeviceService deviceService;
    private final DeadLetterProducer deadLetterProducer;

    public ParallelDevicePatcher(@Value("${" + PROPERTIES_PREFIX + ".threads-amount}") int threadsAmount,
                                 DeviceService deviceService, DeadLetterProducer deadLetterProducer) {
        this.executorService = Executors.newFixedThreadPool(threadsAmount);
        this.deviceService = deviceService;
        this.deadLetterProducer = deadLetterProducer;
    }


    public Map<TopicPartition, OffsetAndMetadata> patch(Map<String, ConsumerRecord<String, SpecificRecord>> recordById) {
        final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new ConcurrentHashMap<>();
        final List<CompletableFuture<Void>> futures = new ArrayList<>(recordById.size());
        for (Map.Entry<String, ConsumerRecord<String, SpecificRecord>> entry : recordById.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                final ConsumerRecord<String, SpecificRecord> record = entry.getValue();
                try {
                    persistWithRetries(record);
                    offsetsToCommit.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
                } catch (Exception e) {
                    if (!isRetriableException(e)) {
                        log.error("Failed to update device with id={} after retries, sending message to dead-letter-topic, offset={} will be committed",
                                record.value(), record.offset(), e);
                        deadLetterProducer.send(record.value());
                        offsetsToCommit.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
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

    @Retry(name = "patchDeviceRetry", fallbackMethod = "updateFallback")
    private void persistWithRetries(ConsumerRecord<String, SpecificRecord> record) {
        final int updated = patchTelemetry(record.value());
        switch (updated) {
            case 0 -> log.warn("No device was updated by id={}, offset={}", record.value(), record.offset());
            case 1 -> log.debug("Device with id={} is updated", record.value());
            default -> log.warn("More than one device were updated by id={}, offset={}", record.value(), record.offset());
        }
    }

    private int patchTelemetry(SpecificRecord record) {
        return switch (record) {
            case DoorSensor ds -> deviceService.patchDoorSensorTelemetry(mapDoorSensor(ds));
            case EnergyMeter em -> deviceService.patchEnergyMeterTelemetry(mapEnergyMeter(em));
            case SmartLight sl -> deviceService.patchSmartLightTelemetry(mapSmartLight(sl));
            case SmartPlug sp -> deviceService.patchSmartPlugTelemetry(mapSmartPlug(sp));
            case SoilMoistureSensor sms -> deviceService.patchSoilMoistureSensorTelemetry(mapSoilMoisture(sms));
            case TemperatureSensor ts -> deviceService.patchTemperatureSensorTelemetry(mapTemperatureSensor(ts));
            case Thermostat t -> deviceService.patchThermostatTelemetry(mapThermostat(t));
            default -> throw new IllegalArgumentException("Unknown device type detected");
        };
    }

    public void updateFallback(ConsumerRecord<String, SpecificRecord> record, Throwable t) {
        log.error("Retry failed for: {}", record, t);
        throw new RuntimeException("Update failed after retries!");
    }
}
