package com.iot.devices.management.registry_service.open.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Ihor Lapka",
                        email = "lapkaihor@gmail.com"
                ),
                description = "Open Api documentation for Registry Service",
                version = "1.0.0"
        ),
        servers = @Server(
                description = "local environment",
                url = "localhost:8080/iot-registry"
        )
)
public class OpenApiConfig {
}
