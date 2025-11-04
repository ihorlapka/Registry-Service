package com.iot.devices.management.registry_service.persistence.services;

import com.iot.devices.management.registry_service.RegistryServiceApplication;
import com.iot.devices.management.registry_service.alerts.DefaultAlertRulesProvider;
import com.iot.devices.management.registry_service.controller.util.CreateAlertRuleRequest;
import com.iot.devices.management.registry_service.controller.util.CreateDeviceRequest;
import com.iot.devices.management.registry_service.controller.util.PatchAlertRuleRequest;
import com.iot.devices.management.registry_service.controller.util.PatchDeviceRequest;
import com.iot.devices.management.registry_service.kafka.AlertingRulesKafkaProducer;
import com.iot.devices.management.registry_service.kafka.properties.AlertingRulesKafkaProducerProperties;
import com.iot.devices.management.registry_service.persistence.model.AlertRule;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.*;

import static com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer.BOSCH;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.MetricType.PRESSURE;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.SeverityLevel.*;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType.LESS_THAN;
import static com.iot.devices.management.registry_service.persistence.model.enums.alerts.ThresholdType.NOT_EQUAL_TO;
import static java.time.OffsetDateTime.now;
import static java.util.stream.Collectors.toSet;
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
        AlertRulesRepository.class,
        DeviceAlertRuleRepository.class,
        DefaultAlertRulesProvider.class,
        AlertingRulesKafkaProducer.class,
        AlertingRulesKafkaProducerProperties.class,
        AlertRuleService.class
})
@Testcontainers
public class DeviceAndAlertRulesServicesTest {
    @Autowired
    UsersRepository usersRepository;
    @Autowired
    DeviceService deviceService;
    @Autowired
    DeviceAlertRuleRepository deviceAlertRuleRepository;
    @Autowired
    AlertRulesRepository alertRulesRepository;
    @Autowired
    AlertRuleService alertRuleService;

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
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers); //needed for kafkaListener
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
    DeviceManufacturer manufacturer = BOSCH;
    String model = "BME280";
    String deviceType = "TEMPERATURE_SENSOR";
    String location = "Living Room";
    String latitude = "50.450100";
    String longitude = "30.523400";
    String status = "ONLINE";
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

    @AfterEach
    void cleanUp() {
        deviceAlertRuleRepository.deleteAll();
        alertRulesRepository.deleteAll();
        usersRepository.deleteAll();
    }

    private final Map<String, com.iot.alerts.AlertRule> messagesByKey = new HashMap<>();

    @KafkaListener(topics = "iot-alerting-rules", groupId = "test-group", properties = {
            "key.deserializer:org.apache.kafka.common.serialization.StringDeserializer",
            "value.deserializer:io.confluent.kafka.serializers.KafkaAvroDeserializer",
            "schema.registry.url:mock://my-scope:8081",
            "specific.avro.reader:true"
    })
    public void listen(ConsumerRecord<String, com.iot.alerts.AlertRule> record) {
        final com.iot.alerts.AlertRule value = record.value();
        if (value == null) {
            log.info("Received tombstone for key={}", record.key());
        }
        messagesByKey.put(record.key(), record.value());
    }

    @Test
    void testDeviceCrudOperations() {
        Optional<User> user = usersRepository.findByUsername(username);
        assertTrue(user.isPresent());

        CreateDeviceRequest createRequest = new CreateDeviceRequest(name, serialNumber, manufacturer, model, DeviceType.valueOf(deviceType),
                location, new BigDecimal(latitude), new BigDecimal(longitude), user.get().getId(),
                DeviceStatus.valueOf(status), now(), firmwareVersion, ImmutableSet.of());

        Device savedDevice1 = deviceService.saveAndSendMessage(createRequest, user.get());

        assertNotNull(savedDevice1);
        List<AlertRule> alertRulesByUsername = alertRulesRepository.findAlertRulesByUsername(username);
        assertEquals(12, alertRulesByUsername.size());
        assertEquals(12, deviceAlertRuleRepository.findAllByDeviceId(savedDevice1.getId()).size());
        assertEquals(12, messagesByKey.size());
        messagesByKey.clear();

        AlertRule newAlertRule1 = alertRuleService.saveAndSendMessage(new CreateAlertRuleRequest(Set.of(savedDevice1.getId()), PRESSURE, NOT_EQUAL_TO, 760f, INFO, true, username), user.get());
        AlertRule newAlertRule2 = alertRuleService.saveAndSendMessage(new CreateAlertRuleRequest(Set.of(savedDevice1.getId()), PRESSURE, LESS_THAN, 750f, INFO, true, username), user.get());

        assertNotNull(newAlertRule1);
        assertNotNull(newAlertRule2);
        assertEquals(14, alertRulesRepository.findAlertRulesByUsername(username).size());
        assertEquals(14, deviceAlertRuleRepository.findAllByDeviceId(savedDevice1.getId()).size());
        assertEquals(2, messagesByKey.size());
        messagesByKey.clear();

        String updatedName = "Bad Room Temperature Sensor";
        List<AlertRule> alertRulesInfos = alertRulesByUsername.stream().filter(x -> x.getSeverity().equals(INFO)).toList();
        AlertRule alertRuleToRemove1 = alertRulesInfos.get(0);
        AlertRule alertRuleToRemove2 = alertRulesInfos.get(1);
        log.info("Removing: {}", List.of(alertRuleToRemove1, alertRuleToRemove2));

        PatchDeviceRequest patchRequest = PatchDeviceRequest.builder()
                .id(savedDevice1.getId())
                .name(updatedName)
                .alertRulesToRemove(Set.of(alertRuleToRemove1.getRuleId(), alertRuleToRemove2.getRuleId()))
                .build();
        Device patchedDevice = deviceService.patch(patchRequest, user.get());

        assertEquals(updatedName, patchedDevice.getName());
        List<AlertRule> updatedAlertRules = alertRulesRepository.findAlertRulesByUsername(username);
        assertEquals(12, updatedAlertRules.size());
        assertEquals(12, deviceAlertRuleRepository.findAllByDeviceId(savedDevice1.getId()).size());
        assertEquals(2, messagesByKey.size());
        assertEquals(2, messagesByKey.values().stream().filter(Objects::isNull).count());
        messagesByKey.clear();

        Set<UUID> alertRuleIds = updatedAlertRules.stream()
                .filter(x -> x.getSeverity().equals(CRITICAL) || x.getSeverity().equals(WARNING))
                .map(AlertRule::getRuleId)
                .collect(toSet());

        CreateDeviceRequest createRequest2 = new CreateDeviceRequest(name, "SN-87123-XXX", manufacturer, model, DeviceType.valueOf(deviceType),
                location, new BigDecimal(latitude), new BigDecimal(longitude), user.get().getId(), DeviceStatus.valueOf(status), now(),
                firmwareVersion, alertRuleIds
        );

        log.info("AlertRuleIds size: {}", alertRuleIds.size());
        Device savedDevice2 = deviceService.saveAndSendMessage(createRequest2, user.get());

        assertNotNull(savedDevice2);
        assertEquals(12, alertRulesRepository.findAlertRulesByUsername(username).size());
        assertEquals(8, deviceAlertRuleRepository.findAllByDeviceId(savedDevice2.getId()).size());

        int removed = deviceService.removeById(savedDevice1.getId(), user.get());

        assertEquals(1, removed);
        assertTrue(deviceAlertRuleRepository.findAllByDeviceId(savedDevice1.getId()).isEmpty());
        assertEquals(8, alertRulesRepository.findAlertRulesByUsername(username).size());

        int removed2 = deviceService.removeById(savedDevice2.getId(), user.get());

        assertEquals(1, removed2);
        assertTrue(deviceAlertRuleRepository.findAllByDeviceId(savedDevice2.getId()).isEmpty());
        assertEquals(0, alertRulesRepository.findAlertRulesByUsername(username).size());
    }

    @Test
    void testAlertRulesCrudOperations() {
        Optional<User> user = usersRepository.findByUsername(username);
        assertTrue(user.isPresent());

        CreateDeviceRequest createRequest1 = new CreateDeviceRequest(name, "SN-87123-XXX", manufacturer, model, DeviceType.valueOf(deviceType),
                location, new BigDecimal(latitude), new BigDecimal(longitude), user.get().getId(),
                DeviceStatus.valueOf(status), now(), firmwareVersion, ImmutableSet.of());
        Device savedDevice1 = deviceService.saveAndSendMessage(createRequest1, user.get());

        List<AlertRule> alertRulesByUsername = alertRulesRepository.findAlertRulesByUsername(username);

        CreateDeviceRequest createRequest2 = new CreateDeviceRequest(name, "SN-87123-YYY", manufacturer, model, DeviceType.valueOf(deviceType),
                location, new BigDecimal(latitude), new BigDecimal(longitude), user.get().getId(),
                DeviceStatus.valueOf(status), now(), firmwareVersion, alertRulesByUsername.stream().map(AlertRule::getRuleId).collect(toSet()));
        Device savedDevice2 = deviceService.saveAndSendMessage(createRequest2, user.get());

        assertNotNull(savedDevice1);
        assertNotNull(savedDevice2);
        List<AlertRule> defaultAlertRulesByUsername = alertRulesRepository.findAlertRulesByUsername(username);
        assertEquals(12, defaultAlertRulesByUsername.size());
        assertEquals(12, deviceAlertRuleRepository.findAllByDeviceId(savedDevice1.getId()).size());
        assertEquals(12, messagesByKey.size());
        messagesByKey.clear();

        AlertRule newAlertRule1 = alertRuleService.saveAndSendMessage(new CreateAlertRuleRequest(Set.of(savedDevice1.getId()),
                PRESSURE, NOT_EQUAL_TO, 760f, INFO, true, username), user.get());
        AlertRule newAlertRule2 = alertRuleService.saveAndSendMessage(new CreateAlertRuleRequest(Set.of(savedDevice1.getId(), savedDevice2.getId()),
                PRESSURE, LESS_THAN, 750f, INFO, true, username), user.get());

        assertNotNull(newAlertRule1);
        assertNotNull(newAlertRule2);
        assertEquals(14, alertRulesRepository.findAlertRulesByUsername(username).size());
        assertEquals(14, deviceAlertRuleRepository.findAllByDeviceId(savedDevice1.getId()).size());
        assertEquals(13, deviceAlertRuleRepository.findAllByDeviceId(savedDevice2.getId()).size());
        assertEquals(2, messagesByKey.size());
        messagesByKey.clear();

        List<AlertRule> alertRulesInfos = defaultAlertRulesByUsername.stream()
                .filter(alertRule -> !alertRule.getSeverity().equals(INFO)).toList();

        alertRuleService.removeAndSendTombstone(alertRulesInfos.get(0).getRuleId());
        alertRuleService.removeAndSendTombstone(alertRulesInfos.get(1).getRuleId());

        assertNotNull(newAlertRule1);
        assertNotNull(newAlertRule2);
        assertEquals(12, alertRulesRepository.findAlertRulesByUsername(username).size());
        assertEquals(12, deviceAlertRuleRepository.findAllByDeviceId(savedDevice1.getId()).size());
        assertEquals(11, deviceAlertRuleRepository.findAllByDeviceId(savedDevice2.getId()).size());
        assertEquals(2, messagesByKey.size());
        messagesByKey.clear();


        AlertRule updatedAlertRule1 = alertRuleService.patchAndSendMessage(PatchAlertRuleRequest.builder()
                .ruleId(newAlertRule1.getRuleId())
                .deviceIdsToAdd(Set.of(savedDevice2.getId()))
                .deviceIdsToRemove(Set.of(savedDevice1.getId()))
                .severity(WARNING)
                .build(), user.get());

        assertNotNull(updatedAlertRule1);
        assertEquals(12, alertRulesRepository.findAlertRulesByUsername(username).size());
        assertEquals(11, deviceAlertRuleRepository.findAllByDeviceId(savedDevice1.getId()).size());
        assertEquals(12, deviceAlertRuleRepository.findAllByDeviceId(savedDevice2.getId()).size());
        assertEquals(1, messagesByKey.size());
        messagesByKey.clear();
    }
}
