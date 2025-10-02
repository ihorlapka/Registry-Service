package com.iot.devices.management.registry_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import static com.iot.devices.management.registry_service.persistence.model.enums.UserRole.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthentificationFilter jwtAuthFilter;
    private final LogoutHandler logoutHandler;

    private final String[] WHITE_LIST = {
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers(POST, "/api/v1/authentication/login").permitAll()
                        .requestMatchers(POST, "/api/v1/users/registerUser").permitAll()
                        .requestMatchers(POST, "/api/v1/users/registerAdmin").hasRole(SUPER_ADMIN.name())
                        .requestMatchers(GET, "/api/v1/users/all").hasAnyRole(ADMIN.name(), SUPER_ADMIN.name())
                        .requestMatchers(GET, "/api/v1/users/me").hasAnyRole(USER.name(), ADMIN.name(), SUPER_ADMIN.name())
                        .requestMatchers(GET, "/api/v1/users/username/*").hasAnyRole(ADMIN.name(), SUPER_ADMIN.name())
                        .requestMatchers(GET, "/api/v1/users/email/*").hasAnyRole(ADMIN.name(), SUPER_ADMIN.name())
                        .requestMatchers(GET, "/api/v1/users/*").hasAnyRole(ADMIN.name(), SUPER_ADMIN.name())
                        .requestMatchers("/api/v1/users/**").hasAnyRole(USER.name(), ADMIN.name(), SUPER_ADMIN.name())
                        .requestMatchers("/api/v1/devices/**").hasAnyRole(USER.name(), ADMIN.name(), SUPER_ADMIN.name())
                        .anyRequest()
                        .authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout ->
                        logout.logoutUrl("/api/v1/authentication/logout")
                                .addLogoutHandler(logoutHandler)
                                .logoutSuccessHandler((req, resp, auth) -> SecurityContextHolder.clearContext())
                )
                .build();
    }
}
