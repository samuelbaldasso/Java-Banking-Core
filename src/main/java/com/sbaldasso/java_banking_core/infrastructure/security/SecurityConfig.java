package com.sbaldasso.java_banking_core.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Ledger API.
 * 
 * Security Model:
 * - Basic Authentication for simplicity (can be upgraded to JWT/OAuth2)
 * - Stateless sessions (no server-side session storage)
 * - Role-based access control (ADMIN, USER)
 * 
 * Endpoints:
 * - Public: Health checks, actuator endpoints
 * - Authenticated: All ledger operations
 * - Admin only: Account management (block, close)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disabled for API; use CSRF tokens in production web apps
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health").permitAll()

                        // Account operations
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/block").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/unblock").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/close").hasRole("ADMIN")

                        // Transaction operations
                        .requestMatchers("/api/v1/transactions/**").hasAnyRole("USER", "ADMIN")

                        // Balance queries
                        .requestMatchers("/api/v1/balances/**").hasAnyRole("USER", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated())
                .httpBasic(basic -> {
                }) // Enable HTTP Basic Authentication
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless
                                                                                                             // sessions
                                                                                                             // for API
                );

        return http.build();
    }

    /**
     * Password encoder using BCrypt.
     * BCrypt is a secure hashing algorithm designed for passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * In-memory user details service for demonstration.
     * 
     * In production, this should be replaced with:
     * - Database-backed UserDetailsService
     * - LDAP authentication
     * - OAuth2 / OIDC integration
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("user123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }
}
