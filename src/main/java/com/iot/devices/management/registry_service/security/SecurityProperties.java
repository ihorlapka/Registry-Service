package com.iot.devices.management.registry_service.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.iot.devices.management.registry_service.security.SecurityProperties.PROPERTIES_PREFIX;

@Slf4j
@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(PROPERTIES_PREFIX)
@RequiredArgsConstructor
public class SecurityProperties {

    final static String PROPERTIES_PREFIX = "security.jwt";

    @Value("${" + PROPERTIES_PREFIX + ".secret-key}")
    private String secretKey;
    @Value("${" + PROPERTIES_PREFIX + ".expiration}")
    private long jwtExpiration;
    @Value("${" + PROPERTIES_PREFIX + ".refresh-token.expiration}")
    private long refreshExpiration;

    @PostConstruct
    private void logProperties() {
        log.info("security properties: {}", this);
    }
}
