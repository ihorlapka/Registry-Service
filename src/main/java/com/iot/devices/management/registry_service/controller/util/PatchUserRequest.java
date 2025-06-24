package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.UserRole;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import lombok.NonNull;

import java.util.UUID;

public record PatchUserRequest(
        @NonNull UUID id,
        @Nullable String username,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable @Email String email,
        @Nullable String phone,
        @Nullable String address,
        @Nullable String passwordHash,
        @Nullable UserRole userRole) {
}
