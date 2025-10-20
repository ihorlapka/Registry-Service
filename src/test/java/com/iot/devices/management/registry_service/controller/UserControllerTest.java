package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.errors.GlobalExceptionHandler;
import com.iot.devices.management.registry_service.controller.util.CreateUserRequest;
import com.iot.devices.management.registry_service.controller.util.PatchUserRequest;
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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserController.class,
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
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;
    @MockitoBean
    TokenRepository tokenRepository;


    String username = "jonndoe123";
    String firstName = "John";
    String lastName = "Doe";
    String phone = "+12345678";
    String email = "someemail@gmail.com";
    String address = "St. Privet";
    String passwordHash = "jwheknrmlear";

    String json = """
            {
              "username": "%s",
              "firstName": "%s",
              "lastName" : "%s",
              "phone"    : "%s",
              "email"    : "%s",
              "address"  : "%s",
              "password"  : "%s"
            }
            """.formatted(username, firstName, lastName, phone, email, address, passwordHash);


    UUID USER_ID = UUID.randomUUID();
    User USER = new User(USER_ID, username, firstName, lastName, email, phone, address, passwordHash,
            UserRole.USER, now(), now(), now(), new HashSet<>(), new ArrayList<>());

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userService, tokenRepository);
    }

    @Test
    void createUser() throws Exception {
        when(userService.save(any(CreateUserRequest.class), eq(UserRole.USER))).thenReturn(USER);
        mockMvc.perform(post("/api/v1/users/registerUser")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
        verify(userService).findByEmail(email);
        verify(userService).save(any(CreateUserRequest.class), eq(UserRole.USER));
    }

    @Test
    void duplicatedUser() throws Exception {
        when(userService.findByEmail(email)).thenReturn(Optional.of(USER));
        mockMvc.perform(post("/api/v1/users/registerUser")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
        verify(userService).findByEmail(email);
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = {"ADMIN"}, username = "test")
    @Test
    void createAdminForbidden() throws Exception {
        when(userService.save(any(CreateUserRequest.class), eq(UserRole.ADMIN))).thenReturn(USER);
        mockMvc.perform(post("/api/v1/users/registerAdmin")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @WithMockUser(roles = {"SUPER_ADMIN"}, username = "test")
    @Test
    void createAdmin() throws Exception {
        when(userService.save(any(CreateUserRequest.class), eq(UserRole.ADMIN))).thenReturn(USER);
        mockMvc.perform(post("/api/v1/users/registerAdmin")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
        verify(userService).findByEmail(email);
        verify(userService).save(any(CreateUserRequest.class), eq(UserRole.ADMIN));
    }

    @Test
    void dbIsDown() throws Exception {
        when(userService.findByEmail(email)).thenThrow(new DataAccessResourceFailureException("Db is down!"));
        mockMvc.perform(post("/api/v1/users/registerUser")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
        verify(userService).findByEmail(email);
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(username = "jonndoe123")
    @Test
    void patchUser() throws Exception {
        String patchedAddress = "some updated address";
        String patch = """
            {
              "id": "%s",
              "username": "%s",
              "address"  : "%s"
            }
            """.formatted(USER_ID, username, patchedAddress);
        when(userService.findByUsername(username)).thenReturn(Optional.of(USER));
        USER.setAddress(patchedAddress);
        when(userService.patch(any(PatchUserRequest.class), any(User.class))).thenReturn(USER);
        mockMvc.perform(patch("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk());
        verify(userService).findByUsername(username);
        verify(userService).patch(any(PatchUserRequest.class), any(User.class));
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(username = "jonndoe123", roles = "MANAGER")
    @Test
    void patchNoUserFound() throws Exception {
        String patchedAddress = "some updated address";
        String patch = """
            {
              "id": "%s",
              "username": "%s",
              "address"  : "%s"
            }
            """.formatted(USER_ID, username, patchedAddress);
        when(userService.findByUsername(username)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isNotFound());
        verify(userService).findByUsername(username);
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = {"ADMIN", "SUPER_ADMIN"})
    @Test
    void getAllUsers() throws Exception {
        when(userService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(listOf(USER)));
        mockMvc.perform(get("/api/v1/users/all")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findAll(any(Pageable.class));
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = "USER", username = "jonndoe123")
    @Test
    void getAllUsersUnauthorized() throws Exception {
        when(userService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(listOf(USER)));
        mockMvc.perform(get("/api/v1/users/all")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = "USER", username = "jonndoe123")
    @Test
    void getUserByIdUnauthorized() throws Exception {
        when(userService.findByUserId(any())).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/" + USER_ID)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @WithMockUser(roles = "ADMIN", username = "jonndoe123")
    @Test
    void getUserById() throws Exception {
        when(userService.findByUserId(any())).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/" + USER_ID)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findByUserId(any());
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = "SUPER_ADMIN", username = "jonndoe123")
    @Test
    void getUserByIdSuperadmin() throws Exception {
        when(userService.findByUserId(any())).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/" + USER_ID)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findByUserId(any());
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = "ADMIN", username = "jonndoe123")
    @Test
    void getUserByUsername() throws Exception {
        when(userService.findByUsername(any())).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/username/" + username)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findByUsername(any());
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = "USER", username = "jonndoe123")
    @Test
    void getMe() throws Exception {
        when(userService.findByUsername(any())).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/me")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findByUsername(any());
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = "ADMIN", username = "jonndoe123")
    @Test
    void findByEmail() throws Exception {
        when(userService.findByEmail(email)).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/email/" + email)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findByEmail(any());
        verifyNoMoreInteractions(userService);
    }

    @WithMockUser(roles = "USER", username = "jonndoe123")
    @Test
    void deleteUser() throws Exception {
        when(userService.findByUserId(any())).thenReturn(Optional.of(USER));
        when(userService.removeById(any())).thenReturn(1);
        mockMvc.perform(delete("/api/v1/users/" + USER_ID)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(userService).findByUserId(any());
        verify(userService).removeById(any());
        verifyNoMoreInteractions(userService);
    }
}