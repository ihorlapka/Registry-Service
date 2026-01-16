package com.iot.devices.management.registry_service.controller.util;

import com.iot.devices.management.registry_service.controller.dto.AlertRuleDto;
import com.iot.devices.management.registry_service.controller.dto.DeviceDto;
import com.iot.devices.management.registry_service.controller.dto.UserDto;
import com.iot.devices.management.registry_service.persistence.model.AlertRule;
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
        return owner.map(user ->  authRole.getLevel() < user.getUserRole().getLevel())
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
        return userToBePatched.map(user -> authRole.getLevel() < user.getUserRole().getLevel())
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

    public static DeviceDto mapDevice(Device device) {
        final UUID ownerId = (device.getOwner() != null) ? device.getOwner().getId() : null;
        return new DeviceDto(device.getId(), device.getName(), device.getSerialNumber(),
                device.getDeviceManufacturer(), device.getModel(), device.getDeviceType(),
                device.getLocation(), device.getLatitude(), device.getLongitude(), ownerId,
                device.getStatus(), device.getLastActiveAt(), device.getFirmwareVersion(),
                device.getCreatedAt(), device.getUpdatedAt());
    }

    public static UserDto mapUser(User saved) {
        return new UserDto(saved.getId(), saved.getUsername(), saved.getFirstName(), saved.getLastName(),
                saved.getPhone(), saved.getEmail(), saved.getAddress(), mapDevices(saved));
    }

    private static Set<DeviceDto> mapDevices(User user) {
        return user.getDevices().stream()
                .map(Utils::mapDevice)
                .collect(toSet());
    }

    public static AlertRuleDto mapAlertRuleToDto(AlertRule dbRule) {
        return new AlertRuleDto(dbRule.getRuleId(), dbRule.getMetricType(),
                dbRule.getThresholdType(), dbRule.getThresholdValue(), dbRule.getSeverity(), dbRule.isEnabled());
    }
}
