package com.iot.devices.management.registry_service.persistence.retry;

import com.iot.devices.*;
import com.iot.devices.management.registry_service.metrics.KpiMetricLogger;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;

import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.*;
import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapSmartPlug;
import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapSoilMoisture;
import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapTemperatureSensor;
import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapThermostat;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetriablePatcher {

    private final DeviceService deviceService;
    private final RetryProperties retryProperties;
    private final KpiMetricLogger kpiMetricLogger;

    public void patchWithRetries(ConsumerRecord<String, SpecificRecord> record) throws Exception {
        int currentTry = 0;
        Exception lastException = null;
        while (currentTry < retryProperties.getMaxAttempts()) {
            try {
                if (currentTry > 0) {
                    sleep(retryProperties.getWaitDuration());
                }
                persist(record, currentTry + 1);
                return;
            } catch (TransientDataAccessException | SQLTransientException | SQLRecoverableException e) {
                log.warn("Failed to persist record on try {}/{}. Waiting {} ms before next retry...",
                        currentTry + 1, retryProperties.getMaxAttempts(), retryProperties.getWaitDuration());
                kpiMetricLogger.incRetriesCount();
                lastException = e;
                currentTry++;
            }
        }
        if (lastException != null) {
            log.error("All {} attempts to persist record failed.", retryProperties.getMaxAttempts(), lastException);
            throw lastException;
        }
    }

    private void persist(ConsumerRecord<String, SpecificRecord> record, int currentTry)
            throws TransientDataAccessException, SQLTransientException, SQLRecoverableException {
        final long startTimeMs = currentTimeMillis();
        final int updated = patchTelemetry(record.value());
        final String deviceType = record.value().getSchema().getName();
        switch (updated) {
            case 0 -> {
                kpiMetricLogger.incNotUpdatedDevices(deviceType);
                log.warn("No device was updated {}, offset={}, tryNum={}", record.value(), record.offset(), currentTry);
            }
            case 1 -> {
                kpiMetricLogger.recordDeviceUpdatingTime(deviceType, currentTimeMillis() - startTimeMs);
                log.info("Successfully updated {} offset={}, tryNum={}", record.value(), record.offset(), currentTry);
            }
            default -> {
                kpiMetricLogger.incSeveralUpdatedDevices(deviceType);
                log.warn("More than one device were updated by {}, offset={}, tryNum={}", record.value(), record.offset(), currentTry);
            }
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
}
