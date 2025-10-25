package com.iot.devices.management.registry_service.persistence.services;

import com.google.common.collect.Sets;
import com.iot.devices.management.registry_service.alerts.StandardAlertRulesProvider;
import com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.DeviceNotFoundException;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.PatchDeviceRequest;
import com.iot.devices.management.registry_service.kafka.AlertingRulesKafkaProducer;
import com.iot.devices.management.registry_service.mapping.*;
import com.iot.devices.management.registry_service.persistence.model.*;
import com.iot.devices.management.registry_service.persistence.repos.AlertRulesRepository;
import com.iot.devices.management.registry_service.persistence.repos.DeviceAlertRuleRepository;
import com.iot.devices.management.registry_service.persistence.repos.DevicesRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.*;

import static com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus.ONLINE;
import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.UnableToCreateDeviceException;
import static com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DevicesRepository devicesRepository;
    private final AlertRulesRepository alertRulesRepository;
    private final DeviceAlertRuleRepository deviceAlertRuleRepository;
    private final StandardAlertRulesProvider alertRulesProvider;
    private final AlertingRulesKafkaProducer alertingRulesKafkaProducer;


    @Transactional
    public Device saveAndSendMessage(@Valid CreateDeviceRequest request, @Nullable User owner) {
        try {
            final Device savedDevice = devicesRepository.save(mapNewDevice(request, owner));
            final StringBuilder sb = new StringBuilder("Device {" + savedDevice + "} is created");
            List<AlertRule> alertRules;
            if (!CollectionUtils.isEmpty(request.alertRuleIds())) {
                alertRules = alertRulesRepository.findAllById(request.alertRuleIds().stream().map(UUID::fromString).toList());
            } else {
                final Set<AlertRule> standardRules = alertRulesProvider.getAlertRules(request.deviceType(), owner);
                if (!standardRules.isEmpty()) {
                    alertRules = alertRulesRepository.saveAll(standardRules);
                } else {
                    alertRules = emptyList();
                }
            }
            if (!alertRules.isEmpty()) {
                final Set<DeviceAlertRule> deviceAlertRules = alertRules.stream()
                        .map(alertRule -> new DeviceAlertRule(DeviceAlertRuleKey.of(savedDevice.getId(), alertRule.getRuleId()), savedDevice, alertRule))
                        .collect(toSet());
                final List<DeviceAlertRule> savedDeviceAlertRules = deviceAlertRuleRepository.saveAll(deviceAlertRules);
                if (!savedDeviceAlertRules.isEmpty() && savedDeviceAlertRules.size() == alertRules.size()) {
                    alertingRulesKafkaProducer.sendTransactionallyCreate(alertRules, List.of(savedDevice.getId().toString()));
                    sb.append(" with alertRules {").append(alertRules).append("}");
                } else {
                    throw new RuntimeException("Unexpected exception during persistence of device alert rules");
                }
            }
            log.info(sb.toString());
            return savedDevice;
        } catch (Exception e) {
            log.warn("Unable to create Device, transaction will be rolled back if it was committed", e);
            throw new UnableToCreateDeviceException(e.getMessage(), e);
        }
    }

    //dirty checking
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Device patch(@Valid PatchDeviceRequest request, User user) {
        try {
            final Optional<Device> device = devicesRepository.findById(request.id());
            if (device.isEmpty()) {
                throw new DeviceNotFoundException(request.id());
            }
            final StringBuilder sb = new StringBuilder();
            if (!CollectionUtils.isEmpty(request.alertRulesToAdd()) || CollectionUtils.isEmpty(request.alertRulesToRemove())) {
                final Set<UUID> alertRulesToBeChangedIds = Sets.union(ofNullable(request.alertRulesToAdd()).orElse(emptySet()),
                        ofNullable(request.alertRulesToRemove()).orElse(emptySet()));
                final List<AlertRule> alertRulesToBeChanged = alertRulesRepository.findAllById(alertRulesToBeChangedIds);

                if (request.alertRulesToAdd() != null && !request.alertRulesToAdd().isEmpty()) {
                    final List<AlertRule> alertRulesToBeAdded = alertRulesToBeChanged.stream()
                            .filter(alertRule -> request.alertRulesToAdd().contains(alertRule.getRuleId()))
                            .toList();
                    final List<DeviceAlertRule> storedDevicesAlertRules = deviceAlertRuleRepository.saveAll(getDeviceAlertRules(device.get(), alertRulesToBeAdded));
                    if (storedDevicesAlertRules.isEmpty() || storedDevicesAlertRules.size() != request.alertRulesToAdd().size()) {
                        throw new RuntimeException("Not all devices alert rules were persisted!");
                    }
                    sb.append(", added to alertRules {").append(request.alertRulesToAdd()).append("}");
                }
                if (request.alertRulesToRemove() != null && !request.alertRulesToRemove().isEmpty()) {
                    final List<DeviceAlertRuleKey> keysToRemove = request.alertRulesToRemove().stream()
                            .map(alertRuleId -> DeviceAlertRuleKey.of(request.id(), alertRuleId))
                            .toList();
                    final int removed = deviceAlertRuleRepository.removeAllByIds(keysToRemove);
                    if (removed == 0) {
                        log.warn("Device alert rules were already removed for deviceId={}", request.id());
                    } else if (removed != request.alertRulesToRemove().size()) {
                        throw new RuntimeException("Not all devices alert rules were removed!");
                    }
                    sb.append(", removed from alertRules {").append(request.alertRulesToRemove()).append("}");
                }
                final Set<DeviceAlertRule> deviceAlertRules = deviceAlertRuleRepository.findAllByAlertRuleIn(alertRulesToBeChanged);
                final Map<AlertRule, List<UUID>> deviceIdsByAlertRule = deviceAlertRules.stream()
                        .collect(groupingBy(DeviceAlertRule::getAlertRule, mapping(dar -> dar.getId().getDeviceId(), toList())));
                alertingRulesKafkaProducer.sendTransactionallyPatch(deviceIdsByAlertRule);
            }
            final Device patchedDevice = patchDevice(request, device.get(), user);
            log.info("Device is updated {}{}", patchedDevice, sb.toString());
            return patchedDevice;
        } catch (Exception e) {
            log.warn("Unable to patch Device, transaction will be rolled back if it was committed", e);
            throw new UnableToPatchDeviceException(e.getMessage(), e);
        }
    }

    @Transactional
    public int removeById(@NonNull UUID deviceId) {
        try {
            final int removedDevice = devicesRepository.removeById(deviceId);
            if (removedDevice == 0) {
                log.warn("Device with id={} has already been removed", deviceId);
            } else {
                log.info("{} device was removed, deviceId={}", removedDevice, deviceId);
            }
            final Set<DeviceAlertRule> deviceAlertRules = deviceAlertRuleRepository.findAllByDeviceId(deviceId);
            final Set<AlertRule> alertRulesToRemove = deviceAlertRules.stream()
                    .collect(groupingBy(DeviceAlertRule::getAlertRule, mapping(dar -> dar.getId().getDeviceId(), toList())))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue().contains(deviceId) && entry.getValue().size() == 1)
                    .map(Map.Entry::getKey)
                    .collect(toSet());

            if (!alertRulesToRemove.isEmpty()) {
                final int removedAlertRules = alertRulesRepository.removeAllByAlertRuleIn(alertRulesToRemove);
                if (removedAlertRules == 0) {
                    log.warn("Alert rules with ids={} has already been removed", alertRulesToRemove);
                } else {
                    log.info("{} alert rules were removed for deviceId={}, alertRuleIds={}", removedAlertRules, deviceId, alertRulesToRemove);
                }
            }
            final List<DeviceAlertRuleKey> keysToRemove = deviceAlertRules.stream()
                    .map(DeviceAlertRule::getId)
                    .toList();
            if (!keysToRemove.isEmpty()) {
                final int removedDeviceAlertRules = deviceAlertRuleRepository.removeAllByIds(keysToRemove);
                if (removedDeviceAlertRules == 0) {
                    log.warn("Device alert rules for device with id={} has already been removed", deviceId);
                } else {
                    log.info("{} device alert rules were removed for deviceId={}", removedDeviceAlertRules, deviceId);
                }
            }
            return removedDevice;
        } catch (Exception e) {
            log.warn("Unable to remove Device, transaction will be rolled back if it was committed", e);
            throw new UnableToRemoveDeviceException(e.getMessage(), e);
        }
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

    private List<DeviceAlertRule> getDeviceAlertRules(Device device, List<AlertRule> alertRules) {
        final List<DeviceAlertRule> deviceAlertRules = new ArrayList<>(alertRules.size());
        for (AlertRule alertRule : alertRules) {
            deviceAlertRules.add(new DeviceAlertRule(DeviceAlertRuleKey.of(device.getId(), alertRule.getRuleId()), device, alertRule));
        }
        return deviceAlertRules;
    }
}
