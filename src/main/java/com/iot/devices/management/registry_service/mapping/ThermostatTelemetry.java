package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value(staticConstructor = "of")
public class ThermostatTelemetry {
    UUID id;
    Float currentTemperature;
    Float targetTemperature;
    Float humidity;
    String mode;
    String status;
    String firmwareVersion;
    OffsetDateTime lastUpdated;
}
