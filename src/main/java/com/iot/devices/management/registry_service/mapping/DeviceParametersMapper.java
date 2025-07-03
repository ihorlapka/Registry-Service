package com.iot.devices.management.registry_service.mapping;

import com.iot.devices.*;
import lombok.experimental.UtilityClass;

import static java.time.ZoneOffset.UTC;

@UtilityClass
public class DeviceParametersMapper {

    public static DoorSensorTelemetry mapDoorSensor(DoorSensor doorSensor) {
        return DoorSensorTelemetry.of(
                doorSensor.getDeviceId(),
                doorSensor.getDoorState().name(),
                doorSensor.getBatteryLevel(),
                doorSensor.getTamperAlert(),
                doorSensor.getStatus().name(),
                doorSensor.getFirmwareVersion(),
                doorSensor.getLastOpened().atOffset(UTC),
                doorSensor.getLastUpdated().atOffset(UTC)
        );
    }
}
