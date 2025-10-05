package com.iot.devices.management.registry_service.persistence.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER(3, "ROLE_USER"),
    MANAGER(2, "ROLE_MANAGER"),
    ADMIN(1, "ROLE_ADMIN"),
    SUPER_ADMIN(0, "ROLE_SUPER_ADMIN");

    private final int level;
    private final String roleName;

    public static UserRole findByRole(String role) {
        for (UserRole userRole : values()) {
            if (userRole.getRoleName().equals(role)) {
                return userRole;
            }
        }
        throw new IllegalArgumentException("No userRole present: " + role);
    }
}
