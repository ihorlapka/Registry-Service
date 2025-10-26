package com.iot.devices.management.registry_service.persistence.services;

import com.iot.devices.*;
import com.iot.devices.management.registry_service.RegistryServiceApplication;
import com.iot.devices.management.registry_service.alerts.StandardAlertRulesProvider;
import com.iot.devices.management.registry_service.kafka.AlertingRulesKafkaProducer;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.repos.AlertRulesRepository;
import com.iot.devices.management.registry_service.persistence.repos.DeviceAlertRuleRepository;
import com.iot.devices.management.registry_service.persistence.repos.DevicesRepository;
import com.iot.devices.management.registry_service.persistence.repos.UsersRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static com.iot.devices.management.registry_service.mapping.DeviceParametersMapper.*;
import static com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus.*;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = {
        RegistryServiceApplication.class,
        DeviceService.class,
        UsersRepository.class,
        DevicesRepository.class,
        DeviceServiceTelemetriesUpdatesTest.TestPersistenceConfig.class,
        AlertRulesRepository.class,
        DeviceAlertRuleRepository.class,
        StandardAlertRulesProvider.class
})
@TestPropertySource("classpath:application-test.yaml")
@Testcontainers
class DeviceServiceTelemetriesUpdatesTest {

    @Autowired
    UsersRepository usersRepository;
    @Autowired
    DeviceService deviceService;
    @Autowired
    EntityManager entityManager;
    @MockitoBean
    PasswordEncoder passwordEncoder;
    @MockitoBean
    AlertingRulesKafkaProducer alertingRulesKafkaProducer;

    ObjectMapper objectMapper = new ObjectMapper();

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.5"))
            .withInitScript("schema.sql");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
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

