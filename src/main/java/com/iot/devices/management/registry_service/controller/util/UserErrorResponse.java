package com.iot.devices.management.registry_service.controller.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Map;

@ToString
@RequiredArgsConstructor(staticName = "of")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserErrorResponse {
    private final HttpStatus status;
    private final String errorMessage;
    private final String detail;
    private final URI uri;
    private final Map<String, String> validationErrors;
}
