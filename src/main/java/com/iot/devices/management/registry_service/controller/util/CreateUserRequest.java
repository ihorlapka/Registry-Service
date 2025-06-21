package com.iot.devices.management.registry_service.controller.util;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

public record CreateUserRequest(
        @NonNull @NotBlank String firstName,
        @NonNull @NotBlank String lastName,
        @NonNull @NotBlank String phone,
        @NonNull @Email String email,
        @NonNull @NotBlank String address) {
}
