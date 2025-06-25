package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.dto.DeviceDTO;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

import static com.iot.devices.management.registry_service.controller.errors.DeviceExceptions.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createDevice(@RequestBody @Valid CreateDeviceRequest request) {
        final Optional<Device> device = deviceService.findBySerialNumber(request.serialNumber());
        if (device.isPresent()) {
            throw new DuplicateDeviceException(request.serialNumber());
        }
        User owner = null;
        if (request.ownerId() != null) {
            owner = userService.findById(request.ownerId()).orElse(null);
        }
        final Device saved = deviceService.save(request, owner);
        final URI location = getLocation(saved);
        final DeviceDTO deviceDTO = getDeviceInfo(saved);
        return ResponseEntity.created(location).body(deviceDTO);
    }

    private static URI getLocation(Device saved) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
    }

    private DeviceDTO getDeviceInfo(Device saved) {
        return new DeviceDTO(saved.getId(), saved.getName(), saved.getSerialNumber(),
                saved.getDeviceManufacturer(), saved.getModel(), saved.getDeviceType(),
                saved.getLocation(), saved.getLatitude(), saved.getLongitude(), saved.getOwner().getId(),
                saved.getStatus(), saved.getLastActiveAt(), saved.getFirmwareVersion(),
                saved.getBatteryLevel(), saved.getCreatedAt(), saved.getUpdatedAt());
    }
}
