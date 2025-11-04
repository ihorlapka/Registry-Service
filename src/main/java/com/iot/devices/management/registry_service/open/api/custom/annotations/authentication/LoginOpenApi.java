package com.iot.devices.management.registry_service.open.api.custom.annotations.authentication;

import com.iot.devices.management.registry_service.controller.util.AuthenticationResponse;
import com.iot.devices.management.registry_service.controller.util.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
        summary = "User Authentication",
        description = "Login user to the system",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "User is authenticated",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = AuthenticationResponse.class))
                ),
                @ApiResponse(
                        responseCode = "403",
                        description = "Create alert rule request is invalid",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "AuthenticationException",
                                        summary = "Username or password is incorrect",
                                        value = """
                                                {
                                                    "status": "FORBIDDEN",
                                                    "errorMessage": "Bad credentials",
                                                    "detail": "Unable to authenticate user!",
                                                    "uri": "uri=/iot-registry/api/v1/authentication/login",
                                                    "validationErrors": {}
                                                }
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "Server error",
                                        summary = "Server is down",
                                        value = """
                                                {
                                                  "status": 500,
                                                  "errorMessage": "Could not open JDBC Connection for transaction",
                                                  "detail": "Unable to obtain JDBC Connection",
                                                  "uri": "/api/v1/alertRules
                                                }
                                                """
                                )
                        )
                )

        }
)
public @interface LoginOpenApi {
}
