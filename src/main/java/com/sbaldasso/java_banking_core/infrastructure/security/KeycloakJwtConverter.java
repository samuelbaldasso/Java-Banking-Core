package com.sbaldasso.java_banking_core.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts Keycloak JWT tokens to Spring Security authorities.
 * 
 * Keycloak stores roles in the JWT token under:
 * - realm_access.roles (for realm-level roles)
 * - resource_access.{clientId}.roles (for client-specific roles)
 * 
 * This converter extracts these roles and maps them to Spring Security
 * GrantedAuthority objects with the ROLE_ prefix.
 */
public class KeycloakJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final String clientId;

    public KeycloakJwtConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = new ArrayList<>();

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);
        if (realmAccess != null && realmAccess.containsKey(ROLES)) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get(ROLES);
            roles.addAll(realmRoles);
        }

        // Extract client roles
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
        if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
            if (clientAccess.containsKey(ROLES)) {
                @SuppressWarnings("unchecked")
                List<String> clientRoles = (List<String>) clientAccess.get(ROLES);
                roles.addAll(clientRoles);
            }
        }

        // Convert to Spring Security authorities with ROLE_ prefix
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}
