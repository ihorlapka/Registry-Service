package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.TempUnit;
import com.iot.devices.TemperatureSensor;
import com.iot.devices.management.registry_service.kafka.DeadLetterProducer;
import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import com.iot.devices.management.registry_service.persistence.repos.DevicesRepository;
import com.iot.devices.management.registry_service.persistence.repos.UsersRepository;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.*;

import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {
        ParallelDevicePatcher.class,
        DeviceService.class,
        ParallelDevicePatcherTest.TestPersistenceConfig.class
})
@TestPropertySource("classpath:application-test.yaml")
@Testcontainers
@Sql(scripts = "/schema.sql")
class ParallelDevicePatcherTest {

    @MockitoBean
    DeadLetterProducer deadLetterProducer;

    @Autowired
    ParallelDevicePatcher parallelDevicePatcher;
    @Autowired
    UsersRepository usersRepository;
    @Autowired
    DevicesRepository devicesRepository;

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
                DeviceStatus.valueOf(status), now(), firmwareVersion, now(), now(), null);

        User USER = new User(null, username, firstName, lastName, email, phone, address, passwordHash,
                UserRole.USER, now(), now(), now(), ImmutableSet.of(DEVICE));
        DEVICE.setOwner(USER);

        usersRepository.save(USER);
    }

    @Test
    void test() {
        List<User> all = usersRepository.findAll();
        User user = all.getFirst();
        Optional<Device> optionalDevice = user.getDevices().stream().findFirst();
        assertTrue(optionalDevice.isPresent());
        Device device = optionalDevice.get();

        Map<String, ConsumerRecord<String, SpecificRecord>> records = new HashMap<>();
        TemperatureSensor temperatureSensorUpdate1 = TemperatureSensor.newBuilder()
                .setDeviceId(device.getId().toString())
                .setTemperature(23f)
                .setUnit(TempUnit.C)
                .setStatus(com.iot.devices.DeviceStatus.ONLINE)
                .setLastUpdated(now().toInstant())
                .build();
        records.put(device.getId().toString(), new ConsumerRecord<>("some-topic", 1, 1L, device.getId().toString(), temperatureSensorUpdate1));

        log.info("Updating device...");
        Map<TopicPartition, OffsetAndMetadata> offsets = parallelDevicePatcher.patch(records);

        Optional<Device> updatedDeviceOptional = devicesRepository.findById(device.getId());
        assertTrue(updatedDeviceOptional.isPresent());
        Device updatedDevice = updatedDeviceOptional.get();

    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.iot.devices.management.registry_service.persistence.repos")
    @EntityScan(basePackages = "com.iot.devices.management.registry_service.persistence.model")
    static class TestPersistenceConfig {
    }

}