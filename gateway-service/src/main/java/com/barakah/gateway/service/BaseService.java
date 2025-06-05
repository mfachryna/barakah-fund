package com.barakah.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Slf4j
public class BaseService {
    public String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                return jwt.getClaimAsString("sub");
            }
            return auth.getName();
        } catch (Exception e) {
            log.warn("Failed to get current user ID: {}", e.getMessage());
            return "anonymous";
        }
    }

    public com.barakah.common.proto.v1.PageRequest createPageRequest(Pageable pageable) {
        com.barakah.common.proto.v1.PageRequest.Builder builder
                = com.barakah.common.proto.v1.PageRequest.newBuilder()
                .setPage(pageable.getPageNumber())
                .setSize(pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                builder.setSort(order.getProperty());
                builder.setDirection(order.getDirection().name());
            });
        }

        return builder.build();
    }
}
