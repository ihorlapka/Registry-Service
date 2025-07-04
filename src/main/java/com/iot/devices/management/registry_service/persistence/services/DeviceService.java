package com.iot.devices.management.registry_service.persistence.services;

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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus.ONLINE;
import static java.time.OffsetDateTime.now;
import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DevicesRepository devicesRepository;

    public Optional<Device> findBySerialNumber(@NonNull @NotBlank(message = "serial number is required") String serialNumber) {
        return devicesRepository.findBySerialNumber(serialNumber);
    }

    public Device save(CreateDeviceRequest request, User owner) {
        final Device newDevice = mapNewDevice(request, owner);
        return devicesRepository.save(newDevice);
    }

    public Optional<Device> findByDeviceId(@NonNull @NotBlank(message = "device id is required") UUID id) {
        return devicesRepository.findById(id);
    }

    public int removeById(@NonNull UUID deviceId) {
        return devicesRepository.removeById(deviceId);
    }

    private Device mapNewDevice(CreateDeviceRequest request, User owner) {
        return new Device(null, request.name(), request.serialNumber(),
                request.deviceManufacturer(), request.model(), request.deviceType(),
                request.location(), request.latitude(), request.longitude(), owner,
                request.status(), request.lastActiveAt(), request.firmwareVersion(),
                now(), now(), null);
    }

    public Device patch(@Valid PatchDeviceRequest request, @NonNull Device device, @Nullable User user) {
        Device patched =  patchDevice(request, device, user);
        return devicesRepository.save(patched);
    }

    private Device patchDevice(PatchDeviceRequest request, Device device, User user) {
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

    private static OffsetDateTime getLastActiveAt(String status, OffsetDateTime lastUpdated) {
        return status.equals(ONLINE.name()) ? lastUpdated : null;
    }

    public int patchDoorSensorTelemetry(DoorSensorTelemetry ds) {
        return devicesRepository.updateDoorSensorTelemetry(ds.getId(), ds.getStatus(), getLastActiveAt(ds.getStatus(), ds.getLastUpdated()),
                ds.getFirmwareVersion(), ds.getBatteryLevel(), ds.getLastUpdated(), ds.getDoorState(), ds.getTamperAlert(), ds.getLastOpened());
    }

    public int patchEnergyMeterTelemetry(EnergyMeterTelemetry em) {
        return 0;
    }

    public int patchSmartLightTelemetry(SmartLightTelemetry sl) {
        return 0;
    }

    public int patchSmartPlugTelemetry(SmartPlugTelemetry sp) {
        return 0;
    }

    public int patchSoilMoistureSensorTelemetry(SoilMoistureSensorTelemetry sms) {
        return 0;
    }

    public int patchTemperatureSensorTelemetry(TemperatureSensorTelemetry ts) {
        return 0;
    }

    public int patchThermostatTelemetry(ThermostatTelemetry t) {
        return 0;
    }
}
