package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value(staticConstructor = "of")
public class SmartPlugTelemetry {
    UUID id;
    Boolean isOn;
    Float voltage;
    Float current;
    Float powerUsage;
    String status;
    String firmwareVersion;
    OffsetDateTime lastUpdated;
}
