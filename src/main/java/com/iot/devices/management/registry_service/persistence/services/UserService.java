package com.iot.devices.management.registry_service.persistence.services;

import com.iot.devices.management.registry_service.controller.util.CreateUserRequest;
import com.iot.devices.management.registry_service.controller.util.PatchUserRequest;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.repos.UsersRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;

    public Optional<User> findByEmail(@NonNull @NotBlank
                                      @Email(message = "Email must be valid")
                                      String email) {
        return usersRepository.findByEmail(email);
    }

    public User save(CreateUserRequest request) {
        final User userEntity = mapNewUser(request);
        return usersRepository.save(userEntity);
    }

    public User patch(PatchUserRequest request, User user) {
        final User patched = patchUser(request,  user);
        return usersRepository.save(patched);
    }

    public Optional<User> findById(@NonNull UUID id) {
        return usersRepository.findById(id);
    }

    public Page<User> findAll(Pageable pageable) {
        return usersRepository.findAll(pageable);
    }

    public int removeById(UUID id) {
        return usersRepository.removeById(id);
    }

    private User mapNewUser(CreateUserRequest request) {
        UserRole userRole = ofNullable(request.userRole()).orElse(UserRole.USER);
        return new User(null, request.username(), request.firstName(), request.lastName(),
                request.email(), request.phone(), request.address(), request.passwordHash(),
                userRole, now(), now(), now(), new HashSet<>());
    }

    private User patchUser(PatchUserRequest request, User user) {
        ofNullable(request.username()).ifPresent(user::setUsername);
        ofNullable(request.firstName()).ifPresent(user::setFirstName);
        ofNullable(request.lastName()).ifPresent(user::setLastName);
        ofNullable(request.email()).ifPresent(user::setEmail);
        ofNullable(request.phone()).ifPresent(user::setPhone);
        ofNullable(request.address()).ifPresent(user::setAddress);
        ofNullable(request.passwordHash()).ifPresent(user::setPasswordHash);
        ofNullable(request.userRole()).ifPresent(user::setUserRole);
        user.setUpdatedAt(now());
        return user;
    }
}
