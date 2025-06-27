package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.errors.ErrorHandler;
import com.iot.devices.management.registry_service.controller.util.PatchUserRequest;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
        ErrorHandler.class
})
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;


    String username = "jonndoe123";
    String firstName = "John";
    String lastName = "Doe";
    String phone = "+12345678";
    String email = "someemail@gmail.com";
    String address = "St. Privet";
    String passwordHash = "jwheknrmlear";
    String userRole = "USER";

    String json = """
            {
              "username": "%s",
              "firstName": "%s",
              "lastName" : "%s",
              "phone"    : "%s",
              "email"    : "%s",
              "address"  : "%s",
              "passwordHash"  : "%s",
              "userRole"  : "%s"
            }
            """.formatted(username, firstName, lastName, phone, email, address, passwordHash, userRole);


    UUID USER_ID = UUID.randomUUID();
    User USER = new User(USER_ID, username, firstName, lastName, email, phone, address, passwordHash,
            UserRole.USER, now(), now(), now(), new HashSet<>());

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userService);
    }

    @Test
    void createUser() throws Exception {
        when(userService.save(any())).thenReturn(USER);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
        verify(userService).findByEmail(email);
        verify(userService).save(any());
    }

    @Test
    void duplicatedUser() throws Exception {
        when(userService.findByEmail(email)).thenReturn(Optional.of(USER));
        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
        verify(userService).findByEmail(email);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void dbIsDown() throws Exception {
        when(userService.findByEmail(email)).thenThrow(new DataAccessResourceFailureException("Db is down!"));
        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
        verify(userService).findByEmail(email);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void patchUser() throws Exception {
        String patchedAddress = "some updated address";
        String patch = """
            {
              "id": "%s",
              "address"  : "%s"
            }
            """.formatted(USER_ID, patchedAddress);
        when(userService.findByUserId(USER_ID)).thenReturn(Optional.of(USER));
        USER.setAddress(patchedAddress);
        when(userService.patch(any(PatchUserRequest.class), any(User.class))).thenReturn(USER);
        mockMvc.perform(patch("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk());
        verify(userService).findByUserId(USER_ID);
        verify(userService).patch(any(PatchUserRequest.class), any(User.class));
        verifyNoMoreInteractions(userService);
    }

    @Test
    void patchNoUserFound() throws Exception {
        String patchedAddress = "some updated address";
        String patch = """
            {
              "id": "%s",
              "address"  : "%s"
            }
            """.formatted(USER_ID, patchedAddress);
        when(userService.findByUserId(USER_ID)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isNotFound());
        verify(userService).findByUserId(USER_ID);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getAllUsers() throws Exception {
        when(userService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(listOf(USER)));
        mockMvc.perform(get("/api/v1/users")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findAll(any(Pageable.class));
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getUserById() throws Exception {
        when(userService.findByUserId(any())).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/" + USER_ID)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findByUserId(any());
        verifyNoMoreInteractions(userService);
    }

    @Test
    void findByEmail() throws Exception {
        when(userService.findByEmail(email)).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/email/" + email)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(userService).findByEmail(any());
        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser() throws Exception {
        when(userService.removeById(any())).thenReturn(1);
        mockMvc.perform(delete("/api/v1/users/" + USER_ID)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(userService).removeById(any());
        verifyNoMoreInteractions(userService);
    }
}