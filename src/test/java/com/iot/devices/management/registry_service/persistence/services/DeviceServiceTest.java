package com.iot.devices.management.registry_service.persistence.services;

import com.iot.devices.TempUnit;
import com.iot.devices.TemperatureSensor;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.repos.DevicesRepository;
import com.iot.devices.management.registry_service.persistence.repos.UsersRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.*;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.mapTemperatureSensor;
import static com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus.MAINTENANCE;
import static com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus.ONLINE;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {
        UserService.class,
        DeviceService.class,
        UsersRepository.class,
        DevicesRepository.class,
        DeviceServiceTest.TestPersistenceConfig.class
})
@TestPropertySource("classpath:application-test.yaml")
@Testcontainers
@Sql(scripts = "/schema.sql")
class DeviceServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    DeviceService deviceService;
    @Autowired
    EntityManager entityManager;

    ObjectMapper objectMapper = new ObjectMapper();

    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.5"));

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @BeforeAll
    static void start() {
        postgreSQLContainer.start();
    }

    @AfterAll
    static void close() {
        postgreSQLContainer.close();
    }

    @BeforeEach
    void setUp() {
        String username = "jonndoe123";
        String firstName = "John";
        String lastName = "Doe";
        String phone = "+12345678";
        String email = "someemail@gmail.com";
        String address = "St. Privet";
        String passwordHash = "jwheknrmlear";

        String name = "Living Room Temperature Sensor";
        String serialNumber = "SN-8754-XYZ";
        String manufacturer = "BOSCH";
        String model = "BME280";
        String deviceType = "TEMPERATURE_SENSOR";
        String location = "Living Room";
        double latitude = 50.450100;
        double longitude = 30.523400;
        String status = "OFFLINE";
        String firmwareVersion = "v2.1.0";

        Device DEVICE = new Device(null, name, serialNumber,
                DeviceManufacturer.valueOf(manufacturer), model, DeviceType.valueOf(deviceType),
                location, new BigDecimal(latitude), new BigDecimal(longitude), null,
                DeviceStatus.valueOf(status), now(), firmwareVersion, now(), now(), "{}");

        User USER = new User(null, username, firstName, lastName, email, phone, address, passwordHash,
                UserRole.USER, now(), now(), now(), ImmutableSet.of(DEVICE));
        DEVICE.setOwner(USER);
        userService.save(USER);
    }

    @Test
    void test() throws IOException {
        Page<User> userPage = userService.findAll(PageRequest.of(0, 1));
        Optional<User> userOptional = userPage.stream().findFirst();
        assertTrue(userOptional.isPresent());
        User user = userOptional.get();
        Optional<Device> optionalDevice = user.getDevices().stream().findFirst();
        assertTrue(optionalDevice.isPresent());
        Device device = optionalDevice.get();

        final Instant now = now().toInstant();
        float temperature = 23.3f;
        String firmwareVersion = "v2.1.1";
        TemperatureSensor temperatureSensor1 = TemperatureSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setFirmwareVersion(firmwareVersion)
                .setLastUpdated(now)
                .setTemperature(temperature)
                .setUnit(TempUnit.C)
                .build();

        int affectedRows1 = deviceService.patchTemperatureSensorTelemetry(mapTemperatureSensor(temperatureSensor1));

        entityManager.clear(); //clean up persistence context

        assertEquals(1, affectedRows1);

        Optional<Device> updatedDeviceOptional = deviceService.findByDeviceId(device.getId());
        assertTrue(updatedDeviceOptional.isPresent());
        Device updatedDevice = updatedDeviceOptional.get();

        assertEquals(ONLINE, updatedDevice.getStatus());
        assertEquals(now.truncatedTo(MILLIS), updatedDevice.getUpdatedAt().toInstant());
        assertEquals(firmwareVersion, updatedDevice.getFirmwareVersion());

        Map<String, Object> telemetry = objectMapper.readValue(updatedDevice.getTelemetry(), new TypeReference<>() {});
        assertEquals(temperature, (double) telemetry.get("temperature"), 0.001);
        assertEquals(TempUnit.C.name(), telemetry.get("unit"));

        float humidity = 12.123f;
        TemperatureSensor temperatureSensor2 = TemperatureSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.MAINTENANCE)
                .setUnit(TempUnit.C)
                .setLastUpdated(now)
                .setHumidity(humidity)
                .build();

        int affectedRows2 = deviceService.patchTemperatureSensorTelemetry(mapTemperatureSensor(temperatureSensor2));

        entityManager.clear();

        assertEquals(1, affectedRows2);
        Optional<Device> updatedDeviceOptional2 = deviceService.findByDeviceId(device.getId());
        assertTrue(updatedDeviceOptional2.isPresent());
        Device updatedDevice2 = updatedDeviceOptional2.get();

        assertEquals(MAINTENANCE, updatedDevice2.getStatus());
        assertEquals(now.truncatedTo(MILLIS), updatedDevice2.getUpdatedAt().toInstant());
        assertEquals(firmwareVersion, updatedDevice2.getFirmwareVersion());

        Map<String, Object> telemetry2 = objectMapper.readValue(updatedDevice2.getTelemetry(), new TypeReference<>() {});
        assertEquals(temperature, (double) telemetry2.get("temperature"), 0.001);
        assertEquals(humidity, (double) telemetry2.get("humidity"), 0.001);
        assertEquals(TempUnit.C.name(), telemetry2.get("unit"));
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.iot.devices.management.registry_service.persistence.repos")
    @EntityScan(basePackages = "com.iot.devices.management.registry_service.persistence.model")
    static class TestPersistenceConfig {}

}