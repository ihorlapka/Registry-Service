package com.iot.devices.management.registry_service.controller.errors;

import org.springframework.security.core.AuthenticationException;

public class AuthenticationHeaderException extends AuthenticationException {

    public AuthenticationHeaderException(String msg) {
        super(msg);
    }
}