        Optional<User> user = usersRepository.findByUsername(username);
        if (user.isEmpty()) {
            Device DEVICE = new Device(null, name, serialNumber,
                    DeviceManufacturer.valueOf(manufacturer), model, DeviceType.valueOf(deviceType),
                    location, new BigDecimal(latitude), new BigDecimal(longitude), null,
                    DeviceStatus.valueOf(status), now(), firmwareVersion, now(), now(), "{}");

            User USER = new User(null, username, firstName, lastName, email, phone, address, passwordHash,
                    UserRole.USER, now(), now(), now(), ImmutableSet.of(DEVICE), ImmutableList.of());
            DEVICE.setOwner(USER);
            usersRepository.save(USER);
        }
    }

    @Test
    void doorSensorUpdate() throws IOException {
        Device device = getDeviceFromDb();
        final Instant now = now().toInstant();
        String firmwareVersion = "v2.1.1";

        DoorState state1 = DoorState.OPEN;
        Instant lastOpened = now.minus(5, SECONDS);
        DoorSensor doorSensor1 = DoorSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setFirmwareVersion(firmwareVersion)
                .setLastUpdated(now)
                .setLastOpened(lastOpened)
                .setDoorState(state1)
                .build();

        int affectedRows1 = deviceService.patchDoorSensorTelemetry(mapDoorSensor(doorSensor1));
        Device updatedDevice = verifyAndGetUpdatedDevice(affectedRows1, device, now, firmwareVersion, ONLINE);

        Map<String, Object> telemetry = readTelemetry(updatedDevice);
        assertEquals(state1.name(), telemetry.get("doorState"));
        assertEquals(lastOpened.atOffset(ZoneOffset.UTC).truncatedTo(MILLIS),
                LocalDateTime.parse((String) telemetry.get("lastOpened")).atOffset(ZoneOffset.UTC).truncatedTo(MILLIS));

        DoorState state2 = DoorState.CLOSED;
        final Instant now2 = now().toInstant();
        int batteryLevel = 76;
        DoorSensor doorSensor2 = DoorSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setLastUpdated(now2)
                .setDoorState(state2)
                .setTamperAlert(true)
                .setBatteryLevel(batteryLevel)
                .build();

        int affectedRows2 = deviceService.patchDoorSensorTelemetry(mapDoorSensor(doorSensor2));
        Device updatedDevice2 = verifyAndGetUpdatedDevice(affectedRows2, device, now2, firmwareVersion, ONLINE);

        Map<String, Object> telemetry2 = readTelemetry(updatedDevice2);
        assertEquals(state2.name(), telemetry2.get("doorState"));
        assertEquals(lastOpened.atOffset(ZoneOffset.UTC).truncatedTo(MILLIS),
                LocalDateTime.parse((String) telemetry2.get("lastOpened")).atOffset(ZoneOffset.UTC).truncatedTo(MILLIS));
        assertTrue((Boolean) telemetry2.get("tamperAlert"));
        assertEquals(batteryLevel, telemetry2.get("batteryLevel"));
    }

    @Test
    void energyMeterUpdate() throws IOException {
        Device device = getDeviceFromDb();
        final Instant now = now().toInstant();
        String firmwareVersion = "v2.1.1";
        float power = 230.53f;
        EnergyMeter energyMeter1 = EnergyMeter.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.MAINTENANCE)
                .setFirmwareVersion(firmwareVersion)
                .setLastUpdated(now)
                .setPower(power)
                .build();

        int affectedRows1 = deviceService.patchEnergyMeterTelemetry(mapEnergyMeter(energyMeter1));
        Device updatedDevice = verifyAndGetUpdatedDevice(affectedRows1, device, now, firmwareVersion, MAINTENANCE);

        Map<String, Object> telemetry = readTelemetry(updatedDevice);
        assertEquals(power, (double) telemetry.get("power"), 0.001);

        final Instant now2 = now().toInstant();
        float voltage = 220.2f;
        float current = 214.7f;
        float energyConsumed = 10245.543f;
        EnergyMeter energyMeter2 = EnergyMeter.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setLastUpdated(now2)
                .setVoltage(voltage)
                .setCurrent(current)
                .setEnergyConsumed(energyConsumed)
                .build();

        int affectedRows2 = deviceService.patchEnergyMeterTelemetry(mapEnergyMeter(energyMeter2));
        Device updatedDevice2 = verifyAndGetUpdatedDevice(affectedRows2, device, now2, firmwareVersion, ONLINE);

        Map<String, Object> telemetry2 = readTelemetry(updatedDevice2);
        assertEquals(power, (double) telemetry2.get("power"), 0.001);
        assertEquals(voltage, (double) telemetry2.get("voltage"), 0.001);
        assertEquals(current, (double) telemetry2.get("current"), 0.001);
        assertEquals(energyConsumed, (double) telemetry2.get("energyConsumed"), 0.001);
    }

    @Test
    void smartLightUpdate() throws IOException {
        Device device = getDeviceFromDb();
        final Instant now = now().toInstant();
        String firmwareVersion = "v2.1.1";
        float powerConsumption = 0.01f;
        SmartLight smartLight1 = SmartLight.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.OFFLINE)
                .setFirmwareVersion(firmwareVersion)
                .setLastUpdated(now)
                .setIsOn(false)
                .setPowerConsumption(powerConsumption)
                .build();

        int affectedRows1 = deviceService.patchSmartLightTelemetry(mapSmartLight(smartLight1));
        Device updatedDevice = verifyAndGetUpdatedDevice(affectedRows1, device, now, firmwareVersion, OFFLINE);

        Map<String, Object> telemetry = readTelemetry(updatedDevice);
        assertEquals(powerConsumption, (double) telemetry.get("powerConsumption"), 0.001);
        assertFalse((Boolean) telemetry.get("isOn"));

        final Instant now2 = now().toInstant();
        int brightness = 4;
        String color = "green";
        SmartLightMode mode = SmartLightMode.AMBIENT;
        SmartLight smartLight2 = SmartLight.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setLastUpdated(now2)
                .setIsOn(true)
                .setBrightness(brightness)
                .setColor(color)
                .setMode(mode)
                .build();

        int affectedRows2 = deviceService.patchSmartLightTelemetry(mapSmartLight(smartLight2));
        Device updatedDevice2 = verifyAndGetUpdatedDevice(affectedRows2, device, now2, firmwareVersion, ONLINE);

        Map<String, Object> telemetry2 = readTelemetry(updatedDevice2);
        assertEquals(powerConsumption, (double) telemetry2.get("powerConsumption"), 0.001);
        assertTrue((Boolean) telemetry2.get("isOn"));
        assertEquals(brightness, telemetry2.get("brightness"));
        assertEquals(color, telemetry2.get("colour"));
        assertEquals(mode.name(), telemetry2.get("mode"));
    }

    @Test
    void smartPlugUpdate() throws IOException {
        Device device = getDeviceFromDb();
        final Instant now = now().toInstant();
        String firmwareVersion = "v2.1.1";
        SmartPlug smartPlug1 = SmartPlug.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.OFFLINE)
                .setFirmwareVersion(firmwareVersion)
                .setLastUpdated(now)
                .setIsOn(false)
                .build();

        int affectedRows1 = deviceService.patchSmartPlugTelemetry(mapSmartPlug(smartPlug1));
        Device updatedDevice = verifyAndGetUpdatedDevice(affectedRows1, device, now, firmwareVersion, OFFLINE);

        Map<String, Object> telemetry = readTelemetry(updatedDevice);
        assertFalse((Boolean) telemetry.get("isOn"));

        final Instant now2 = now().toInstant();
        float voltage = 220.2f;
        float current = 217.98f;
        float powerUsage = 98.9f;
        SmartPlug smartPlug2 = SmartPlug.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setLastUpdated(now2)
                .setIsOn(true)
                .setVoltage(voltage)
                .setCurrent(current)
                .setPowerUsage(powerUsage)
                .build();

        int affectedRows2 = deviceService.patchSmartPlugTelemetry(mapSmartPlug(smartPlug2));
        Device updatedDevice2 = verifyAndGetUpdatedDevice(affectedRows2, device, now2, firmwareVersion, ONLINE);

        Map<String, Object> telemetry2 = readTelemetry(updatedDevice2);
        assertEquals(powerUsage, (double) telemetry2.get("powerUsage"), 0.001);
        assertEquals(voltage, (double) telemetry2.get("voltage"), 0.001);
        assertEquals(current, (double) telemetry2.get("current"), 0.001);
    }

    @Test
    void soilMoistureSensorUpdate() throws IOException {
        Device device = getDeviceFromDb();
        final Instant now = now().toInstant();
        String firmwareVersion = "v2.1.1";
        int batteryLevel = 86;
        float soilTemperature = 21.3f;
        SoilMoistureSensor soilMoistureSensor1 = SoilMoistureSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setFirmwareVersion(firmwareVersion)
                .setLastUpdated(now)
                .setBatteryLevel(batteryLevel)
                .setSoilTemperature(soilTemperature)
                .build();

        int affectedRows1 = deviceService.patchSoilMoistureSensorTelemetry(mapSoilMoisture(soilMoistureSensor1));
        Device updatedDevice = verifyAndGetUpdatedDevice(affectedRows1, device, now, firmwareVersion, ONLINE);

        Map<String, Object> telemetry = readTelemetry(updatedDevice);
        assertEquals(batteryLevel, telemetry.get("batteryLevel"));
        assertEquals(soilTemperature, (double) telemetry.get("soilTemperature"), 0.001);

        final Instant now2 = now().toInstant();
        int batteryLevel2 = 82;
        float moisturePercentage = 25.6f;
        SoilMoistureSensor soilMoistureSensor2 = SoilMoistureSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setLastUpdated(now2)
                .setBatteryLevel(batteryLevel2)
                .setMoisturePercentage(moisturePercentage)
                .build();

        int affectedRows2 = deviceService.patchSoilMoistureSensorTelemetry(mapSoilMoisture(soilMoistureSensor2));
        Device updatedDevice2 = verifyAndGetUpdatedDevice(affectedRows2, device, now2, firmwareVersion, ONLINE);

        Map<String, Object> telemetry2 = readTelemetry(updatedDevice2);
        assertEquals(batteryLevel2, telemetry2.get("batteryLevel"));
        assertEquals(moisturePercentage, (double) telemetry2.get("moisturePercentage"), 0.001);
        assertEquals(soilTemperature, (double) telemetry2.get("soilTemperature"), 0.001);
    }

    @Test
    void temperatureSensorUpdate() throws IOException {
        Device device = getDeviceFromDb();
        final Instant now = now().toInstant();
        float temperature = 23.3f;
        String firmwareVersion = "v2.1.1";
        String unit = TempUnit.C.name();
        TemperatureSensor temperatureSensor1 = TemperatureSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setFirmwareVersion(firmwareVersion)
                .setLastUpdated(now)
                .setTemperature(temperature)
                .setUnit(TempUnit.C)
                .build();

        int affectedRows1 = deviceService.patchTemperatureSensorTelemetry(mapTemperatureSensor(temperatureSensor1));
        Device updatedDevice = verifyAndGetUpdatedDevice(affectedRows1, device, now, firmwareVersion, ONLINE);

        Map<String, Object> telemetry = readTelemetry(updatedDevice);
        assertEquals(temperature, (double) telemetry.get("temperature"), 0.001);
        assertEquals(unit, telemetry.get("unit"));

        float humidity = 12.123f;
        TemperatureSensor temperatureSensor2 = TemperatureSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setLastUpdated(now)
                .setHumidity(humidity)
                .build();

        int affectedRows2 = deviceService.patchTemperatureSensorTelemetry(mapTemperatureSensor(temperatureSensor2));
        Device updatedDevice2 = verifyAndGetUpdatedDevice(affectedRows2, device, now, firmwareVersion, ONLINE);

        Map<String, Object> telemetry2 = readTelemetry(updatedDevice2);
        assertEquals(temperature, (double) telemetry2.get("temperature"), 0.001);
        assertEquals(humidity, (double) telemetry2.get("humidity"), 0.001);
        assertEquals(unit, telemetry2.get("unit"));
    }

    @Test
    void thermostatUpdate() throws IOException {
        Device device = getDeviceFromDb();
        final Instant now = now().toInstant();
        float currentTemperature = 23.3f;
        String firmwareVersion = "v2.1.1";
        float targetTemperature = 18.5f;
        Thermostat thermostat1 = Thermostat.newBuilder()
                .setDeviceId(device.getId().toString())
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setFirmwareVersion(firmwareVersion)
                .setLastUpdated(now)
                .setCurrentTemperature(currentTemperature)
                .setTargetTemperature(targetTemperature)
                .build();

        int affectedRows1 = deviceService.patchThermostatTelemetry(mapThermostat(thermostat1));
        Device updatedDevice = verifyAndGetUpdatedDevice(affectedRows1, device, now, firmwareVersion, ONLINE);

        Map<String, Object> telemetry = readTelemetry(updatedDevice);
        assertEquals(currentTemperature, (double) telemetry.get("currentTemperature"), 0.001);
        assertEquals(targetTemperature, (double) telemetry.get("targetTemperature"), 0.001);

        final Instant now2 = now().toInstant();
        float currentTemperature2 = 19.7f;
        float humidity2 = 5.5f;
        ThermostatMode mode = ThermostatMode.COOL;
        Thermostat thermostat2 = Thermostat.newBuilder()
                .setDeviceId(device.getId().toString())
                .setLastUpdated(now2)
                .setCurrentTemperature(currentTemperature2)
                .setHumidity(humidity2)
                .setMode(mode)
                .build();

        int affectedRows2 = deviceService.patchThermostatTelemetry(mapThermostat(thermostat2));
        Device updatedDevice2 = verifyAndGetUpdatedDevice(affectedRows2, device, now2, firmwareVersion, ONLINE);

        Map<String, Object> telemetry2 = readTelemetry(updatedDevice2);
        assertEquals(currentTemperature2, (double) telemetry2.get("currentTemperature"), 0.001);
        assertEquals(targetTemperature, (double) telemetry2.get("targetTemperature"), 0.001);
        assertEquals(humidity2, (double) telemetry2.get("humidity"), 0.001);
        assertEquals(mode.name(), telemetry2.get("mode"));
    }

    private Device getDeviceFromDb() {
        Page<User> userPage = usersRepository.findAll(PageRequest.of(0, 1));
        Optional<User> userOptional = userPage.stream().findFirst();
        assertTrue(userOptional.isPresent());
        User user = userOptional.get();
        Optional<Device> optionalDevice = user.getDevices().stream().findFirst();
        assertTrue(optionalDevice.isPresent());
        return optionalDevice.get();
    }

    private Device verifyAndGetUpdatedDevice(int affectedRows1, Device device, Instant now, String firmwareVersion, DeviceStatus status) {
        entityManager.clear();
        assertEquals(1, affectedRows1);

        Optional<Device> updatedDeviceOptional = deviceService.findByDeviceId(device.getId());
        assertTrue(updatedDeviceOptional.isPresent());
        Device updatedDevice = updatedDeviceOptional.get();

        assertEquals(status, updatedDevice.getStatus());
        assertEquals(now.truncatedTo(MILLIS), updatedDevice.getUpdatedAt().toInstant());
        assertEquals(firmwareVersion, updatedDevice.getFirmwareVersion());
        return updatedDevice;
    }

    private Map<String, Object> readTelemetry(Device updatedDevice2) throws IOException {
        return objectMapper.readValue(updatedDevice2.getTelemetry(), new TypeReference<>() {});
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.iot.devices.management.registry_service.persistence.repos")
    @EntityScan(basePackages = "com.iot.devices.management.registry_service.persistence.model")
    static class TestPersistenceConfig {}

}