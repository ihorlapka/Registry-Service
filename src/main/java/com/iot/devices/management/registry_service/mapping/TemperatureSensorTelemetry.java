package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value(staticConstructor = "of")
public class TemperatureSensorTelemetry {
    UUID id;
    Float temperature;
    Float humidity;
    Float pressure;
    String unit;
    String status;
    String firmwareVersion;
    OffsetDateTime lastUpdated;
}
