package com.iot.devices.management.registry_service.persistence.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceManufacturer {
    SAMSUNG("Samsung"),
    CISCO_SYSTEMS("Cisco_Systems"),
    HONEYWELL("Honeywell"),
    BOSCH("Bosch"),
    SIEMENS("Siemens"),
    PHILIPS_HUE("Philips_Hue"),
    FITBIT("Fitbit"),
    AMAZON("Amazon"),
    TEXAS_INSTRUMENTS("Texas_Instruments"),
    SCHNEIDER_ELECTRIC("Schneider_Electric");

    private final String name;
}
