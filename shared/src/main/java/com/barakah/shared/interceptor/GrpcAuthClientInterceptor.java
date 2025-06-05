package com.barakah.shared.interceptor;

import com.barakah.shared.context.UserContext;
import com.barakah.shared.context.UserContextHolder;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GrpcAuthClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> USER_ID_KEY
            = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USERNAME_KEY
            = Metadata.Key.of("x-username", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ROLES_KEY
            = Metadata.Key.of("x-roles", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TOKEN_KEY
            = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> SERVICE_AUTH_KEY
            = Metadata.Key.of("x-service-auth", Metadata.ASCII_STRING_MARSHALLER);

    @Value("${vault:service-auth#internal-key:barakah-service-key-2024}")
    private String serviceAuthToken;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                headers.put(SERVICE_AUTH_KEY, serviceAuthToken);

                boolean jwtExtracted = extractJwtAuthentication(headers);

                if (!jwtExtracted) {
                    extractUserContext(headers);
                }

                super.start(responseListener, headers);
            }

            private boolean extractJwtAuthentication(Metadata headers) {
                try {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();
                        String token = jwt.getTokenValue();

                        headers.put(TOKEN_KEY, "Bearer " + token);

                        String userId = jwt.getClaimAsString("sub");
                        String username = extractUsername(jwt);
                        List<String> roles = extractRoles(jwt);

                        if (userId != null) {
                            headers.put(USER_ID_KEY, userId);
                        }

                        if (username != null) {
                            headers.put(USERNAME_KEY, username);
                        }

                        if (!roles.isEmpty()) {
                            headers.put(ROLES_KEY, String.join(",", roles));
                        }

                        log.debug("Extracted JWT auth for user: {} (ID: {})", username, userId);
                        return true;
                    }
                } catch (Exception e) {
                    log.debug("Failed to extract JWT from security context: {}", e.getMessage());
                }
                return false;
            }

            private void extractUserContext(Metadata headers) {
                UserContext currentUser = UserContextHolder.getContext();
                log.debug("Current User in client interceptor: {}", currentUser);

                if (currentUser != null) {
                    if (currentUser.getUserId() != null) {
                        headers.put(USER_ID_KEY, currentUser.getUserId());
                    }

                    if (currentUser.getUsername() != null) {
                        headers.put(USERNAME_KEY, currentUser.getUsername());
                    }

                    if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
                        headers.put(ROLES_KEY, String.join(",", currentUser.getRoles()));
                    }

                    if (currentUser.getToken() != null) {
                        headers.put(TOKEN_KEY, "Bearer " + currentUser.getToken());
                    }

                    log.debug("Added UserContext auth metadata for user: {}", currentUser.getUsername());
                } else {
                    log.debug("No user context available, using service-to-service auth only");
                }
            }

            private String extractUsername(Jwt jwt) {

                String username = jwt.getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwt.getClaimAsString("username");
                }
                if (username == null) {
                    username = jwt.getClaimAsString("email");
                }
                if (username == null) {
                    username = jwt.getClaimAsString("name");
                }
                return username;
            }

            private List<String> extractRoles(Jwt jwt) {
                try {

                    Object rolesObj = jwt.getClaim("roles");
                    if (rolesObj == null) {
                        rolesObj = jwt.getClaim("authorities");
                    }
                    if (rolesObj == null) {
                        rolesObj = jwt.getClaim("scope");
                    }

                    if (rolesObj instanceof List<?> rolesList) {
                        return rolesList.stream()
                                .map(Object::toString)
                                .toList();
                    } else if (rolesObj instanceof String rolesString) {

                        return List.of(rolesString.split("[\\s,]+"));
                    }
                } catch (Exception e) {
                    log.debug("Failed to extract roles from JWT: {}", e.getMessage());
                }
                return List.of();
            }
        };
    }
}
