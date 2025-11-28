package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.errors.GlobalExceptionHandler;
import com.iot.devices.management.registry_service.persistence.model.Token;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.repos.TokenRepository;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import com.iot.devices.management.registry_service.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.OffsetDateTime;
import java.util.*;

import static com.iot.devices.management.registry_service.security.JwtAuthentificationFilter.TOKEN_PREFIX;
import static java.time.OffsetDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = {
        AuthenticationController.class,
        GlobalExceptionHandler.class
})
@Import({
        JwtService.class,
        SecurityConfig.class,
        AppConfig.class,
        JwtAuthentificationFilter.class,
        SecurityProperties.class,
        LogoutService.class
})
class AuthenticationControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;

    @MockitoBean
    UserService userService;
    @MockitoBean
    AuthenticationManager authenticationManager;
    @MockitoBean
    TokenRepository tokenRepository;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userService, tokenRepository, authenticationManager);
    }

    String username = "testUser";
    String firstName = "John";
    String lastName = "Doe";
    String phone = "+12345678";
    String email = "someemail@gmail.com";
    String address = "St. Privet";
    String passwordHash = "jwheknrmlear";

    UUID USER_ID = UUID.randomUUID();
    User USER = new User(USER_ID, username, firstName, lastName, email, phone, address, passwordHash,
            UserRole.USER, now(), now(), now(), new HashSet<>(), new ArrayList<>());


    @Test
    void loginLogout() throws Exception {
        String json = """
            {
              "username": "%s",
              "password"  : "%s"
            }
            """.formatted(username, passwordHash);

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        String jwt = jwtService.generateTokens(USER).getAccessToken();
        headers.add(AUTHORIZATION, TOKEN_PREFIX + jwt);

        when(userService.findByUsername(username)).thenReturn(Optional.of(USER));
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(Token.builder()
                .token(jwt).user(USER).revoked(false).expired(false).refresh(false).build()));
        when(tokenRepository.removeAllByUserId(any())).thenReturn(2);
        when(userService.updateLastLoginTime(eq(USER_ID), any(OffsetDateTime.class))).thenReturn(1);
        mockMvc.perform(post("/api/v1/authentication/login")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/authentication/logout")
                        .contentType(APPLICATION_JSON)
                        .headers(HttpHeaders.readOnlyHttpHeaders(headers)))
                .andExpect(status().isOk());

        verify(userService).findByUsername(any());
        verify(authenticationManager).authenticate(any());
        verify(tokenRepository, times(2)).saveAll(anyList());
        verify(userService).updateLastLoginTime(eq(USER_ID), any(OffsetDateTime.class));
        verify(tokenRepository).findByToken(anyString());
        verify(tokenRepository).removeAllByUserId(any());
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