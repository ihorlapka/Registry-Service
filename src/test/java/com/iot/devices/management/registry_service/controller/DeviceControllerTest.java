package com.iot.devices.management.registry_service.controller;

import com.google.common.collect.ImmutableSet;
import com.iot.devices.management.registry_service.controller.errors.GlobalExceptionHandler;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.PatchDeviceRequest;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.repos.TokenRepository;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import com.iot.devices.management.registry_service.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = {
        DeviceController.class,
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
class DeviceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeviceService deviceService;
    @MockitoBean
    UserService userService;
    @MockitoBean
    TokenRepository tokenRepository;

    String name = "Living Room Temperature Sensor";
    String serialNumber = "SN-8754-XYZ";
    String manufacturer = "BOSCH";
    String model = "BME280";
    String deviceType = "TEMPERATURE_SENSOR";
    String location = "Living Room";
    Double latitude = 50.450100;
    Double longitude = 30.523400;
    String ownerId = "9369f52f-e0d4-4695-a8f2-bd1eb77a221f";
    String status = "ONLINE";
    String lastActiveAt = "2025-06-25T19:42:00+03:00";
    String firmwareVersion = "v2.1.0";

    String json = """
            {
              "name": "%s",
              "serialNumber": "%s",
              "deviceManufacturer": "%s",
              "model": "%s",
              "deviceType": "%s",
              "location": "%s",
              "latitude": "%s",
              "longitude": "%s",
              "ownerId": "%s",
              "status": "%s",
              "lastActiveAt": "%s",
              "firmwareVersion": "%s"
            }
            """;

    String filledJson = json.formatted(name, serialNumber, manufacturer,
            model, deviceType, location, latitude, longitude, ownerId,
            status, lastActiveAt, firmwareVersion);

    Device DEVICE = new Device(UUID.randomUUID(), name, serialNumber,
            DeviceManufacturer.valueOf(manufacturer), model, DeviceType.valueOf(deviceType),
            location, new BigDecimal(latitude), new BigDecimal(longitude), null,
            DeviceStatus.valueOf(status), now(), firmwareVersion, now(), now(), "{}");

    User USER = new User(UUID.fromString(ownerId),
            "some_username", "firstName", "lastName",
            "some_email@gmail.com", "+3801234456", null,
            "6576887654", UserRole.USER, now(), now(), now(), ImmutableSet.of(DEVICE), ImmutableList.of());


    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userService, deviceService, tokenRepository);
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void createDeviceWithUser() throws Exception {
        when(deviceService.findBySerialNumber(any())).thenReturn(Optional.empty());
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.of(USER));
        when(deviceService.saveAndSendMessage(any(CreateDeviceRequest.class), any(User.class))).thenReturn(DEVICE);
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isCreated());
        verify(deviceService).findBySerialNumber(serialNumber);
        verify(deviceService).saveAndSendMessage(any(CreateDeviceRequest.class), any(User.class));
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void createDeviceWithoutUser() throws Exception {
        when(deviceService.findBySerialNumber(any())).thenReturn(Optional.empty());
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.empty());
        when(deviceService.saveAndSendMessage(any(CreateDeviceRequest.class), isNull())).thenReturn(DEVICE);
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isForbidden());
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "ADMIN")
    @Test
    void createDeviceWithoutUserAdmin() throws Exception {
        when(deviceService.findBySerialNumber(any())).thenReturn(Optional.empty());
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.empty());
        when(deviceService.saveAndSendMessage(any(CreateDeviceRequest.class), isNull())).thenReturn(DEVICE);
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isCreated());
        verify(deviceService).findBySerialNumber(serialNumber);
        verify(deviceService).saveAndSendMessage(any(CreateDeviceRequest.class), isNull());
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "ADMIN")
    @Test
    void createDuplicateDeviceWithoutUserAdmin() throws Exception {
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.empty());
        when(deviceService.findBySerialNumber(any())).thenReturn(Optional.of(DEVICE));
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isConflict());
        verify(deviceService).findBySerialNumber(serialNumber);
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void createDuplicateDevice() throws Exception {
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.of(USER));
        when(deviceService.findBySerialNumber(any())).thenReturn(Optional.of(DEVICE));
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isConflict());
        verify(deviceService).findBySerialNumber(serialNumber);
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void createDeviceDbIsDown() throws Exception {
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.of(USER));
        when(deviceService.findBySerialNumber(any())).thenThrow(new DataAccessResourceFailureException("Db is down!"));
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isInternalServerError());
        verify(deviceService).findBySerialNumber(serialNumber);
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "ADMIN")
    @Test
    void createDeviceWithoutUserInRequest() throws Exception {
        String filledJson = """
                {
                  "name": "%s",
                  "serialNumber": "%s",
                  "deviceManufacturer": "%s",
                  "model": "%s",
                  "deviceType": "%s",
                  "status": "%s",
                  "lastActiveAt": "%s",
                  "firmwareVersion": "%s"
                }
                """.formatted(name, serialNumber, manufacturer, model, deviceType,
                status, lastActiveAt, firmwareVersion);
        Device device = DEVICE;
        device.setOwner(null);
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.empty());
        when(deviceService.saveAndSendMessage(any(CreateDeviceRequest.class), isNull())).thenReturn(device);
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isCreated());
        verify(deviceService).findBySerialNumber(serialNumber);
        verify(deviceService).saveAndSendMessage(any(CreateDeviceRequest.class), isNull());
        verifyNoInteractions(userService);
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void patchDevice() throws Exception {
        String firmwareVersion = "1.58.5v";
        String filledJson = """
                {
                  "id": "%s",
                  "firmwareVersion": "%s",
                  "ownerId": "%s"
                }
                """.formatted(DEVICE.getId(), firmwareVersion, ownerId);
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.of(USER));
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        Device patchedDevice = DEVICE;
        patchedDevice.setFirmwareVersion(firmwareVersion);
        when(deviceService.patch(any(PatchDeviceRequest.class), any(User.class))).thenReturn(patchedDevice);
        mockMvc.perform(patch("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isOk());
        verify(deviceService).patch(any(PatchDeviceRequest.class), any(User.class));
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "ADMIN")
    @Test
    void patchDeviceAdmin() throws Exception {
        String firmwareVersion = "1.58.5v";
        String filledJson = """
                {
                  "id": "%s",
                  "firmwareVersion": "%s",
                  "ownerId": "%s"
                }
                """.formatted(DEVICE.getId(), firmwareVersion, ownerId);
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.of(USER));
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        Device patchedDevice = DEVICE;
        patchedDevice.setFirmwareVersion(firmwareVersion);
        when(deviceService.patch(any(PatchDeviceRequest.class), any(User.class))).thenReturn(patchedDevice);
        mockMvc.perform(patch("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isOk());
        verify(deviceService).patch(any(PatchDeviceRequest.class), any(User.class));
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "ADMIN")
    @Test
    void patchDeviceWithoutUser() throws Exception {
        String firmwareVersion = "1.58.5v";
        String filledJson = """
                {
                  "id": "%s",
                  "firmwareVersion": "%s",
                  "ownerId": "%s"
                }
                """.formatted(DEVICE.getId(), firmwareVersion, ownerId);
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.empty());
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        Device patchedDevice = DEVICE;
        patchedDevice.setFirmwareVersion(firmwareVersion);
        when(deviceService.patch(any(PatchDeviceRequest.class), isNull())).thenReturn(patchedDevice);
        mockMvc.perform(patch("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isOk());
        verify(deviceService).patch(any(PatchDeviceRequest.class), isNull());
        verify(userService).findByUserId(USER.getId());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void getDevice() throws Exception {
        DEVICE.setOwner(USER);
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        mockMvc.perform(get("/api/v1/devices/" + DEVICE.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(deviceService).findByDeviceId(any());
    }

    @WithMockUser(username = "some_username", roles = "ADMIN")
    @Test
    void getDeviceAdmin() throws Exception {
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        mockMvc.perform(get("/api/v1/devices/" + DEVICE.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(deviceService).findByDeviceId(any());
    }

    @WithMockUser(username = "some_username", roles = "USER")
    @Test
    void deleteDevice() throws Exception {
        DEVICE.setOwner(USER);
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        when(deviceService.removeById(any(), eq(USER))).thenReturn(1);
        mockMvc.perform(delete("/api/v1/devices/" + DEVICE.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(deviceService).removeById(any(), eq(USER));
        verify(deviceService).findByDeviceId(any());
    }

    @WithMockUser(username = "some_username", roles = "ADMIN")
    @Test
    void deleteDeviceAdmin() throws Exception {
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        when(deviceService.removeById(any(), isNull())).thenReturn(1);
        mockMvc.perform(delete("/api/v1/devices/" + DEVICE.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(deviceService).removeById(any(), isNull());
        verify(deviceService).findByDeviceId(any());
    }
}