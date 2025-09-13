package org.yaroslaavl.cvservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.yaroslaavl.cvservice.config.converter.KeyCloakAuthenticationRoleConverter;

import java.util.Collection;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer
                                .jwtAuthenticationConverter(jwtToken -> {
                                    Collection<GrantedAuthority> authorities = new KeyCloakAuthenticationRoleConverter().convert(jwtToken);
                                    return new JwtAuthenticationToken(jwtToken, authorities);
                                })
                        )
                )
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers(
                                        "/error",
                                        "/actuator/health").permitAll()
                                .requestMatchers(
                                        "/api/v1/cv/*/recruiter").hasRole("INTERNAL_SERVICE")
                                .requestMatchers(
                                        "/api/v1/cv/upload",
                                        "/api/v1/cv/*",
                                        "/api/v1/cv/*/candidate"
                                        ).hasRole("VERIFIED_CANDIDATE")
                );

        return http.build();
    }
}