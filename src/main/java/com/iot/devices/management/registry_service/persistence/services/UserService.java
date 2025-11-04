package com.iot.devices.management.registry_service.persistence.services;

import com.iot.devices.management.registry_service.controller.errors.UserExceptions.UserNotFoundException;
import com.iot.devices.management.registry_service.controller.util.CreateUserRequest;
import com.iot.devices.management.registry_service.controller.util.PatchUserRequest;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.repos.UsersRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.iot.devices.management.registry_service.cache.CacheConfig.USERS_CACHE;
import static java.time.OffsetDateTime.now;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = USERS_CACHE, sync = true)
    public Optional<User> findByEmail(@NonNull @NotBlank
                                      @Email(message = "Email must be valid")
                                      String email) {
        return usersRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(@NonNull @NotBlank String username) {
        return usersRepository.findByUsername(username);
    }

    @Transactional
    public User save(CreateUserRequest request, UserRole role) {
        final User userEntity = mapNewUser(request, role);
        return usersRepository.save(userEntity);
    }

    @Transactional
    public User save(User user) {
        return usersRepository.save(user);
    }

    @Transactional
    @Caching(put = {
            @CachePut(value = USERS_CACHE, key = "#user.id"),
            @CachePut(value = USERS_CACHE, key = "#user.email")
    })
    public User patch(PatchUserRequest request, User user) {
        final User patched = patchUser(request, user);
        return usersRepository.save(patched);
    }

    @Cacheable(value = USERS_CACHE, sync = true)
    public Optional<User> findByUserId(@NonNull UUID id) {
        return usersRepository.findById(id);
    }

    public Page<User> findAll(Pageable pageable) {
        return usersRepository.findAll(pageable);
    }

    @Transactional
    public int removeById(UUID id) {
        final Optional<User> user = usersRepository.findById(id);
        if (user.isPresent()) {
            int removed = usersRepository.removeById(id);
            evictUserCache(user.get().getId(), user.get().getEmail());
            return removed;
        }
        throw new UserNotFoundException(id);
    }

    @Transactional
    public int updateLastLoginTime(UUID userId, OffsetDateTime loginTime) {
        return usersRepository.updateLastLoginTime(userId, loginTime);
    }

    @Caching(evict = {
            @CacheEvict(value = "usersCache", key = "#id"),
            @CacheEvict(value = "usersCache", key = "#email")
    })
    private void evictUserCache(UUID id, String email) {
    }

    private User mapNewUser(CreateUserRequest request, UserRole role) {
        UserRole userRole = ofNullable(role).orElse(UserRole.USER);
        return new User(null, request.username(), request.firstName(), request.lastName(),
                request.email(), request.phone(), request.address(), passwordEncoder.encode(request.password()),
                userRole, now(), null, null, new HashSet<>(), new ArrayList<>());
    }

    private User patchUser(PatchUserRequest request, User user) {
        ofNullable(request.newUsername()).ifPresent(user::setUsername);
        ofNullable(request.firstName()).ifPresent(user::setFirstName);
        ofNullable(request.lastName()).ifPresent(user::setLastName);
        ofNullable(request.email()).ifPresent(user::setEmail);
        ofNullable(request.phone()).ifPresent(user::setPhone);
        ofNullable(request.address()).ifPresent(user::setAddress);
        ofNullable(request.newPassword()).ifPresent(pass -> changePassword(request, user));
        ofNullable(request.userRole()).ifPresent(user::setUserRole);
        user.setUpdatedAt(now());
        return user;
    }

    private void changePassword(PatchUserRequest request, User user) {
        checkArgument(request.password() != null);
        checkArgument(request.confirmationPassword() != null);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalStateException("Current password is wrong");
        }
        if (!Objects.equals(request.newPassword(), request.confirmationPassword())) {
            throw new IllegalStateException("Password are not the same");
        }
        final String encodedPassword = passwordEncoder.encode(request.newPassword());
        if (Objects.equals(encodedPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password can not be the same as current one");
        }
        user.setPassword(encodedPassword);
    }
}
