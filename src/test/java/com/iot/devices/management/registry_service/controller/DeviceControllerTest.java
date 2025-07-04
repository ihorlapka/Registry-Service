package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.errors.ErrorHandler;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.PatchDeviceRequest;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashSet;
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

@WebMvcTest(controllers = {
        DeviceController.class,
        ErrorHandler.class
})
class DeviceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeviceService deviceService;
    @MockitoBean
    UserService userService;

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

    User USER = new User(UUID.fromString("9369f52f-e0d4-4695-a8f2-bd1eb77a221f"),
            "username", "firstName", "lastName",
            "some_email@gmail.com", "+3801234456", null,
            "6576887654", UserRole.USER, now(), now(), now(), new HashSet<>());

    Device DEVICE = new Device(UUID.randomUUID(), name, serialNumber,
            DeviceManufacturer.valueOf(manufacturer), model, DeviceType.valueOf(deviceType),
            location, new BigDecimal(latitude), new BigDecimal(longitude), USER,
            DeviceStatus.valueOf(status), now(), firmwareVersion, now(), now(), "{}");

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userService, deviceService);
    }

    @Test
    void createDeviceWithUser() throws Exception {
        when(deviceService.findBySerialNumber(any())).thenReturn(Optional.empty());
        when(userService.findByUserId(UUID.fromString(ownerId))).thenReturn(Optional.of(USER));
        when(deviceService.save(any(CreateDeviceRequest.class), eq(USER))).thenReturn(DEVICE);
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isCreated());
        verify(deviceService).findBySerialNumber(serialNumber);
        verify(userService).findByUserId(UUID.fromString(ownerId));
        verify(deviceService).save(any(CreateDeviceRequest.class), eq(USER));
    }

    @Test
    void createDuplicateDevice() throws Exception {
        when(deviceService.findBySerialNumber(any())).thenReturn(Optional.of(DEVICE));
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isConflict());
        verify(deviceService).findBySerialNumber(serialNumber);
    }

    @Test
    void createDeviceDbIsDown() throws Exception {
        when(deviceService.findBySerialNumber(any())).thenThrow(new DataAccessResourceFailureException("Db is down!"));
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isInternalServerError());
        verify(deviceService).findBySerialNumber(serialNumber);
    }

    @Test
    void createDeviceWithoutUser() throws Exception {
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
        when(deviceService.save(any(CreateDeviceRequest.class), isNull())).thenReturn(device);
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isCreated());
        verify(deviceService).findBySerialNumber(serialNumber);
        verifyNoInteractions(userService);
        verify(deviceService).save(any(CreateDeviceRequest.class), isNull());
    }

    @Test
    void patchDevice() throws Exception {
        String firmwareVersion = "1.58.5v";
        String filledJson = """
                {
                  "id": "%s",
                  "firmwareVersion": "%s"
                }
                """.formatted(DEVICE.getId(), firmwareVersion);
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        Device patchedDevice = DEVICE;
        patchedDevice.setFirmwareVersion(firmwareVersion);
        when(deviceService.patch(any(PatchDeviceRequest.class), eq(DEVICE), isNull())).thenReturn(patchedDevice);
        mockMvc.perform(patch("/api/v1/devices")
                        .contentType(APPLICATION_JSON)
                        .content(filledJson))
                .andExpect(status().isOk());
        verify(deviceService).findByDeviceId(DEVICE.getId());
        verify(deviceService).patch(any(PatchDeviceRequest.class), eq(patchedDevice), isNull());
    }

    @Test
    void getDevice() throws Exception {
        when(deviceService.findByDeviceId(any())).thenReturn(Optional.of(DEVICE));
        mockMvc.perform(get("/api/v1/devices/" + DEVICE.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(deviceService).findByDeviceId(any());
    }

    @Test
    void deleteDevice() throws Exception {
        when(deviceService.removeById(any())).thenReturn(1);
        mockMvc.perform(delete("/api/v1/devices/" + DEVICE.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(deviceService).removeById(any());
    }
}