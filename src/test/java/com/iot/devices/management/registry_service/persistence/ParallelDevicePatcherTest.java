package com.iot.devices.management.registry_service.persistence;

import com.iot.devices.management.registry_service.kafka.DeadLetterProducer;
import com.iot.devices.management.registry_service.persistence.services.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@Slf4j
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        ParallelDevicePatcher.class,
        DeviceService.class
})
@TestPropertySource("classpath:application-test.yaml")
class ParallelDevicePatcherTest {

    @MockitoBean
    DeadLetterProducer deadLetterProducer;

    @Autowired
    ParallelDevicePatcher parallelDevicePatcher;








}