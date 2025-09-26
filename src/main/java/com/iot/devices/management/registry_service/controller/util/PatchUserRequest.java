package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

public record PatchUserRequest(
        @NonNull UUID id,
        @NonNull String username,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable @Email String email,
        @Nullable String phone,
        @Nullable String address,
        @Nullable String password,
        @Nullable UserRole userRole) {
}
