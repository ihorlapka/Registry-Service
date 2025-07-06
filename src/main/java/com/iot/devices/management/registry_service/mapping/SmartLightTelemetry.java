package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value(staticConstructor = "of")
public class SmartLightTelemetry {
    UUID id;
    Boolean isOn;
    Integer brightness;
    String colour;
    String mode;
    Float powerConsumption;
    String status;
    String firmwareVersion;
    OffsetDateTime lastUpdated;
}
