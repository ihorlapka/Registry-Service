package com.iot.devices.management.registry_service.persistence.services;

import com.iot.devices.management.registry_service.kafka.AlertingRulesKafkaProducer;
import com.iot.devices.management.registry_service.persistence.repos.AlertRulesRepository;
import com.iot.devices.management.registry_service.persistence.repos.DeviceAlertRuleRepository;
import com.iot.devices.management.registry_service.persistence.repos.DevicesRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {
        AlertRulesRepository.class,
        DeviceAlertRuleRepository.class,
        DevicesRepository.class,
        AlertingRulesKafkaProducer.class
})
@EnableJpaRepositories
class AlertRuleServiceTest {

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


}