package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value(staticConstructor = "of")
public class DoorSensorTelemetry {
    UUID id;
    String doorState;
    Integer batteryLevel;
    Boolean tamperAlert;
    String status;
    String firmwareVersion;
    OffsetDateTime lastOpened;
    OffsetDateTime lastUpdated;
}
