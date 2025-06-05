package com.barakah.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class JwtExtractor {

    public Optional<String> getCurrentToken() {
        return getCurrentJwt().map(Jwt::getTokenValue);
    }

    public Optional<String> getCurrentUserId() {
        return getCurrentJwt().map(jwt -> jwt.getClaimAsString("sub"));
    }

    public Optional<String> getCurrentUsername() {
        return getCurrentJwt()
            .map(jwt -> {
                String username = jwt.getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwt.getClaimAsString("username");
                }
                if (username == null) {
                    username = jwt.getClaimAsString("email");
                }
                return username;
            });
    }

    public List<String> getCurrentRoles() {
        return getCurrentJwt()
            .map(jwt -> {
                Object rolesObj = jwt.getClaim("roles");
                if (rolesObj instanceof List<?> rolesList) {
                    return rolesList.stream()
                        .map(Object::toString)
                        .toList();
                }
                return List.<String>of();
            })
            .orElse(List.of());
    }

    private Optional<Jwt> getCurrentJwt() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                return Optional.of(jwtAuth.getToken());
            }
        } catch (Exception e) {
            log.debug("No JWT found in security context: {}", e.getMessage());
        }
        return Optional.empty();
    }
}