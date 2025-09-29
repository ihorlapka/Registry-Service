package com.iot.devices.management.registry_service.security;

import com.iot.devices.management.registry_service.persistence.repos.TokenRepository;
import com.iot.devices.management.registry_service.persistence.model.Token;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.iot.devices.management.registry_service.security.JwtAuthentificationFilter.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

    private final TokenRepository tokenRepository;

    @Transactional
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) { //todo: verify if dirty checking works
        final String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            return;
        }
        final String jwt = authHeader.substring(TOKEN_BEGIN_INDEX);
        final Optional<Token> tokenOptional = tokenRepository.findByToken(jwt);
        if (tokenOptional.isPresent()) {
            Token token = tokenOptional.get();
            token.setExpired(true);
            token.setRevoked(true);
            SecurityContextHolder.clearContext();
        } else {
            log.warn("Unable to find token: {}", jwt);
        }
    }
}
