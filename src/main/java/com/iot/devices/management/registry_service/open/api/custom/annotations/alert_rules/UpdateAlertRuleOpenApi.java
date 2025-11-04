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
        summary = "Update existing alert rule",
        description = "Updates present alert rule in the system",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Alert rule updated",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = AlertRuleDto.class))
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Patch Alert rule request is invalid",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "Bad request",
                                        summary = "Request is invalid",
                                        value = """
                                                {
                                                    "status": 400,
                                                    "errorMessage": "Param ruleId can not be empty",
                                                    "detail": "Validation failed for one or more fields!",
                                                    "uri": "/api/v1/alertRules,
                                                    "validationErrors": {
                                                        "id": "ruleId is required"
                                                    }
                                                }
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "Alert rule is not found",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "AlertRuleNotFoundExample",
                                        summary = "Alert rule is not found",
                                        value = """
                                                {
                                                    "status": 400,
                                                    "errorMessage": "Alert rule with id: 24d33306-e6bb-4ae7-b071-db0425fbaa60 not found",
                                                    "detail": "Unable to find alert rule!",
                                                    "uri": "/api/v1/alertRules
                                                }
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "Device is not found for alert rule",
                        content = @Content(
                                mediaType = APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = @ExampleObject(
                                        name = "DeviceNotFoundExample",
                                        summary = "Device is not found for alert rule",
                                        value = """
                                                {
                                                    "status": 400,
                                                    "errorMessage": "Device with id: 24d33306-e6bb-4ae7-b071-db0425fbaa60 not found",
                                                    "detail": "Unable to find device!",
                                                    "uri": "/api/v1/alertRule
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
                                                    "uri": "/api/v1/devices
                                                }
                                                """
                                )
                        )
                )

        }
)
public @interface UpdateAlertRuleOpenApi {
}
