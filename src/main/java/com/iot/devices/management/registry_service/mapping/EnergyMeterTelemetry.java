package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;

@Value(staticConstructor = "of")
public class EnergyMeterTelemetry {
    String id;
    Float voltage;
    Float current;
    Float power;
    Float energyConsumed;
    String status;
    String firmwareVersion;
    OffsetDateTime lastUpdated;
}
