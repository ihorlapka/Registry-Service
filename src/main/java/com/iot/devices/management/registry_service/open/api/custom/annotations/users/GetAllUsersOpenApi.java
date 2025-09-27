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
        summary = "Get all users",
        description = "Get all users in the system",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Users are found",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = UserDTO.class))
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "Users are not found",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "UserNotFoundExample",
                                        summary = "Users are not found",
                                        value = """
                                                {
                                                    "status": 400,
                                                    "errorMessage": "No users were found."
                                                    "detail": "Unable to find users!"
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
public @interface GetAllUsersOpenApi {
}
