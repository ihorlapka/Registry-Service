package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.dto.DeviceDTO;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.PatchDeviceRequest;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.CreateDeviceOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.GetDeviceByIdOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.RemoveDeviceByIdOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.devices.UpdateDeviceOpenApi;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "CRUD operations for Devices")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    @CreateDeviceOpenApi
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody @Valid CreateDeviceRequest request) {
        final Optional<Device> device = deviceService.findBySerialNumber(request.serialNumber());
        if (device.isPresent()) {
            throw new DuplicateDeviceException(request.serialNumber());
        }
        final Device saved = deviceService.save(request);
        final URI location = getLocation(saved);
        final DeviceDTO deviceDTO = getDeviceInfo(saved);
        return ResponseEntity.created(location).body(deviceDTO);
    }

    @PatchMapping
    @UpdateDeviceOpenApi
    public ResponseEntity<DeviceDTO> patchDevice(@RequestBody @Valid PatchDeviceRequest request) {
        final Device patched = deviceService.patch(request);
        final DeviceDTO deviceDTO = getDeviceInfo(patched);
        return ResponseEntity.ok(deviceDTO);
    }

    @GetMapping("{deviceId}")
    @GetDeviceByIdOpenApi
    @RateLimiter(name = "get_device_limiter", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<DeviceDTO> getDevice(@PathVariable @NonNull UUID deviceId) {
        final Optional<Device> device = deviceService.findByDeviceId(deviceId);
        return device.map(this::getDeviceInfo)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    @DeleteMapping("{deviceId}")
    @RemoveDeviceByIdOpenApi
    public ResponseEntity<Void> deleteDevice(@PathVariable @NonNull UUID deviceId) {
        final int removedDevice = deviceService.removeById(deviceId);
        if (removedDevice < 1) {
            throw new DeviceNotFoundException(deviceId);
        }
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<DeviceDTO>  rateLimitFallback(UUID deviceId, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    private URI getLocation(Device saved) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
    }

    private DeviceDTO getDeviceInfo(Device device) {
        UUID ownerId = (device.getOwner() != null) ? device.getOwner().getId() : null;
        return new DeviceDTO(device.getId(), device.getName(), device.getSerialNumber(),
                device.getDeviceManufacturer(), device.getModel(), device.getDeviceType(),
                device.getLocation(), device.getLatitude(), device.getLongitude(), ownerId,
                device.getStatus(), device.getLastActiveAt(), device.getFirmwareVersion(),
                device.getCreatedAt(), device.getUpdatedAt());
    }
}
