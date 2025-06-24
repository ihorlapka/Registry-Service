package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.repos.UsersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

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
    UsersRepository usersRepository;


    String firstName = "John";
    String lastName = "Doe";
    String phone = "+12345678";
    String email = "someemail@gmail.com";
    String address = "St. Privet";

    String json = """
            {
              "firstName": "%s",
              "lastName" : "%s",
              "phone"    : "%s",
              "email"    : "%s",
              "address"  : "%s"
            }
            """.formatted(firstName, lastName, phone, email, address);


    User USER = new User(1L, firstName, lastName, email, phone, address);

    @Test
    void createUser() throws Exception {
        when(usersRepository.save(any())).thenReturn(USER);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
        verify(usersRepository).findByEmail(email);
        verify(usersRepository).save(any());
    }

    @Test
    void duplicatedUser() throws Exception {
        when(usersRepository.findByEmail(email)).thenReturn(Optional.of(USER));
        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
        verify(usersRepository).findByEmail(email);
        verifyNoMoreInteractions(usersRepository);
    }

    @Test
    void dbIsDown() throws Exception {
        when(usersRepository.findByEmail(email)).thenThrow(new DataAccessResourceFailureException("Db is down!"));
        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
        verify(usersRepository).findByEmail(email);
        verifyNoMoreInteractions(usersRepository);
    }

    @Test
    void patchUser() throws Exception {
        String patchedAddress = "some updated address";
        String patch = """
            {
              "id": "%s",
              "address"  : "%s"
            }
            """.formatted(1, patchedAddress);
        when(usersRepository.findById(1L)).thenReturn(Optional.of(USER));
        USER.setAddress(patchedAddress);
        when(usersRepository.save(any())).thenReturn(USER);
        mockMvc.perform(patch("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk());
        verify(usersRepository).findById(1L);
        verify(usersRepository).save(any(User.class));
        verifyNoMoreInteractions(usersRepository);
    }

    @Test
    void patchNoUserFound() throws Exception {
        String patchedAddress = "some updated address";
        String patch = """
            {
              "id": "%s",
              "address"  : "%s"
            }
            """.formatted(1, patchedAddress);
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isNotFound());
        verify(usersRepository).findById(1L);
        verifyNoMoreInteractions(usersRepository);
    }

    @Test
    void getAllUsers() throws Exception {
        when(usersRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(listOf(USER)));
        mockMvc.perform(get("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
        verify(usersRepository).findAll(any(Pageable.class));
        verifyNoMoreInteractions(usersRepository);
    }

    @Test
    void getUserById() throws Exception {
        when(usersRepository.findById(anyLong())).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/1")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
        verify(usersRepository).findById(any());
        verifyNoMoreInteractions(usersRepository);
    }

    @Test
    void findByEmail() throws Exception {
        when(usersRepository.findByEmail(email)).thenReturn(Optional.of(USER));
        mockMvc.perform(get("/api/v1/users/email/" + email)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
        verify(usersRepository).findByEmail(any());
        verifyNoMoreInteractions(usersRepository);
    }

    @Test
    void deleteUser() throws Exception {
        when(usersRepository.removeById(anyLong())).thenReturn(1);
        mockMvc.perform(delete("/api/v1/users/1")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());
        verify(usersRepository).removeById(anyLong());
        verifyNoMoreInteractions(usersRepository);
    }
}