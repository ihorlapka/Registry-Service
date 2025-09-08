package com.iot.devices.management.registry_service.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.MINUTES;

@Slf4j
@EnableCaching
@Configuration
public class CacheConfig {

    public static final String PROPERTIES_PREFIX = "cache";
    public static final String USERS_CACHE = "usersCache";

    @Bean
    public CacheManager cacheManager(@Value("${" + PROPERTIES_PREFIX + ".initial.capacity}") int initialCapacity,
                                     @Value("${" + PROPERTIES_PREFIX + ".max.size}") int maxSize,
                                     @Value("${" + PROPERTIES_PREFIX + ".expiration.time.min}") int expirationTimeMin) {
        log.info("Creating Caffeine cache with initialCapacity={}, maxSize={}, expirationTimeMin={}",
                initialCapacity, maxSize, expirationTimeMin);
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(USERS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maxSize)
                .expireAfterWrite(expirationTimeMin, MINUTES)
                .recordStats());
        return cacheManager;
    }
}
