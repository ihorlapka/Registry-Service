package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value(staticConstructor = "of")
public class SoilMoistureSensorTelemetry {
    UUID id;
    Float moisturePercentage;
    Float soilTemperature;
    Integer batteryLevel;
    String status;
    String firmwareVersion;
    OffsetDateTime lastUpdated;
}
