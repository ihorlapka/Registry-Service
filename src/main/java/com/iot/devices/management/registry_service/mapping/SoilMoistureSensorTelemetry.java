package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;

@Value(staticConstructor = "of")
public class SoilMoistureSensorTelemetry {
    String id;
    Float moisturePercentage;
    Float soilTemperature;
    Integer batteryLevel;
    String status;
    String firmwareVersion;
    OffsetDateTime lastUpdated;
}
