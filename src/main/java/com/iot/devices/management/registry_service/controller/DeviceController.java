package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.dto.DeviceDTO;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.PatchDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.Utils;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.CreateDeviceOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.GetDeviceByIdOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.RemoveDeviceByIdOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.UpdateDeviceOpenApi;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

import static com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.*;
import static com.iot.devices.management.registry_service.controller.util.Utils.*;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "CRUD operations for Devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final UserService userService;

    @PostMapping
    @CreateDeviceOpenApi
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody @Valid CreateDeviceRequest request, Authentication auth) {
        final Optional<User> owner = loadUser(request.ownerId());
        if (!isAdmin(auth) && !isTheSameUser(owner, auth)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        final Optional<Device> device = deviceService.findBySerialNumber(request.serialNumber());
        if (device.isPresent()) {
            throw new DuplicateDeviceException(request.serialNumber());
        }
        final Device saved = deviceService.save(request, owner.orElse(null));
        return ResponseEntity.created(getLocation(saved.getId()))
                .body(getDeviceInfo(saved));
    }

    @PatchMapping
    @UpdateDeviceOpenApi
    public ResponseEntity<DeviceDTO> patchDevice(@RequestBody @Valid PatchDeviceRequest request, Authentication auth) {
        final Optional<User> owner = loadUser(request.ownerId());
        if (!isAdmin(auth) && !isTheSameUser(owner, auth)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        final Device patched = deviceService.patch(request, owner.orElse(null));
        return ResponseEntity.ok(getDeviceInfo(patched));
    }

    @GetMapping("{deviceId}")
    @GetDeviceByIdOpenApi
    @RateLimiter(name = "get_device_limiter", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<DeviceDTO> getDevice(@PathVariable @NonNull UUID deviceId, Authentication auth) {
        final Optional<Device> device = deviceService.findByDeviceId(deviceId);
        final Optional<User> owner = device.map(Device::getOwner);
        if (!isAdmin(auth) && !isTheSameUser(owner, auth)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        return device.map(Utils::getDeviceInfo)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    @DeleteMapping("{deviceId}")
    @RemoveDeviceByIdOpenApi
    public ResponseEntity<Void> deleteDevice(@PathVariable @NonNull UUID deviceId, Authentication auth) {
        final Optional<Device> device = deviceService.findByDeviceId(deviceId);
        final Optional<User> owner = device.map(Device::getOwner);
        if (!isAdmin(auth) && !isTheSameUser(owner, auth)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        if (device.isEmpty()) {
            throw new DeviceNotFoundException(deviceId);
        }
        deviceService.removeById(deviceId);
        return ResponseEntity.noContent().build();
    }

    //not redundant, used when method getDevice() achieved max retries
    public ResponseEntity<DeviceDTO>  rateLimitFallback(UUID deviceId, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    private Optional<User> loadUser(UUID userId) {
        return ofNullable(userId).flatMap(userService::findByUserId);
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "BooleanMethodIsAlwaysInverted"})
    private boolean isTheSameUser(Optional<User> owner, Authentication auth) {
        return owner.stream().anyMatch(o -> o.getUsername().equals(auth.getName()));
    }
}
