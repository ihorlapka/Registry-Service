package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import lombok.NonNull;
import org.springframework.lang.Nullable;

public record PatchUserRequest(
        @NonNull String username,
        @Nullable String newUsername,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable @Email String email,
        @Nullable String phone,
        @Nullable String address,
        @Nullable String password,
        @Nullable String newPassword,
        @Nullable String confirmationPassword,
        @Nullable UserRole userRole) {
}
