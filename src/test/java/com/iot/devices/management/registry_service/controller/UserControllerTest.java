package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.repos.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {


    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UsersRepository usersRepository;

    @BeforeEach
    void setUp() {
    }

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

    @Test
    void createUser() throws Exception {
        when(usersRepository.save(any())).thenReturn(new User(1L, firstName, lastName, email, phone, address));
        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
        verify(usersRepository).findByEmail(email);
        verify(usersRepository).save(any());
    }

    @Test
    void patchUser() {
    }

    @Test
    void getAllUsers() {
    }

    @Test
    void getUserById() {
    }

    @Test
    void findByEmail() {
    }

    @Test
    void deleteUser() {
    }
}