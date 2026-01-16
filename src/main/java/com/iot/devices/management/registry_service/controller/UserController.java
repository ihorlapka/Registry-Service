package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.util.PatchUserRequest;
import com.iot.devices.management.registry_service.controller.dto.UserDto;
import com.iot.devices.management.registry_service.controller.util.CreateUserRequest;
import com.iot.devices.management.registry_service.controller.util.Utils;
import com.iot.devices.management.registry_service.open.api.custom.annotations.users.*;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.iot.devices.management.registry_service.controller.errors.UserExceptions.*;

import java.util.*;

import static com.iot.devices.management.registry_service.controller.util.Utils.*;
import static com.iot.devices.management.registry_service.persistence.model.enums.UserRole.*;
import static com.iot.devices.management.registry_service.controller.errors.UserExceptions.PermissionDeniedException;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "CRUD operations for users")
public class UserController {

    private final UserService userService;

    @PostMapping("/registerUser")
    @CreateUserOpenApi
    public ResponseEntity<UserDto> createUser(@RequestBody @Valid CreateUserRequest request) {
        return registerUser(request, USER);
    }

    @PostMapping("/registerAdmin")
    @CreateAdminOpenApi
    public ResponseEntity<UserDto> createAdmin(@RequestBody @Valid CreateUserRequest request) {
        return registerUser(request, ADMIN);
    }

    private ResponseEntity<UserDto> registerUser(CreateUserRequest request, UserRole role) {
        final Optional<User> user = userService.findByEmail(request.email());
        if (user.isPresent()) {
            throw new DuplicateUserException(request.email());
        }
        final User saved = userService.save(request, role);
        return ResponseEntity.created(getLocation(saved.getId())).body(mapUser(saved));
    }

    @PatchMapping
    @UpdateUserOpenApi
    public ResponseEntity<UserDto> patchUser(@RequestBody @Valid PatchUserRequest request, Authentication auth) {
        final Optional<User> user = userService.findByUsername(request.username());
        if (user.isEmpty()) {
            throw new UserNotFoundException(request.username());
        }
        if (!hasPatchPermission(auth, user.get(), request)) {
            throw new PermissionDeniedException(auth.getName());
        }
        final User saved = userService.patch(request, user.get());
        return ResponseEntity.ok(mapUser(saved));
    }

    @GetMapping("/all")
    @GetAllUsersOpenApi
    public ResponseEntity<List<UserDto>> getAllUsers(Pageable pageable) {
        final Page<User> users = userService.findAll(pageable);
        final List<UserDto> userDTOS = users.stream().map(Utils::mapUser).toList();
        return ResponseEntity.ok(userDTOS);
    }

    @GetMapping("/me")
    @GetMyUserOpenApi
    public ResponseEntity<UserDto> getMyUser(Authentication auth) {
        final Optional<User> user = userService.findByUsername(auth.getName());
        if (user.isEmpty()) {
            throw new UserNotFoundException(auth.getName());
        }
        return ResponseEntity.ok(mapUser(user.get()));
    }

    @GetMapping("{userId}")
    @GetUserByIdOpenApi
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID userId) {
        final Optional<User> user = userService.findByUserId(userId);
        return user.map(u -> ResponseEntity.ok(mapUser(u)))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @GetMapping("/username/{username}")
    @GetUserByUsernameOpenApi
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        final Optional<User> user = userService.findByUsername(username);
        return user.map(u -> ResponseEntity.ok(mapUser(u)))
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    @GetMapping("email/{email}")
    @GetUserByEmailOpenApi
    public ResponseEntity<UserDto> findByEmail(@PathVariable @Valid String email) {
        final Optional<User> user = userService.findByEmail(email);
        return user.map(u -> ResponseEntity.ok(mapUser(u)))
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @DeleteMapping("/{userId}")
    @RemoveUserByIdOpenApi
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId, Authentication auth) {
        final Optional<User> user = userService.findByUserId(userId);
        if (!hasPermission(auth, user)) {
            throw new PermissionDeniedException(auth.getName());
        }
        if (user.isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        final int removedUser = userService.removeById(userId);
        if (removedUser == 1) {
            log.info("User with id: {} is removed", userId);
        }
        return ResponseEntity.noContent().build();
    }
}
