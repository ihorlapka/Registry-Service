package com.iot.devices.management.registry_service.controller.dto;

import java.util.Set;
import java.util.UUID;

public record UserDTO(UUID id,
                      String username,
                      String firstName,
                      String lastName,
                      String phone,
                      String email,
                      String address,
                      Set<DeviceDTO> devices) {
}
