package com.iot.devices.management.registry_service.persistence.services;

import com.google.common.collect.Sets;
import com.iot.devices.management.registry_service.controller.util.CreateAlertRuleRequest;
import com.iot.devices.management.registry_service.controller.util.PatchAlertRuleRequest;
import com.iot.devices.management.registry_service.kafka.AlertingRulesKafkaProducer;
import com.iot.devices.management.registry_service.persistence.model.*;
import com.iot.devices.management.registry_service.persistence.repos.AlertRulesRepository;
import com.iot.devices.management.registry_service.persistence.repos.DeviceAlertRuleRepository;
import com.iot.devices.management.registry_service.persistence.repos.DevicesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.DeviceNotFoundException;
import static java.util.stream.Collectors.toSet;
import static com.iot.devices.management.registry_service.controller.errors.AlertRulesException.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertRuleService {

    private final AlertRulesRepository alertRulesRepository;
    private final DevicesRepository devicesRepository;
    private final DeviceAlertRuleRepository deviceAlertRuleRepository;
    private final AlertingRulesKafkaProducer alertRulesProducer;


    @Transactional
    public AlertRule saveAndSendMessage(CreateAlertRuleRequest request, User user) {
        try {
            final List<Device> devices = loadDevices(request.deviceIds());
            final AlertRule alertRule = alertRulesRepository.save(mapNewAlertRule(request, user));
            final List<DeviceAlertRule> storedDevicesAlertRules = deviceAlertRuleRepository.saveAll(getDeviceAlertRules(devices, alertRule));
            if (!storedDevicesAlertRules.isEmpty() && storedDevicesAlertRules.size() == devices.size()) {
                alertRulesProducer.sendTransactionally(Map.of(alertRule, request.deviceIds()), emptySet());
            } else {
                throw new RuntimeException("Not all deviceAlertRules were persisted!");
            }
            log.info("Alert rule created, alertRuleId={} with deviceIds={}", alertRule.getRuleId(), request.deviceIds());
            return alertRule;
        } catch (Exception e) {
            log.warn("Unable to create AlertRule, request: {}", request, e);
            throw new UnableToCreateAlertRuleException(e.getMessage(), e);
        }
    }

    //dirty checking
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AlertRule patchAndSendMessage(PatchAlertRuleRequest request, @Nullable User user) {
        try {
            final Optional<AlertRule> alertRule = alertRulesRepository.findById(request.ruleId());
            if (alertRule.isEmpty()) {
                throw new AlertRuleNotFoundException(request.ruleId());
            }
            final StringBuilder sb = new StringBuilder();
            if (request.deviceIdsToAdd() != null && !request.deviceIdsToAdd().isEmpty()) {
                final List<Device> devicesToBeAdded = loadDevices(request.deviceIdsToAdd());
                final List<DeviceAlertRule> storedDevicesAlertRules = deviceAlertRuleRepository.saveAll(getDeviceAlertRules(devicesToBeAdded, alertRule.get()));
                if (storedDevicesAlertRules.isEmpty() || storedDevicesAlertRules.size() != request.deviceIdsToAdd().size()) {
                    throw new RuntimeException("Not all deviceAlertRules were saved");
                }
                sb.append(", added to devices").append(request.deviceIdsToAdd());
            }
            if (request.deviceIdsToRemove() != null && !request.deviceIdsToRemove().isEmpty()) {
                final int removed = deviceAlertRuleRepository.removeAllByIds(getDeviceAlertRuleKeys(request.deviceIdsToRemove(), alertRule.get()));
                if (removed == 0) {
                    log.warn("DeviceAlertRules were already removed for alertRuleId={}", request.ruleId());
                }
                if (removed != request.deviceIdsToRemove().size()) {
                    throw new RuntimeException("Not all devices alert rules were removed!");
                }
                sb.append(", removed from devices").append(request.deviceIdsToRemove());
            }
            final Set<DeviceAlertRule> deviceAlertRules = deviceAlertRuleRepository.findByAlertRule(alertRule.get());
            final AlertRule alertRulePatched = patchAlertRule(request, alertRule.get(), user);
            alertRulesProducer.sendTransactionally(Map.of(alertRulePatched, getDeviceIds(deviceAlertRules)), emptySet());
            log.info("AlertRule is updated{}", sb);
            return alertRulePatched;
        } catch (Exception e) {
            log.warn("Unable to update AlertRule, request: {}", request, e);
            throw e;
        }
    }

    @Transactional
    public void removeAndSendTombstone(UUID ruleId) {
        try {
            final Optional<AlertRule> alertRule = alertRulesRepository.findById(ruleId);
            if (alertRule.isPresent()) {
                final Set<DeviceAlertRule> deviceAlertRules = deviceAlertRuleRepository.findByAlertRule(alertRule.get());
                if (!deviceAlertRules.isEmpty()) {
                    final int removedDeviceAlertRules = deviceAlertRuleRepository.removeAllByIds(deviceAlertRules.stream()
                            .map(DeviceAlertRule::getId)
                            .toList());
                    if (removedDeviceAlertRules == 0) {
                        log.warn("DeviceAlertRules for alertRuleId={} has already been removed", ruleId);
                    } else {
                        log.info("{} deviceAlertRules were removed for alertRuleId={}", removedDeviceAlertRules, ruleId);
                    }
                }
                final int removedAlertRule = alertRulesRepository.removeById(ruleId);
                if (removedAlertRule == 0) {
                    log.warn("AlertRule with id={} has already been removed", ruleId);
                } else {
                    log.info("{} alertRule was removed, alertRuleId={}", removedAlertRule, ruleId);
                }
                alertRulesProducer.sendTransactionally(emptyMap(), Set.of(ruleId));
            } else {
                log.warn("No alert rule present for removing, alertRuleId={}", ruleId);
            }
        } catch (RuntimeException e) {
            log.warn("Unable to delete AlertRule, alertRuleId={}", ruleId, e);
            throw e;
        }
    }

    public AlertRule findAlertRuleById(UUID alertRuleId) {
        return alertRulesRepository.findById(alertRuleId)
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));
    }

    public List<AlertRule> findRulesByUsername(String username) {
        return alertRulesRepository.findAlertRulesByUsername(username);
    }

    private List<Device> loadDevices(Set<UUID> deviceIds) {
        final List<Device> devices = devicesRepository.findAllById(deviceIds);
        if (devices.isEmpty()) {
            throw new DeviceNotFoundException("No devices found for given Ids: " + deviceIds);
        }
        if (devices.size() != deviceIds.size()) {
            throw new DeviceNotFoundException("No devices present in db with Ids: [" +
                    Sets.difference(deviceIds, devices.stream().map(Device::getId).collect(toSet())) + "]");
        }
        return devices;
    }

    private List<DeviceAlertRule> getDeviceAlertRules(List<Device> devices, AlertRule alertRule) {
        final List<DeviceAlertRule> deviceAlertRules = new ArrayList<>(devices.size());
        for (Device device : devices) {
            deviceAlertRules.add(new DeviceAlertRule(new DeviceAlertRuleKey(device.getId(), alertRule.getRuleId()), device, alertRule));
        }
        return deviceAlertRules;
    }

    private List<DeviceAlertRuleKey> getDeviceAlertRuleKeys(Set<UUID> deviceIds, AlertRule alertRule) {
        return deviceIds.stream()
                .map(deviceId -> new DeviceAlertRuleKey(deviceId, alertRule.getRuleId()))
                .toList();
    }

    private Set<UUID> getDeviceIds(Set<DeviceAlertRule> deviceAlertRules) {
        return deviceAlertRules.stream()
                .map(DeviceAlertRule::getDevice)
                .map(Device::getId)
                .collect(toSet());
    }

    private AlertRule mapNewAlertRule(CreateAlertRuleRequest request, @Nullable User user) {
        return new AlertRule(null, request.metricType(), request.thresholdType(),
                request.thresholdValue(), request.severity(), request.isEnabled(), getUsername(user));
    }

    private AlertRule patchAlertRule(PatchAlertRuleRequest request, AlertRule alertRule, @Nullable User user) {
        ofNullable(request.metricType()).ifPresent(alertRule::setMetricType);
        ofNullable(request.thresholdType()).ifPresent(alertRule::setThresholdType);
        ofNullable(request.thresholdValue()).ifPresent(alertRule::setThresholdValue);
        ofNullable(request.severity()).ifPresent(alertRule::setSeverity);
        ofNullable(request.isEnabled()).ifPresent(alertRule::setEnabled);
        ofNullable(getUsername(user)).ifPresent(alertRule::setUsername);
        return alertRule;
    }

    private String getUsername(User user) {
        return ofNullable(user).map(User::getUsername).orElse(null);
    }
}
