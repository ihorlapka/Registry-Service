package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.controller.dto.DeviceDTO;
import com.iot.devices.management.registry_service.controller.dto.UserDTO;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

import static com.iot.devices.management.registry_service.persistence.model.enums.UserRole.*;
import static java.util.stream.Collectors.toSet;

@UtilityClass
public class Utils {

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "OptionalUsedAsFieldOrParameterType"})
    public static boolean hasPermission(Authentication auth, Optional<User> owner) {
        final UserRole authRole = getMinRoleLevel(auth);
        if (owner.isEmpty() && USER.equals(authRole)) {
            return false;
        }
        if (owner.map(o -> o.getUsername().equals(auth.getName())).orElse(false)) {
            return true;
        }
        return owner.map(user -> user.getUserRole().getLevel() < authRole.getLevel())
                .orElse(true);
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "OptionalUsedAsFieldOrParameterType"})
    public static boolean hasPatchPermission(Authentication auth, Optional<User> userToBePatched, PatchUserRequest request) {
        final UserRole authRole = getMinRoleLevel(auth);
        if (userToBePatched.isEmpty()) {
            return !USER.equals(authRole);
        }
        if (userToBePatched.map(u -> u.getUsername().equals(auth.getName())).orElse(false)) {
            return Objects.equals(request.userRole(), authRole) || request.userRole() == null;
        }
        return userToBePatched.map(user -> user.getUserRole().getLevel() < authRole.getLevel())
                .orElse(true);
    }

    private UserRole getMinRoleLevel(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(UserRole::findByRole)
                .min(Comparator.comparingInt(UserRole::getLevel))
                .orElseThrow(() -> new NoSuchElementException("No authorities present for user: " + authentication.getName()));
    }

    public static URI getLocation(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    public static DeviceDTO getDeviceInfo(Device device) {
        final UUID ownerId = (device.getOwner() != null) ? device.getOwner().getId() : null;
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
