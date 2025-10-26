package com.iot.devices.management.registry_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.iot.devices.management.registry_service.controller.errors.GlobalExceptionHandler;
import com.iot.devices.management.registry_service.controller.util.CreateAlertRuleRequest;
import com.iot.devices.management.registry_service.controller.util.PatchAlertRuleRequest;
import com.iot.devices.management.registry_service.persistence.model.AlertRule;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.repos.TokenRepository;
import com.iot.devices.management.registry_service.persistence.services.AlertRuleService;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import com.iot.devices.management.registry_service.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType.HUMIDITY;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType.PERCENTAGE;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel.CRITICAL;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel.WARNING;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType.GREATER_THAN;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType.LESS_THAN;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.iot.devices.management.registry_service.controller.errors.AlertRulesException.*;

@WebMvcTest(controllers = {
        AlertRuleController.class,
        GlobalExceptionHandler.class
})
@ExtendWith(SpringExtension.class)
@Import({
        AppConfig.class,
        JwtService.class,
        SecurityConfig.class,
        JwtAuthentificationFilter.class,
        SecurityProperties.class,
        LogoutService.class
})
class AlertRuleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AlertRuleService alertRuleService;
    @MockitoBean
    UserService userService;
    @MockitoBean
    TokenRepository tokenRepository;

    User USER = new User(randomUUID(),
            "some_username", "firstName", "lastName",
            "some_email@gmail.com", "+3801234456", null,
            "6576887654", UserRole.USER, now(), now(), now(), ImmutableSet.of(), ImmutableList.of());

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userService, alertRuleService, tokenRepository);
    }


    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void getMyAlertRulesSuccess() throws Exception {
        AlertRule alertRule = new AlertRule(randomUUID(), HUMIDITY, GREATER_THAN,
                10f, WARNING, true, USER.getUsername());
        when(alertRuleService.findRulesByUsername(USER.getUsername())).thenReturn(List.of(alertRule));
        mockMvc.perform(get("/api/v1/alertRules/myRules")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(alertRuleService).findRulesByUsername(USER.getUsername());
    }

    @WithMockUser(username = "some_manager", roles = "MANAGER")
    @Test
    void getOtherUserAlertRules() throws Exception {
        AlertRule alertRule = new AlertRule(randomUUID(), HUMIDITY, GREATER_THAN,
                10f, WARNING, true, USER.getUsername());
        when(alertRuleService.findRulesByUsername(USER.getUsername())).thenReturn(List.of(alertRule));

        mockMvc.perform(get("/api/v1/alertRules/userRules/" + USER.getUsername())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(alertRuleService).findRulesByUsername(USER.getUsername());
    }

    @WithMockUser(username = "some_admin", roles = "ADMIN")
    @Test
    void createAlertRuleToOtherUser() throws Exception {
        UUID deviceId = randomUUID();
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(Set.of(deviceId), PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        AlertRule alertRule = new AlertRule(randomUUID(), PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        when(userService.findByUsername(USER.getUsername())).thenReturn(Optional.of(USER));
        when(alertRuleService.saveAndSendMessage(request, USER)).thenReturn(alertRule);

        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(post("/api/v1/alertRules")
                        .contentType(APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isCreated());

        verify(alertRuleService).saveAndSendMessage(request, USER);
        verify(userService).findByUsername(USER.getUsername());
    }

    @WithMockUser(username = "some_admin", roles = "ADMIN")
    @Test
    void createAlertRuleToOtherUserFailed() throws Exception {
        UUID deviceId = randomUUID();
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(Set.of(deviceId), PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        AlertRule alertRule = new AlertRule(randomUUID(), PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        when(userService.findByUsername(USER.getUsername())).thenReturn(Optional.of(USER));
        when(alertRuleService.saveAndSendMessage(request, USER)).thenThrow(new AlertRuleNotSentException(alertRule.getRuleId()));

        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(post("/api/v1/alertRules")
                        .contentType(APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isNotFound());

        verify(alertRuleService).saveAndSendMessage(request, USER);
        verify(userService).findByUsername(USER.getUsername());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void createAlertRuleToMyUser() throws Exception {
        UUID deviceId = randomUUID();
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(Set.of(deviceId), PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        AlertRule alertRule = new AlertRule(randomUUID(), PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        when(userService.findByUsername(USER.getUsername())).thenReturn(Optional.of(USER));
        when(alertRuleService.saveAndSendMessage(request, USER)).thenReturn(alertRule);

        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(post("/api/v1/alertRules")
                        .contentType(APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isCreated());

        verify(alertRuleService).saveAndSendMessage(request, USER);
        verify(userService).findByUsername(USER.getUsername());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void patchAlertRuleToMyUser() throws Exception {
        UUID ruleId = randomUUID();
        UUID deviceId = randomUUID();
        PatchAlertRuleRequest request = new PatchAlertRuleRequest(ruleId, Set.of(deviceId), null, PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        AlertRule alertRule = new AlertRule(ruleId, PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        when(userService.findByUsername(USER.getUsername())).thenReturn(Optional.of(USER));
        when(alertRuleService.patchAndSendMessage(request, USER)).thenReturn(alertRule);

        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/alertRules")
                        .contentType(APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isCreated());

        verify(alertRuleService).patchAndSendMessage(request, USER);
        verify(userService).findByUsername(USER.getUsername());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void patchAlertRuleToMyUserFailed() throws Exception {
        UUID ruleId = randomUUID();
        UUID deviceId = randomUUID();
        PatchAlertRuleRequest request = new PatchAlertRuleRequest(ruleId, Set.of(deviceId), null, PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        AlertRule alertRule = new AlertRule(ruleId, PERCENTAGE, LESS_THAN,
                50f, CRITICAL, true, USER.getUsername());

        when(userService.findByUsername(USER.getUsername())).thenReturn(Optional.of(USER));
        when(alertRuleService.patchAndSendMessage(request, USER)).thenThrow(new AlertRuleNotFoundException(ruleId));

        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(patch("/api/v1/alertRules")
                        .contentType(APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isNotFound());

        verify(alertRuleService).patchAndSendMessage(request, USER);
        verify(userService).findByUsername(USER.getUsername());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void deleteAlertRuleToMyUser() throws Exception {
        UUID ruleId = randomUUID();

        when(userService.findByUsername(USER.getUsername())).thenReturn(Optional.of(USER));

        mockMvc.perform(delete("/api/v1/alertRules/" + ruleId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(alertRuleService).removeAndSendTombstone(ruleId);
        verify(userService).findByUsername(USER.getUsername());
    }

}