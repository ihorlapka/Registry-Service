package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.util.PatchUserRequest;
import com.iot.devices.management.registry_service.controller.dto.UserDTO;
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
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "CRUD operations for users")
public class UserController {

    private final UserService userService;

    @PostMapping("/registerUser")
    @CreateUserOpenApi
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid CreateUserRequest request) {
        return registerUser(request, USER);
    }

    @PostMapping("/registerAdmin")
    @CreateAdminOpenApi
    public ResponseEntity<UserDTO> createAdmin(@RequestBody @Valid CreateUserRequest request) {
        return registerUser(request, ADMIN);
    }

    private ResponseEntity<UserDTO> registerUser(CreateUserRequest request, UserRole role) {
        final Optional<User> user = userService.findByEmail(request.email());
        if (user.isPresent()) {
            throw new DuplicateUserException(request.email());
        }
        final User saved = userService.save(request, role);
        return ResponseEntity.created(getLocation(saved.getId())).body(getUserInfo(saved));
    }

    @PatchMapping
    @UpdateUserOpenApi
    public ResponseEntity<UserDTO> patchUser(@RequestBody @Valid PatchUserRequest request, Authentication auth) {
        final Optional<User> user = userService.findByUsername(request.username());
        if (!hasPermission(auth, user)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        if (user.isEmpty()) {
            throw new UserNotFoundException(request.username());
        }
        final User saved = userService.patch(request, user.get());
        return ResponseEntity.ok(getUserInfo(saved));
    }

    @GetMapping("/all")
    @GetAllUsersOpenApi
    public ResponseEntity<List<UserDTO>> getAllUsers(Pageable pageable) {
        final Page<User> users = userService.findAll(pageable);
        final List<UserDTO> userDTOS = users.stream().map(Utils::getUserInfo).toList();
        return ResponseEntity.ok(userDTOS);
    }

    @GetMapping("/me")
    @GetMyUserOpenApi
    public ResponseEntity<UserDTO> getMyUser(Authentication auth) {
        final Optional<User> user = userService.findByUsername(auth.getName());
        if (user.isEmpty()) {
            throw new UserNotFoundException(auth.getName());
        }
        return ResponseEntity.ok(getUserInfo(user.get()));
    }

    @GetMapping("{userId}")
    @GetUserByIdOpenApi
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID userId) {
        final Optional<User> user = userService.findByUserId(userId);
        return user.map(u -> ResponseEntity.ok(getUserInfo(u)))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @GetMapping("/username/{username}")
    @GetUserByUsernameOpenApi
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        final Optional<User> user = userService.findByUsername(username);
        return user.map(u -> ResponseEntity.ok(getUserInfo(u)))
                .orElseThrow(() -> new UserNotFoundException(username));
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
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId, Authentication auth) {
        final Optional<User> user = userService.findByUserId(userId);
        if (!hasPermission(auth, user)) {
            return ResponseEntity.status(FORBIDDEN).build();
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
