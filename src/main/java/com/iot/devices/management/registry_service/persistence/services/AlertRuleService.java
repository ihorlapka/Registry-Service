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
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
            final List<DeviceAlertRule> deviceAlertRules = getDeviceAlertRules(devices, alertRule);
            final List<DeviceAlertRule> storedDevicesAlertRules = deviceAlertRuleRepository.saveAll(deviceAlertRules);
            if (!storedDevicesAlertRules.isEmpty() && storedDevicesAlertRules.size() == deviceAlertRules.size()) {
                alertRulesProducer.sendOne(alertRule.getRuleId().toString(), buildAlertRuleMessage(alertRule, request.deviceIds()));
            } else {
                throw new RuntimeException("Not all devices alert rules were persisted!");
            }
            return alertRule;
        } catch (Exception e) {
            log.warn("Unable to create Alert Rule, transaction will be rolled back if it was committed", e);
            throw new UnableToCreateAlertRuleException(e.getMessage(), e);
        }
    }

    //dirty checking
    @Transactional
    public AlertRule patchAndSendMessage(PatchAlertRuleRequest request, @Nullable User user) {
        try {
            final Optional<AlertRule> alertRule = alertRulesRepository.findById(request.ruleId());
            if (alertRule.isEmpty()) {
                throw new AlertRuleNotFoundException(request.ruleId());
            }
            if (request.deviceIdsToAdd() != null && !request.deviceIdsToAdd().isEmpty()) {
                final List<Device> devicesToBeAdded = loadDevices(request.deviceIdsToAdd());
                final List<DeviceAlertRule> deviceAlertRules = getDeviceAlertRules(devicesToBeAdded, alertRule.get());
                final List<DeviceAlertRule> storedDevicesAlertRules = deviceAlertRuleRepository.saveAll(deviceAlertRules);
                if (storedDevicesAlertRules.isEmpty() || storedDevicesAlertRules.size() != request.deviceIdsToAdd().size()) {
                    throw new RuntimeException("Not all devices alert rules were persisted!");
                }
            }
            if (request.deviceIdsToRemove() != null && !request.deviceIdsToRemove().isEmpty()) {
                final List<DeviceAlertRuleKey> keysToRemove = request.deviceIdsToRemove().stream()
                        .map(deviceId -> DeviceAlertRuleKey.of(deviceId, alertRule.get().getRuleId()))
                        .toList();
                final int removed = deviceAlertRuleRepository.removeAllByIds(keysToRemove);
                if (removed == 0) {
                    log.warn("Device alert rules were already removed for alertRuleId={}", request.ruleId());
                }
                if (removed != request.deviceIdsToRemove().size()) {
                    throw new RuntimeException("Not all devices alert rules were removed!");
                }
            }
            final Set<DeviceAlertRule> deviceAlertRules = deviceAlertRuleRepository.findByAlertRule(alertRule.get());
            final Set<UUID> deviceIds = deviceAlertRules.stream()
                    .map(DeviceAlertRule::getDevice)
                    .map(Device::getId)
                    .collect(toSet());
            final AlertRule alertRulePatched = patchAlertRule(request, alertRule.get(), user);
            alertRulesProducer.sendOne(alertRule.get().getRuleId().toString(), buildAlertRuleMessage(alertRulePatched, deviceIds));
            return alertRulePatched;
        } catch (RuntimeException e) {
            log.warn("Unable to patch or send Alert Rule, transaction will be rolled back if it was committed");
            throw e;
        }
    }

    @Transactional
    public void removeAndSendTombstone(UUID ruleId) {
        final Optional<AlertRule> alertRule = alertRulesRepository.findById(ruleId);
        if (alertRule.isPresent()) {
            try {
                alertRulesRepository.deleteById(ruleId);
                //TODO: remove device alert rules!
                alertRulesProducer.sendOne(alertRule.get().getRuleId().toString(), null);
            } catch (RuntimeException e) {
                log.warn("Unable to delete or send Alert Rule, transaction will be rolled back if it was committed");
                throw e;
            }
        } else {
            log.warn("No alert rule present for removing, ruleId={}", ruleId);
        }
    }

    public List<AlertRule> findRulesByUsername(String username) {
        return alertRulesRepository.findAlertRulesByUsername(username);
    }

    private List<Device> loadDevices(Set<UUID> deviceIds) {
        final List<Device> devices = devicesRepository.findAllById(deviceIds);
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("No devices found for given Ids: [" + deviceIds + "]");
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
            deviceAlertRules.add(new DeviceAlertRule(DeviceAlertRuleKey.of(device.getId(), alertRule.getRuleId()), device, alertRule));
        }
        return deviceAlertRules;
    }

    private com.iot.alerts.AlertRule buildAlertRuleMessage(AlertRule alertRule, Set<UUID> deviceUuids) {
        return com.iot.alerts.AlertRule.newBuilder()
                .setRuleId(alertRule.getRuleId().toString())
                .setDeviceIds(deviceUuids.stream().map(UUID::toString).toList())
                .setMetricName(com.iot.alerts.MetricType.valueOf(alertRule.getMetricType().name()))
                .setThresholdType(com.iot.alerts.ThresholdType.valueOf(alertRule.getThresholdType().name()))
                .setThresholdValue(alertRule.getThresholdValue())
                .setSeverity(com.iot.alerts.SeverityLevel.valueOf(alertRule.getSeverity().name()))
                .setIsEnabled(alertRule.isEnabled())
                .build();
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
