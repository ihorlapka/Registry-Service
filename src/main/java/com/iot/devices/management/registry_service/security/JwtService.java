package com.iot.devices.management.registry_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.devices.management.registry_service.controller.util.AuthenticationResponse;
import com.iot.devices.management.registry_service.persistence.model.Token;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.repos.TokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TokenRepository tokenRepository;
    private final SecurityProperties securityProperties;

    @Transactional
    public AuthenticationResponse generateTokens(User user) {
        final String accessToken = generateToken(user);
        final String refreshToken = generateRefreshToken(user);
        final List<Token> tokens = List.of(buildTokenEntity(user, accessToken, false),
                buildTokenEntity(user, refreshToken, true));
        List<Token> savedTokens = tokenRepository.saveAll(tokens);
        log.info("Tokens for userId: {}, were saved {}", user.getId(), savedTokens);
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthenticationResponse refreshToken(String currentRefreshToken, User user,
                                               OutputStream outputStream) throws IOException {
        if (isTokenValid(currentRefreshToken, user)) {
            revokeAllUserTokens(user);
            final String accessToken = generateToken(user);
            final String newRefreshToken = buildToken(new HashMap<>(), user, extractExpiration(currentRefreshToken));
            final List<Token> tokens = List.of(buildTokenEntity(user, accessToken, false),
                    buildTokenEntity(user, newRefreshToken, true));
            List<Token> savedTokens = tokenRepository.saveAll(tokens);
            log.info("Tokens for userId: {}, were refreshed {}", user.getId(), savedTokens);
            AuthenticationResponse authResponse = AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken)
                    .build();
            OBJECT_MAPPER.writeValue(outputStream, authResponse);
            return authResponse;
        } else {
            log.warn("Token is not valid {}", currentRefreshToken);
        }
        throw new IllegalArgumentException("Invalid token " + currentRefreshToken);
    }

    private Token buildTokenEntity(User user, String token, boolean isRefresh) {
        return Token.builder()
                .user(user)
                .token(token)
                .expired(false)
                .revoked(false)
                .refresh(isRefresh)
                .build();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private long extractExpiration(String currentRefreshToken) {
        return extractClaim(currentRefreshToken, Claims::getExpiration).getTime();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private void revokeAllUserTokens(User user) {
        final List<Token> validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    private String generateToken(Map<String, Object> claimsToAdd, UserDetails userDetails) {
        return buildToken(claimsToAdd, userDetails, securityProperties.getJwtExpiration());
    }

    private String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, securityProperties.getRefreshExpiration());
    }

    private String buildToken(Map<String, Object> claimsToAdd, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(claimsToAdd)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(securityProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
