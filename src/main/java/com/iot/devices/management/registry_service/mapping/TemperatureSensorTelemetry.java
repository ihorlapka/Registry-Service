package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;

@Value(staticConstructor = "of")
public class TemperatureSensorTelemetry {
    String id;
    Float temperature;
    Float humidity;
    Float pressure;
    String unit;
    String status;
    String firmwareVersion;
    OffsetDateTime lastUpdated;
}
