package com.iot.devices.management.registry_service.controller.util;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import lombok.NonNull;

public record PatchUserRequest(
        @NonNull Long id,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable String phone,
        @Nullable @Email String email,
        @Nullable String address) {
}
