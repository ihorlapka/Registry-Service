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
    private final JwtService jwtService;

    @Transactional
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        final String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            log.warn("Unable to logout authorization header not provided or invalid token");
            return;
        }
        final String jwt = authHeader.substring(TOKEN_BEGIN_INDEX);
        final String username = jwtService.extractUsername(jwt);
        final Optional<Token> token = tokenRepository.findByToken(jwt);
        if (token.isPresent()) {
            if (token.get().isExpired() || token.get().isRevoked()) {
                log.info("Token is already expired or has been revoked {}", jwt);
                return;
            }
            SecurityContextHolder.clearContext();
            log.info("Logout successful username: {}", username);
            final int rows = tokenRepository.removeAllByUserId(token.get().getUser().getId());
            log.info("{} tokens were removed for username: {}", rows, username);
        } else {
            log.warn("Unable to find token in db for username: {}, token: {}", username, jwt);
        }
    }
}
