package com.iot.devices.management.registry_service.controller.util;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

public record CreateUserRequest(
        @NonNull
        @NotBlank(message = "username is required")
        String username,

        @NonNull
        @NotBlank(message = "First name is required")
        String firstName,

        @NonNull
        @NotBlank(message = "Last name is required")
        String lastName,

        @NonNull
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NonNull
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9\\- ]{7,20}$", message = "Phone must be a valid number")
        String phone,

        @NonNull
        @NotBlank(message = "Address is required")
        @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
        String address,

        @NonNull
        @NotBlank(message = "Password is required")
        String password
        ) {
}
