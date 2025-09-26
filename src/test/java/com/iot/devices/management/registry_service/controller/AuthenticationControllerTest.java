package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.errors.ErrorHandler;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import com.iot.devices.management.registry_service.security.AppConfig;
import com.iot.devices.management.registry_service.security.JwtAuthentificationFilter;
import com.iot.devices.management.registry_service.security.JwtService;
import com.iot.devices.management.registry_service.security.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthenticationController.class,
        ErrorHandler.class
})
@Import({
        JwtService.class,
        SecurityConfig.class,
        AppConfig.class,
        JwtAuthentificationFilter.class
})
class AuthenticationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;
    @MockitoBean
    AuthenticationManager authenticationManager;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userService);
    }

    String username = "jonndoe123";
    String firstName = "John";
    String lastName = "Doe";
    String phone = "+12345678";
    String email = "someemail@gmail.com";
    String address = "St. Privet";
    String passwordHash = "jwheknrmlear";

    UUID USER_ID = UUID.randomUUID();
    User USER = new User(USER_ID, username, firstName, lastName, email, phone, address, passwordHash,
            UserRole.USER, now(), now(), now(), new HashSet<>());


    @Test
    void login() throws Exception {
        String json = """
            {
              "username": "%s",
              "password"  : "%s"
            }
            """.formatted(username, passwordHash);
        when(userService.findByUsername(username)).thenReturn(Optional.of(USER));
        mockMvc.perform(post("/api/v1/authentication/login")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
        verify(userService).findByUsername(any());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void loginError() throws Exception {
        String json = """
            {
              "username": "%s",
              "password"  : "%s"
            }
            """.formatted(username, passwordHash);
        when(authenticationManager.authenticate(any())).thenThrow(new AccountExpiredException("error"));
        mockMvc.perform(post("/api/v1/authentication/login")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
        verify(authenticationManager).authenticate(any());
    }
}