package com.barakah.shared.service;

import com.barakah.shared.context.UserContext;
import lombok.extern.slf4j.Slf4j;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GrpcKeycloakAuthService {

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${keycloak.realm:barakah}")
    private String realm;

    @Value("${keycloak.resource:banking-services}")
    private String clientId;

    @Value("${keycloak.credentials.secret:your-client-secret}")
    private String clientSecret;

    private AuthzClient authzClient;

    private final Map<String, CacheEntry> tokenCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("secret", clientSecret);

        Configuration configuration = new Configuration(
                serverUrl, realm, clientId, credentials, null);

        this.authzClient = AuthzClient.create(configuration);
        log.info("gRPC Keycloak AuthService initialized");

        startCacheCleanup();
    }

    public AuthResult validateToken(String token) {

        CacheEntry cached = tokenCache.get(token);
        if (cached != null && !cached.isExpired()) {
            return AuthResult.success(cached.userContext);
        }

        try {

            TokenIntrospectionResponse accessToken = authzClient.protection().introspectRequestingPartyToken(token);

            if (accessToken != null && accessToken.isActive()) {
                UserContext userContext = extractUserContext(accessToken);

                tokenCache.put(token, new CacheEntry(userContext, System.currentTimeMillis() + 300000));

                return AuthResult.success(userContext);
            } else {
                return AuthResult.failure("Token is not active");
            }

        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return AuthResult.failure("Invalid token");
        }
    }

    private UserContext extractUserContext(TokenIntrospectionResponse tokenResponse) {
        Set<String> roles = Set.of("USER");

        try {

            Object realmAccess = tokenResponse.getOtherClaims().get("realm_access");
            if (realmAccess instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
                Object rolesObj = realmAccessMap.get("roles");
                if (rolesObj instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) rolesObj;
                    roles = Set.copyOf(rolesList);
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract roles from token introspection response: {}", e.getMessage());
        }

        return UserContext.builder()
                .userId(tokenResponse.getSubject())
                .username((String) tokenResponse.getOtherClaims().get("preferred_username"))
                .email((String) tokenResponse.getOtherClaims().get("email"))
                .firstName((String) tokenResponse.getOtherClaims().get("given_name"))
                .lastName((String) tokenResponse.getOtherClaims().get("family_name"))
                .roles(roles)
                .build();
    }

    private void startCacheCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000);
                    long now = System.currentTimeMillis();
                    tokenCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    public static class AuthResult {

        private final boolean success;
        private final String errorMessage;
        private final UserContext userContext;

        private AuthResult(boolean success, String errorMessage, UserContext userContext) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.userContext = userContext;
        }

        public static AuthResult success(UserContext userContext) {
            return new AuthResult(true, null, userContext);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, errorMessage, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public UserContext getUserContext() {
            return userContext;
        }
    }

    private static class CacheEntry {

        final UserContext userContext;
        final long expiryTime;

        CacheEntry(UserContext userContext, long expiryTime) {
            this.userContext = userContext;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
