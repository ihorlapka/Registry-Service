package com.iot.devices.management.registry_service.persistence.services;

import com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.DeviceNotFoundException;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.PatchDeviceRequest;
import com.iot.devices.management.registry_service.mapping.*;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.repos.DevicesRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus.ONLINE;
import static java.time.OffsetDateTime.now;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DevicesRepository devicesRepository;

    @Transactional
    public Device save(@Valid CreateDeviceRequest request, @Nullable User owner) {
        return devicesRepository.save(mapNewDevice(request, owner));
    }

    //dirty checking
    @Transactional
    public Device patch(@Valid PatchDeviceRequest request, User user) {
        final Optional<Device> device = devicesRepository.findById(request.id());
        if (device.isEmpty()) {
            throw new DeviceNotFoundException(request.id());
        }
        return patchDevice(request, device.get(), user);
    }

    @Transactional
    public int removeById(@NonNull UUID deviceId) {
        return devicesRepository.removeById(deviceId);
    }

    @Transactional
    public int patchDoorSensorTelemetry(DoorSensorTelemetry ds) {
        logDebug(ds);
        return devicesRepository.updateDoorSensorTelemetry(ds.getId(), ds.getStatus(), getLastActiveAt(ds.getStatus(), ds.getLastUpdated()),
                ds.getFirmwareVersion(), ds.getBatteryLevel(), ds.getLastUpdated(), ds.getDoorState(), ds.getTamperAlert(), ds.getLastOpened());
    }

    @Transactional
    public int patchEnergyMeterTelemetry(EnergyMeterTelemetry em) {
        logDebug(em);
        return devicesRepository.updateEnergyMeterTelemetry(em.getId(), em.getStatus(), em.getFirmwareVersion(), em.getLastUpdated(),
                em.getVoltage(), em.getCurrent(), em.getPower(), em.getEnergyConsumed());
    }

    @Transactional
    public int patchSmartLightTelemetry(SmartLightTelemetry sl) {
        logDebug(sl);
        return devicesRepository.updateSmartLightTelemetry(sl.getId(), sl.getStatus(), sl.getFirmwareVersion(), sl.getLastUpdated(),
                sl.getIsOn(), sl.getBrightness(), sl.getColour(), sl.getMode(), sl.getPowerConsumption());
    }

    @Transactional
    public int patchSmartPlugTelemetry(SmartPlugTelemetry sp) {
        logDebug(sp);
        return devicesRepository.updateSmartPlugTelemetry(sp.getId(), sp.getStatus(), sp.getFirmwareVersion(), sp.getLastUpdated(),
                sp.getIsOn(), sp.getVoltage(), sp.getCurrent(), sp.getPowerUsage());
    }

    @Transactional
    public int patchSoilMoistureSensorTelemetry(SoilMoistureSensorTelemetry sms) {
        logDebug(sms);
        return devicesRepository.updateSoilMoistureSensorTelemetry(sms.getId(), sms.getStatus(), sms.getFirmwareVersion(),
                sms.getLastUpdated(), sms.getMoisturePercentage(), sms.getSoilTemperature(), sms.getBatteryLevel());
    }

    @Transactional
    public int patchTemperatureSensorTelemetry(TemperatureSensorTelemetry ts) {
        logDebug(ts);
        return devicesRepository.updateTemperatureSensorTelemetry(ts.getId(), ts.getStatus(), getLastActiveAt(ts.getStatus(), ts.getLastUpdated()),
                ts.getFirmwareVersion(), ts.getLastUpdated(), ts.getTemperature(), ts.getHumidity(), ts.getPressure(), ts.getUnit());
    }

    @Transactional
    public int patchThermostatTelemetry(ThermostatTelemetry t) {
        logDebug(t);
        return devicesRepository.updateThermostatTelemetry(t.getId(), t.getStatus(), t.getFirmwareVersion(), t.getLastUpdated(),
                t.getCurrentTemperature(), t.getTargetTemperature(), t.getHumidity(), t.getMode());
    }

    public Optional<Device> findBySerialNumber(@NonNull @NotBlank(message = "serial number is required") String serialNumber) {
        return devicesRepository.findBySerialNumber(serialNumber);
    }

    public Optional<Device> findByDeviceId(@NonNull @NotBlank(message = "device id is required") UUID id) {
        return devicesRepository.findById(id);
    }

    private Device mapNewDevice(CreateDeviceRequest request, @Nullable User owner) {
        return new Device(null, request.name(), request.serialNumber(),
                request.deviceManufacturer(), request.model(), request.deviceType(),
                request.location(), request.latitude(), request.longitude(), owner,
                request.status(), request.lastActiveAt(), request.firmwareVersion(),
                now(), now(), null);
    }

    private Device patchDevice(PatchDeviceRequest request, Device device, @Nullable User user) {
        ofNullable(request.name()).ifPresent(device::setName);
        ofNullable(request.model()).ifPresent(device::setModel);
        ofNullable(request.deviceType()).ifPresent(device::setDeviceType);
        ofNullable(request.location()).ifPresent(device::setLocation);
        ofNullable(request.latitude()).ifPresent(device::setLatitude);
        ofNullable(request.longitude()).ifPresent(device::setLongitude);
        ofNullable(user).ifPresent(device::setOwner);
        ofNullable(request.status()).ifPresent(device::setStatus);
        ofNullable(request.lastActiveAt()).ifPresent(device::setLastActiveAt);
        ofNullable(request.firmwareVersion()).ifPresent(device::setFirmwareVersion);
        device.setUpdatedAt(now());
        return device;
    }

    private static OffsetDateTime getLastActiveAt(@Nullable String status, OffsetDateTime lastUpdated) {
        if (status == null) {
            return null;
        }
        return status.equals(ONLINE.name()) ? lastUpdated : null;
    }

    private void logDebug(Object o) {
        log.debug("Patching: {}", o);
    }
}
