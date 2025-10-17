package com.iot.devices.management.registry_service.open.api.custom.annotations.devices;

import com.iot.devices.management.registry_service.controller.dto.DeviceDto;
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
        summary = "Remove device by Id",
        description = "Remove device from the system",
        responses = {
                @ApiResponse(
                        responseCode = "204",
                        description = "Device is removed",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = DeviceDto.class))
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "Device is not found",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "DeviceNotFoundExample",
                                        summary = "Device is not found",
                                        value = """
                                                {
                                                    "status": 400,
                                                    "errorMessage": "Device with id: 1 not found."
                                                    "detail": "Unable to find device!"
                                                    "uri": "/api/v1/devices,
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
                                                    "uri": "/api/v1/devices
                                                }
                                                """
                                )
                        )
                )

        }
)
public @interface RemoveDeviceByIdOpenApi {
}
