package com.iot.devices.management.registry_service.open.api.custom.annotations.users;

import com.iot.devices.management.registry_service.controller.dto.UserDTO;
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
        summary = "Update existing user",
        description = "Updates present user in the system",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "User Updated",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = UserDTO.class))
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Patch User Request is invalid",
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
                                                        "username": "id is required"
                                                    }
                                                }
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "User is not found",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "UserNotFoundExample",
                                        summary = "User is not found",
                                        value = """
                                                {
                                                    "status": 400,
                                                    "errorMessage": "User with id: 1 not found."
                                                    "detail": "Unable to find user!"
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
public @interface UpdateUserOpenApi {
}
