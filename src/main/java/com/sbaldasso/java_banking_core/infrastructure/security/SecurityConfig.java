package com.sbaldasso.java_banking_core.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Ledger API.
 * 
 * Security Model:
 * - OAuth2 JWT Authentication with Keycloak
 * - Stateless sessions (no server-side session storage)
 * - Role-based access control (ADMIN, USER)
 * 
 * Authentication Flow:
 * 1. Client obtains JWT token from Keycloak
 * 2. Client sends JWT in Authorization header (Bearer token)
 * 3. Application validates JWT signature and extracts roles
 * 4. Spring Security enforces role-based access control
 * 
 * Endpoints:
 * - Public: Health checks, actuator endpoints
 * - Authenticated: All ledger operations
 * - Admin only: Account management (create, block, close)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${keycloak.client-id:banking-api}")
    private String keycloakClientId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disabled for API; use CSRF tokens in production web apps
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health").permitAll()

                        // Account operations - ADMIN only for creation and management
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/block").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/unblock").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/close").hasRole("ADMIN")

                        // Transaction operations - both USER and ADMIN
                        .requestMatchers("/api/v1/transactions/**").hasAnyRole("USER", "ADMIN")

                        // Balance queries - both USER and ADMIN
                        .requestMatchers("/api/v1/balances/**").hasAnyRole("USER", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated())

                // Configure OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))

                // Stateless sessions for API
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    /**
     * JWT decoder that validates tokens against Keycloak's public key.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * Converts JWT to Spring Security Authentication.
     * Extracts roles from Keycloak JWT and maps them to authorities.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Use custom converter to extract Keycloak roles
        KeycloakJwtConverter keycloakConverter = new KeycloakJwtConverter(keycloakClientId);
        converter.setJwtGrantedAuthoritiesConverter(keycloakConverter);

        // Set principal claim name (use preferred_username from Keycloak)
        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }
}
