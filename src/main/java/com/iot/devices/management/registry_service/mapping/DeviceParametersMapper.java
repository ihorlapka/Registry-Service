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

    public static EnergyMeterTelemetry mapEnergyMeter(EnergyMeter energyMeter) {
        return EnergyMeterTelemetry.of(
                energyMeter.getDeviceId(),
                energyMeter.getVoltage(),
                energyMeter.getCurrent(),
                energyMeter.getPower(),
                energyMeter.getEnergyConsumed(),
                energyMeter.getStatus().name(),
                energyMeter.getFirmwareVersion(),
                energyMeter.getLastUpdated().atOffset(UTC)
        );
    }

    public static SmartLightTelemetry mapSmartLight(SmartLight smartLight) {
        return SmartLightTelemetry.of(
                smartLight.getDeviceId(),
                smartLight.getIsOn(),
                smartLight.getBrightness(),
                smartLight.getColor(),
                smartLight.getMode().name(),
                smartLight.getPowerConsumption(),
                smartLight.getStatus().name(),
                smartLight.getFirmwareVersion(),
                smartLight.getLastUpdated().atOffset(UTC)
        );
    }

    public static SmartPlugTelemetry mapSmartPlug(SmartPlug smartPlug) {
        return SmartPlugTelemetry.of(
                smartPlug.getDeviceId(),
                smartPlug.getIsOn(),
                smartPlug.getVoltage(),
                smartPlug.getCurrent(),
                smartPlug.getPowerUsage(),
                smartPlug.getStatus().name(),
                smartPlug.getFirmwareVersion(),
                smartPlug.getLastUpdated().atOffset(UTC)
        );
    }

    public static SoilMoistureSensorTelemetry mapSoilMoisture(SoilMoistureSensor soilMoistureSensor) {
        return SoilMoistureSensorTelemetry.of(
                soilMoistureSensor.getDeviceId(),
                soilMoistureSensor.getMoisturePercentage(),
                soilMoistureSensor.getSoilTemperature(),
                soilMoistureSensor.getBatteryLevel(),
                soilMoistureSensor.getStatus().name(),
                soilMoistureSensor.getFirmwareVersion(),
                soilMoistureSensor.getLastUpdated().atOffset(UTC)
        );
    }

    public static TemperatureSensorTelemetry mapTemperatureSensor(TemperatureSensor temperatureSensor) {
        return TemperatureSensorTelemetry.of(
                temperatureSensor.getDeviceId(),
                temperatureSensor.getTemperature(),
                temperatureSensor.getHumidity(),
                temperatureSensor.getPressure(),
                temperatureSensor.getUnit().name(),
                temperatureSensor.getStatus().name(),
                temperatureSensor.getFirmwareVersion(),
                temperatureSensor.getLastUpdated().atOffset(UTC)
        );
    }

    public static ThermostatTelemetry mapThermostat(Thermostat thermostat) {
        return ThermostatTelemetry.of(
                thermostat.getDeviceId(),
                thermostat.getCurrentTemperature(),
                thermostat.getTargetTemperature(),
                thermostat.getHumidity(),
                thermostat.getMode().name(),
                thermostat.getStatus().name(),
                thermostat.getFirmwareVersion(),
                thermostat.getLastUpdated().atOffset(UTC)
        );
    }
}
