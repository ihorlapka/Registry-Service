package com.iot.devices.management.registry_service.open.api.custom.annotations.alert_rules;

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
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
        summary = "Returns alert rules for my user",
        description = "Get present alert rule in the system",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Alert rules are found",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = List.class))
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
                                                    "uri": "/api/v1/devices
                                                }
                                                """
                                )
                        )
                )

        }
)
public @interface GetUserAlertRulesOpenApi {
}
