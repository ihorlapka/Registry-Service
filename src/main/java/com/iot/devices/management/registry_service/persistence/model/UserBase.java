package com.iot.devices.management.registry_service.persistence.model;

import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;

import java.util.UUID;

public interface UserBase {

    UUID getId();
    String getUsername();
    UserRole getUserRole();
}
