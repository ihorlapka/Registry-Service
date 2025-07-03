package com.iot.devices.management.registry_service.mapping;

import lombok.Value;

import java.time.OffsetDateTime;

@Value(staticConstructor = "of")
public class DoorSensorTelemetry {
    String id;
    String doorState;
    Integer batteryLevel;
    Boolean tamperAlert;
    String status;
    String firmwareVersion;
    OffsetDateTime lastOpened;
    OffsetDateTime lastUpdated;
}
