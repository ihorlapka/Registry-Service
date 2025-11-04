package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.errors.AuthenticationHeaderException;
import com.iot.devices.management.registry_service.controller.util.AuthenticationRequest;
import com.iot.devices.management.registry_service.controller.util.AuthenticationResponse;
import com.iot.devices.management.registry_service.open.api.custom.annotations.authentication.LoginOpenApi;
import com.iot.devices.management.registry_service.open.api.custom.annotations.authentication.RefreshTokenOpenApi;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import com.iot.devices.management.registry_service.security.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.iot.devices.management.registry_service.controller.errors.UserExceptions.*;
import static com.iot.devices.management.registry_service.security.JwtAuthentificationFilter.TOKEN_BEGIN_INDEX;
import static com.iot.devices.management.registry_service.security.JwtAuthentificationFilter.TOKEN_PREFIX;

@Slf4j
@RestController
@RequestMapping("/api/v1/authentication")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Operations for Authentication")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    @LoginOpenApi
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        log.info("user: {} is authenticated", request.username());
        final Optional<User> user = userService.findByUsername(request.username());
        if (user.isEmpty()) {
            throw new UserNotFoundException(request.username());
        }
        final AuthenticationResponse authenticationResponse = jwtService.generateTokens(user.get());
        int rows = userService.updateLastLoginTime(user.get().getId(), OffsetDateTime.now());
        if (rows == 1) {
            log.info("Updated login time to user: {}, id: {}", user.get().getUsername(), user.get().getId());
        } else {
            log.warn("Login time was updated to {} users, user: {}, id: {}", rows, user.get().getUsername(), user.get().getId());
        }
        return ResponseEntity.ok(authenticationResponse);
    }

    @PostMapping("/refresh-token")
    @RefreshTokenOpenApi
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            throw new AuthenticationHeaderException("Authentication header is invalid");
        }
        final String refreshToken = authHeader.substring(TOKEN_BEGIN_INDEX);
        final String username = jwtService.extractUsername(refreshToken);
        final Optional<User> user = userService.findByUsername(username);
        if (user.isEmpty()) {
            throw new UserNotFoundException(username);
        }
        final AuthenticationResponse authenticationResponse = jwtService.refreshToken(refreshToken, user.get(), response.getOutputStream());
        return ResponseEntity.ok(authenticationResponse);
    }
}
