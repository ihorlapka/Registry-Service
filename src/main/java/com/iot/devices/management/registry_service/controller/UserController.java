package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.dto.DeviceDTO;
import com.iot.devices.management.registry_service.controller.util.PatchUserRequest;
import com.iot.devices.management.registry_service.controller.dto.UserDTO;
import com.iot.devices.management.registry_service.controller.util.CreateUserRequest;
import com.iot.devices.management.registry_service.open.api.custom.annotations.*;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.iot.devices.management.registry_service.controller.errors.UserExceptions.*;

import static java.util.stream.Collectors.toSet;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "CRUD operations for users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @CreateUserOpenApi
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid CreateUserRequest request) {
        final Optional<User> user = userService.findByEmail(request.email());
        if (user.isPresent()) {
            throw new DuplicateUserException(request.email());
        }
        final User saved = userService.save(request);
        final URI location = getLocation(saved);
        final UserDTO userDTO = getUserInfo(saved);
        return ResponseEntity.created(location).body(userDTO);
    }

    @PatchMapping
    @UpdateUserOpenApi
    public ResponseEntity<UserDTO> patchUser(@RequestBody @Valid PatchUserRequest request) {
        final Optional<User> user = userService.findByUserId(request.id());
        if (user.isEmpty()) {
            throw new UserNotFoundException(request.id());
        }
        final User saved = userService.patch(request, user.get());
        final UserDTO userDTO = getUserInfo(saved);
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping
    @Hidden
    public ResponseEntity<List<UserDTO>> getAllUsers(Pageable pageable) {
        final Page<User> users = userService.findAll(pageable);
        final List<UserDTO> userDTOS = users.stream().map(this::getUserInfo).toList();
        return ResponseEntity.ok(userDTOS);
    }

    @GetMapping("{userId}")
    @GetUserByIdOpenApi
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID userId) {
        final Optional<User> user = userService.findByUserId(userId);
        return user.map(u -> ResponseEntity.ok(getUserInfo(u)))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @GetMapping("email/{email}")
    @GetUserByEmailOpenApi
    public ResponseEntity<UserDTO> findByEmail(@PathVariable @Valid String email) {
        final Optional<User> user = userService.findByEmail(email);
        return user.map(u -> ResponseEntity.ok(getUserInfo(u)))
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @DeleteMapping("/{userId}")
    @RemoveUserByIdOpenApi
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        final int removedUser = userService.removeById(userId);
        if (removedUser < 1) {
            throw new UserNotFoundException(userId);
        }
        return ResponseEntity.noContent().build();
    }

    private URI getLocation(User saved) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
    }

    private UserDTO getUserInfo(User saved) {
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
