package com.iot.devices.management.registry_service.controller.util;

public record UserDTO(Long id,
                      String firstName,
                      String lastName,
                      String phone,
                      String email,
                      String address) {
}
