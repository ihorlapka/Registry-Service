package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.dto.DeviceDto;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.PermissionToDeviceResponse;
import com.iot.devices.management.registry_service.controller.util.PatchDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.Utils;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.CreateDeviceOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.GetDeviceByIdOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.RemoveDeviceByIdOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.UpdateDeviceOpenApi;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.UserProjection;
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
import static com.iot.devices.management.registry_service.controller.errors.UserExceptions.PermissionDeniedException;

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
    public ResponseEntity<DeviceDto> createDevice(@RequestBody @Valid CreateDeviceRequest request, Authentication auth) {
        final Optional<User> owner = loadUser(request.ownerId());
        if (!hasPermission(auth, owner)) {
            throw new PermissionDeniedException(auth.getName());
        }
        final Optional<Device> device = deviceService.findBySerialNumber(request.serialNumber());
        if (device.isPresent()) {
            throw new DuplicateDeviceException(request.serialNumber());
        }
        final Device saved = deviceService.saveAndSendMessage(request, owner.orElse(null));
        return ResponseEntity.created(getLocation(saved.getId()))
                .body(mapDevice(saved));
    }

    @PatchMapping
    @UpdateDeviceOpenApi
    public ResponseEntity<DeviceDto> patchDevice(@RequestBody @Valid PatchDeviceRequest request, Authentication auth) {
        final Optional<User> owner = loadUser(request.ownerId());
        if (!hasPermission(auth, owner)) {
            throw new PermissionDeniedException(auth.getName());
        }
        final Device patched = deviceService.patch(request, owner.orElse(null));
        return ResponseEntity.ok(mapDevice(patched));
    }

    @GetMapping("{deviceId}")
    @GetDeviceByIdOpenApi
    @RateLimiter(name = "get_device_limiter", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<DeviceDto> getDevice(@PathVariable @NonNull UUID deviceId, Authentication auth) {
        final Optional<Device> device = deviceService.findByDeviceId(deviceId);
        final Optional<UserProjection> owner = userService.getUserProjectionByDevice(deviceId);
        if (!hasPermission(auth, owner)) {
            throw new PermissionDeniedException(auth.getName());
        }
        return device.map(Utils::mapDevice)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    @DeleteMapping("{deviceId}")
    @RemoveDeviceByIdOpenApi
    public ResponseEntity<Void> deleteDevice(@PathVariable @NonNull UUID deviceId, Authentication auth) {
        final Optional<Device> device = deviceService.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            throw new DeviceNotFoundException(deviceId);
        }
        final Optional<UserProjection> owner = userService.getUserProjectionByDevice(deviceId);
        if (!hasPermission(auth, owner)) {
            throw new PermissionDeniedException(auth.getName());
        }
        deviceService.removeById(deviceId, owner.orElse(null));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("permission/{deviceId}")
    public ResponseEntity<PermissionToDeviceResponse> checkPermissionToDevice(@PathVariable @NonNull UUID deviceId, Authentication auth) {
        final Optional<Device> device = deviceService.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            throw new DeviceNotFoundException(deviceId);
        }
        final Optional<UserProjection> owner = userService.getUserProjectionByDevice(deviceId);
        return ResponseEntity.ok(new PermissionToDeviceResponse(hasPermission(auth, owner)));
    }

    //not redundant, used when method getDevice() achieved max retries
    public ResponseEntity<DeviceDto>  rateLimitFallback(UUID deviceId, Authentication auth, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    private Optional<User> loadUser(UUID userId) {
        return ofNullable(userId).flatMap(userService::findByUserId);
    }
}
