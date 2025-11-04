package com.iot.devices.management.registry_service.open.api.custom.annotations.alert_rules;

import com.iot.devices.management.registry_service.controller.dto.AlertRuleDto;
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
        summary = "Remove alert rule by Id",
        description = "Remove alert rule from the system",
        responses = {
                @ApiResponse(
                        responseCode = "204",
                        description = "Alert rule is removed",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = AlertRuleDto.class))
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
public @interface RemoveAlertRuleByIdOpenApi {
}
