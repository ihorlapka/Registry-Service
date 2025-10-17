package com.iot.devices.management.registry_service.open.api.custom.annotations.users;

import com.iot.devices.management.registry_service.controller.dto.UserDto;
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
        summary = "Create a new admin",
        description = "Adds a new admin to the system",
        responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Admin Created",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = UserDto.class))
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Create Admin Request is invalid",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "Bad request",
                                        summary = "Request is invalid",
                                        value = """
                                                {
                                                    "status": 400,
                                                    "errorMessage": "Validation failed for argument [0]..."
                                                    "detail": "Validation failed for one or more fields!"
                                                    "uri": "/api/v1/users,
                                                    "validationErrors": {
                                                        "username": "username is required"
                                                    }
                                                }
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "409",
                        description = "Admin is already present",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "DuplicatedUserExample",
                                        summary = "Admin already exists",
                                        value = """
                                                {
                                                    "status": 400,
                                                    "errorMessage": "User with email: someemail@gmail.com already exists."
                                                    "detail": "Duplicate user!"
                                                    "uri": "/api/v1/users,
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
                                                    "status": 500 ,
                                                    "errorMessage": "Could not open JDBC Connection for transaction"
                                                    "detail": "Unable to obtain JDBC Connection"
                                                    "uri": "/api/v1/users
                                                }
                                                """
                                )
                        )
                )

        }
)
public @interface CreateAdminOpenApi {
}
