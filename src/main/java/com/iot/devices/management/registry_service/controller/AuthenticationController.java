package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.errors.UserExceptions;
import com.iot.devices.management.registry_service.controller.util.AuthenticationRequest;
import com.iot.devices.management.registry_service.controller.util.AuthenticationResponse;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import com.iot.devices.management.registry_service.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
@RestController
@RequestMapping("/api/v1/authentication")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            log.info("user {} is authenticated", request.username());
            final Optional<User> user = userService.findByUsername(request.username());
            if (user.isEmpty()) {
                throw new UserExceptions.UserNotFoundException(request.username());
            }
            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .token(jwtService.generateToken(user.get()))
                    .build());
        } catch (AuthenticationException e) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
    }
}
