package com.iot.devices.management.registry_service.mapping;

import com.iot.devices.*;
import lombok.experimental.UtilityClass;

import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.util.Optional.ofNullable;

@UtilityClass
public class DeviceParametersMapper {

    public static DoorSensorTelemetry mapDoorSensor(DoorSensor doorSensor) {
        return DoorSensorTelemetry.of(
                UUID.fromString(doorSensor.getDeviceId()),
                ofNullable(doorSensor.getDoorState()).map(Enum::name).orElse(null),
                doorSensor.getBatteryLevel(),
                doorSensor.getTamperAlert(),
                ofNullable(doorSensor.getStatus()).map(Enum::name).orElse(null),
                doorSensor.getFirmwareVersion(),
                ofNullable(doorSensor.getLastOpened()).map(instant -> instant.atOffset(UTC)).orElse(null),
                doorSensor.getLastUpdated().atOffset(UTC)
        );
    }

    public static EnergyMeterTelemetry mapEnergyMeter(EnergyMeter energyMeter) {
        return EnergyMeterTelemetry.of(
                UUID.fromString(energyMeter.getDeviceId()),
                energyMeter.getVoltage(),
                energyMeter.getCurrent(),
                energyMeter.getPower(),
                energyMeter.getEnergyConsumed(),
                ofNullable(energyMeter.getStatus()).map(Enum::name).orElse(null),
                energyMeter.getFirmwareVersion(),
                energyMeter.getLastUpdated().atOffset(UTC)
        );
    }

    public static SmartLightTelemetry mapSmartLight(SmartLight smartLight) {
        return SmartLightTelemetry.of(
                UUID.fromString(smartLight.getDeviceId()),
                smartLight.getIsOn(),
                smartLight.getBrightness(),
                smartLight.getColor(),
                ofNullable(smartLight.getMode()).map(Enum::name).orElse(null),
                smartLight.getPowerConsumption(),
                ofNullable(smartLight.getStatus()).map(Enum::name).orElse(null),
                smartLight.getFirmwareVersion(),
                smartLight.getLastUpdated().atOffset(UTC)
        );
    }

    public static SmartPlugTelemetry mapSmartPlug(SmartPlug smartPlug) {
        return SmartPlugTelemetry.of(
                UUID.fromString(smartPlug.getDeviceId()),
                smartPlug.getIsOn(),
                smartPlug.getVoltage(),
                smartPlug.getCurrent(),
                smartPlug.getPowerUsage(),
                ofNullable(smartPlug.getStatus()).map(Enum::name).orElse(null),
                smartPlug.getFirmwareVersion(),
                smartPlug.getLastUpdated().atOffset(UTC)
        );
    }

    public static SoilMoistureSensorTelemetry mapSoilMoisture(SoilMoistureSensor soilMoistureSensor) {
        return SoilMoistureSensorTelemetry.of(
                UUID.fromString(soilMoistureSensor.getDeviceId()),
                soilMoistureSensor.getMoisturePercentage(),
                soilMoistureSensor.getSoilTemperature(),
                soilMoistureSensor.getBatteryLevel(),
                ofNullable(soilMoistureSensor.getStatus()).map(Enum::name).orElse(null),
                soilMoistureSensor.getFirmwareVersion(),
                soilMoistureSensor.getLastUpdated().atOffset(UTC)
        );
    }

    public static TemperatureSensorTelemetry mapTemperatureSensor(TemperatureSensor temperatureSensor) {
        return TemperatureSensorTelemetry.of(
                UUID.fromString(temperatureSensor.getDeviceId()),
                temperatureSensor.getTemperature(),
                temperatureSensor.getHumidity(),
                temperatureSensor.getPressure(),
                ofNullable(temperatureSensor.getUnit()).map(Enum::name).orElse(null),
                ofNullable(temperatureSensor.getStatus()).map(Enum::name).orElse(null),
                temperatureSensor.getFirmwareVersion(),
                temperatureSensor.getLastUpdated().atOffset(UTC)
        );
    }

    public static ThermostatTelemetry mapThermostat(Thermostat thermostat) {
        return ThermostatTelemetry.of(
                UUID.fromString(thermostat.getDeviceId()),
                thermostat.getCurrentTemperature(),
                thermostat.getTargetTemperature(),
                thermostat.getHumidity(),
                ofNullable(thermostat.getMode()).map(Enum::name).orElse(null),
                ofNullable(thermostat.getStatus()).map(Enum::name).orElse(null),
                thermostat.getFirmwareVersion(),
                thermostat.getLastUpdated().atOffset(UTC)
        );
    }
}
