package com.iot.devices.management.registry_service.controller.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Map;

@Getter
@ToString
@RequiredArgsConstructor(staticName = "of")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Schema(description = "HTTP status code")
    private final HttpStatus status;
    @Schema(description = "Error message")
    private final String errorMessage;
    @Schema(description = "Error detail")
    private final String detail;
    @Schema(description = "Error uri")
    private final URI uri;
    @Schema(description = "Error due to validation failures")
    private final Map<String, String> validationErrors;
}
