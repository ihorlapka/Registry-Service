package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.controller.dto.DeviceDTO;
import com.iot.devices.management.registry_service.controller.dto.UserDTO;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import static com.iot.devices.management.registry_service.persistence.model.enums.UserRole.ADMIN;
import static com.iot.devices.management.registry_service.persistence.model.enums.UserRole.SUPER_ADMIN;
import static java.util.stream.Collectors.toSet;

@UtilityClass
public class Utils {

    private static final String ROLE_PREFIX = "ROLE_";

    public static boolean isAdmin(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.contains(new SimpleGrantedAuthority(ROLE_PREFIX + ADMIN.name())) ||
                authorities.contains(new SimpleGrantedAuthority(ROLE_PREFIX + SUPER_ADMIN.name()));
    }

    public static URI getLocation(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    public static DeviceDTO getDeviceInfo(Device device) {
        UUID ownerId = (device.getOwner() != null) ? device.getOwner().getId() : null;
        return new DeviceDTO(device.getId(), device.getName(), device.getSerialNumber(),
                device.getDeviceManufacturer(), device.getModel(), device.getDeviceType(),
                device.getLocation(), device.getLatitude(), device.getLongitude(), ownerId,
                device.getStatus(), device.getLastActiveAt(), device.getFirmwareVersion(),
                device.getCreatedAt(), device.getUpdatedAt());
    }

    public static UserDTO getUserInfo(User saved) {
        return new UserDTO(saved.getId(), saved.getUsername(), saved.getFirstName(), saved.getLastName(),
                saved.getPhone(), saved.getEmail(), saved.getAddress(), mapDevices(saved));
    }

    private static Set<DeviceDTO> mapDevices(User saved) {
        return saved.getDevices().stream()
                .map(device -> new DeviceDTO(device.getId(),
                        device.getName(), device.getSerialNumber(),
                        device.getDeviceManufacturer(), device.getModel(), device.getDeviceType(),
                        device.getLocation(), device.getLatitude(), device.getLongitude(),
                        device.getOwner().getId(), device.getStatus(), device.getLastActiveAt(),
                        device.getFirmwareVersion(), device.getCreatedAt(), device.getUpdatedAt()))
                .collect(toSet());
    }
}
