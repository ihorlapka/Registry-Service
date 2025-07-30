package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.*;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.*;
import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapSmartPlug;
import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapSoilMoisture;
import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapTemperatureSensor;
import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapThermostat;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetriablePersister {

    private final DeviceService deviceService;

    @Retry(name = "patchDeviceRetry", fallbackMethod = "updateFallback")
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void persistWithRetries(ConsumerRecord<String, SpecificRecord> record) {
        final int updated = patchTelemetry(record.value());
        switch (updated) {
            case 0 -> log.warn("No device was updated by id={}, offset={}", record.value(), record.offset());
            case 1 -> log.debug("Device with id={} is updated, offset={}", record.value(), record.offset());
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
