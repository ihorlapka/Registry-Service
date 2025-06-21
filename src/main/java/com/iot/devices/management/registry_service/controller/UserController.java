package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.util.PatchUserRequest;
import com.iot.devices.management.registry_service.controller.util.UserDTO;
import com.iot.devices.management.registry_service.controller.util.CreateUserRequest;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.repos.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import com.iot.devices.management.registry_service.controller.errors.UserExceptions.*;

import static java.util.Optional.ofNullable;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UsersRepository repo;

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        final Optional<User> user = repo.findByEmail(request.email());
        if (user.isPresent()) {
            throw new DuplicateUserException(request.email());
        }
        final User userEntity = mapUser(request);
        final User saved = repo.save(userEntity);
        final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        final UserDTO userDTO = getUserInfo(saved);
        return ResponseEntity.created(location).body(userDTO);
    }

    @PatchMapping
    public ResponseEntity<UserDTO> patchUser(@RequestBody PatchUserRequest request) {
        final Optional<User> user = repo.findById(request.id());
        if (user.isEmpty()) {
            throw new UserNotFoundException(request.id());
        }
        final User patched = patch(request,  user.get());
        final User saved = repo.save(patched);
        final UserDTO userDTO = getUserInfo(saved);
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        final List<User> users = repo.findAll();
        final List<UserDTO> userDTOS = users.stream().map(this::getUserInfo).toList();
        return ResponseEntity.ok(userDTOS);
    }

    @GetMapping("{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        final Optional<User> user = repo.findById(id);
        return user.map(u -> ResponseEntity.ok(getUserInfo(u)))
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @GetMapping("{firstName}/{lastName}")
    public ResponseEntity<UserDTO> findByEmail(@PathVariable String email) {
        final Optional<User> user = repo.findByEmail(email);
        return user.map(u -> ResponseEntity.ok(getUserInfo(u)))
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        final int removedUser = repo.removeById(id);
        if (removedUser < 1) {
            throw new UserNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }

    private User mapUser(CreateUserRequest request) {
        return new User(null, request.firstName(), request.lastName(),
                request.email(), request.phone(), request.address());
    }

    private User patch(PatchUserRequest request, User user) {
        ofNullable(request.firstName()).ifPresent(user::setFirstName);
        ofNullable(request.lastName()).ifPresent(user::setLastName);
        ofNullable(request.phone()).ifPresent(user::setPhone);
        ofNullable(request.email()).ifPresent(user::setEmail);
        ofNullable(request.address()).ifPresent(user::setAddress);
        return user;
    }

    private UserDTO getUserInfo(User saved) {
        return new UserDTO(saved.getId(), saved.getFirstName(), saved.getLastName(),
                saved.getPhone(), saved.getEmail(), saved.getAddress());
    }
}
