package com.iot.devices.management.registry_service.persistence.services;

import com.iot.devices.management.registry_service.RegistryServiceApplication;
import com.iot.devices.management.registry_service.alerts.StandardAlertRulesProvider;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.kafka.AlertingRulesKafkaProducer;
import com.iot.devices.management.registry_service.kafka.properties.AlertingRulesKafkaProducerProperties;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Optional;

import static java.time.OffsetDateTime.now;

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
        StandardAlertRulesProvider.class,
        AlertingRulesKafkaProducer.class,
        AlertingRulesKafkaProducerProperties.class,
        SimpleMeterRegistry.class,
})
@TestPropertySource("classpath:application-test.yaml")
@Testcontainers
public class DeviceServiceTest {
    @Autowired
    UsersRepository usersRepository;
    @Autowired
    DeviceService deviceService;
    @Autowired
    EntityManager entityManager;
    @MockitoBean
    PasswordEncoder passwordEncoder;

    ObjectMapper objectMapper = new ObjectMapper();

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.5"))
            .withInitScript("schema.sql");

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.9.0"));

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("kafka.producer.alerting-rules.properties.bootstrap.servers", kafkaContainer::getBootstrapServers);
    }

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

    @BeforeEach
    void setUp() {
        Optional<User> user = usersRepository.findByUsername(username);
        if (user.isEmpty()) {
            User USER = new User(null, username, firstName, lastName, email, phone, address, passwordHash,
                    UserRole.USER, now(), now(), now(), ImmutableSet.of(), ImmutableList.of());
            usersRepository.save(USER);
        }
    }

    @Test
    void test() {

        deviceService.saveAndSendMessage(new CreateDeviceRequest(name, serialNumber, DeviceManufacturer.BOSCH, model, DeviceType.valueOf(deviceType),
                location, new BigDecimal(latitude), new BigDecimal(longitude), null,
                DeviceStatus.valueOf(status), now(), firmwareVersion, ImmutableSet.of()), null);
    }
}
