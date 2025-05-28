package com.barakah.shared.service;

import com.barakah.shared.context.UserContext;
import com.barakah.shared.exception.AuthExceptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class KeycloakAuthService {

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${keycloak.realm:barakah}")
    private String realm;

    @Value("${keycloak.resource:banking-services}")
    private String clientId;

    @Value("${keycloak.credentials.secret:your-client-secret}")
    private String clientSecret;

    private AuthzClient authzClient;
    private RestTemplate restTemplate;

    public KeycloakAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("KeycloakAuthService created with RestTemplate: {}",
                restTemplate != null ? restTemplate.getClass().getSimpleName() : "null");
    }

    @PostConstruct
    public void init() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("secret", clientSecret);

        Configuration configuration = new Configuration(
                serverUrl, realm, clientId, credentials, null);

        this.authzClient = AuthzClient.create(configuration);
        this.restTemplate = new RestTemplate();
        log.info("Keycloak AuthzClient initialized for realm: {} at: {}", realm, serverUrl);
    }

    public AuthenticationResult login(String username, String password) {
        log.debug("Attempting login for username: {}", username);

        if (username == null || username.trim().isEmpty()) {
            throw new AuthExceptions.InvalidCredentialsException("Username cannot be empty");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new AuthExceptions.InvalidCredentialsException("Password cannot be empty");
        }

        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", username.trim());
            body.add("password", password);
            body.add("grant_type", OAuth2Constants.PASSWORD);
            body.add("scope", "openid profile email");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<AccessTokenResponse> response = restTemplate.postForEntity(
                    tokenUrl, request, AccessTokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AccessTokenResponse tokenResponse = response.getBody();

                ValidationResult validation = validateToken(tokenResponse.getToken(), "access_token");
                UserContext userContext = validation.isValid() ? validation.getUserContext() : null;

                if (!validation.isValid()) {
                    throw new AuthExceptions.InvalidTokenException("Received invalid token from Keycloak");
                }

                log.info("Login successful for user: {}", username);
                return AuthenticationResult.success(
                        tokenResponse.getToken(),
                        tokenResponse.getRefreshToken(),
                        tokenResponse.getExpiresIn(),
                        userContext
                );
            } else {
                throw new AuthExceptions.InvalidCredentialsException("Authentication failed: Invalid response from server");
            }

        } catch (AuthExceptions.InvalidCredentialsException | AuthExceptions.InvalidTokenException e) {

            throw e;

        } catch (HttpClientErrorException e) {
            log.warn("Login HTTP error for user {}: {} - {}", username, e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new AuthExceptions.InvalidCredentialsException("Invalid username or password");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody.contains("invalid_grant")) {
                    throw new AuthExceptions.InvalidCredentialsException("Invalid username or password");
                } else if (responseBody.contains("account_disabled")) {
                    throw new AuthExceptions.InvalidCredentialsException("Account is disabled");
                } else if (responseBody.contains("account_temporarily_disabled")) {
                    throw new AuthExceptions.InvalidCredentialsException("Account is temporarily locked");
                }
                throw new AuthExceptions.InvalidCredentialsException("Authentication failed: " + responseBody);
            } else {
                throw new RuntimeException("Authentication service error: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Unexpected login error for user {}: {}", username, e.getMessage(), e);
            throw new RuntimeException("Authentication service error: " + e.getMessage(), e);
        }
    }

    public RefreshResult refreshToken(String refreshToken) {
        log.debug("Attempting to refresh token");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new AuthExceptions.InvalidTokenException("Refresh token cannot be empty");
        }

        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken.trim());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<AccessTokenResponse> response = restTemplate.postForEntity(
                    tokenUrl, request, AccessTokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AccessTokenResponse tokenResponse = response.getBody();

                log.info("Token refresh successful");
                return RefreshResult.success(
                        tokenResponse.getToken(),
                        tokenResponse.getRefreshToken(),
                        tokenResponse.getExpiresIn()
                );
            } else {
                throw new AuthExceptions.TokenExpiredException();
            }

        } catch (AuthExceptions.TokenExpiredException e) {
            throw e;

        } catch (HttpClientErrorException e) {
            log.warn("Token refresh HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody.contains("invalid_grant") || responseBody.contains("Token is not active")) {
                    throw new AuthExceptions.TokenExpiredException();
                }
                throw new AuthExceptions.InvalidTokenException("Invalid refresh token");
            } else {
                throw new RuntimeException("Token refresh service error: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Unexpected token refresh error: {}", e.getMessage(), e);
            throw new RuntimeException("Token refresh service error: " + e.getMessage(), e);
        }
    }

    public LogoutResult logout(String refreshToken) {
        log.debug("Attempting logout");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.warn("Logout attempted with empty refresh token");
            return LogoutResult.success("Logged out (no token to revoke)");
        }

        try {
            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken.trim());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Logout successful");
                return LogoutResult.success("Logged out successfully");
            } else {
                log.warn("Logout returned non-success status: {}", response.getStatusCode());
                return LogoutResult.success("Logged out (with warnings)");
            }

        } catch (HttpClientErrorException e) {
            log.warn("Logout HTTP error (ignoring): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return LogoutResult.success("Logged out (with warnings)");
        } catch (Exception e) {
            log.warn("Logout error (ignoring): {}", e.getMessage());
            return LogoutResult.success("Logged out (with warnings)");
        }
    }

    public LogoutResult logoutWithAccessToken(String accessToken, String refreshToken) {
        log.debug("Attempting logout with access token");

        try {

            boolean accessTokenRevoked = false;
            boolean refreshTokenRevoked = false;

            if (accessToken != null && !accessToken.trim().isEmpty()) {
                accessTokenRevoked = revokeToken(accessToken, "access_token");
            }

            if (refreshToken != null && !refreshToken.trim().isEmpty()) {
                refreshTokenRevoked = revokeToken(refreshToken, "refresh_token");
            }

            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                headers.setBearerAuth(accessToken);
            }

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && (accessTokenRevoked || refreshTokenRevoked)) {
                log.info("Logout and token revocation successful");
                return LogoutResult.success("Logged out and tokens revoked successfully");
            } else if (accessTokenRevoked || refreshTokenRevoked) {
                log.info("Token revocation successful, logout had warnings");
                return LogoutResult.success("Token revoked successfully (logout had warnings)");
            } else {
                log.warn("Both logout and token revocation had issues");
                return LogoutResult.success("Logout completed with warnings");
            }

        } catch (Exception e) {
            log.warn("Logout error: {}", e.getMessage());

            try {
                boolean tokenRevoked = false;
                if (accessToken != null) {
                    tokenRevoked = revokeToken(accessToken, "access_token");
                }
                if (refreshToken != null) {
                    tokenRevoked = revokeToken(refreshToken, "refresh_token") || tokenRevoked;
                }

                if (tokenRevoked) {
                    return LogoutResult.success("Token revoked (logout had errors)");
                } else {
                    return LogoutResult.failure("Both logout and token revocation failed");
                }
            } catch (Exception revokeError) {
                log.error("Both logout and token revocation failed: {}", revokeError.getMessage());
                return LogoutResult.failure("Logout failed and token revocation failed");
            }
        }
    }

    private boolean revokeToken(String token, String tokenTypeHint) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String revokeUrl = String.format("%s/realms/%s/protocol/openid-connect/revoke",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", token.trim());
            body.add("token_type_hint", tokenTypeHint != null ? tokenTypeHint : "access_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(revokeUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Token revoked successfully: {}", tokenTypeHint);
                return true;
            } else {
                log.warn("Token revocation failed with status: {} for type: {}", response.getStatusCode(), tokenTypeHint);
                return false;
            }

        } catch (Exception e) {
            log.error("Token revocation error for type {}: {}", tokenTypeHint, e.getMessage());
            return false;
        }
    }

    public ValidationResult validateToken(String token, String typeHint) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthExceptions.InvalidTokenException("Token cannot be empty");
        }

        try {
            String introspectionUrl = String.format("%s/realms/%s/protocol/openid-connect/token/introspect",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", token.trim());
            body.add("token_type_hint", typeHint);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(introspectionUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenData = response.getBody();

                Boolean active = (Boolean) tokenData.get("active");
                if (active != null && active) {
                    log.debug("Token validation successful");
                    UserContext userContext = extractUserContextFromMap(tokenData);
                    return ValidationResult.success(userContext);
                } else {
                    log.debug("Token is not active");
                    throw new AuthExceptions.TokenExpiredException();
                }
            } else {
                throw new AuthExceptions.InvalidTokenException("Token introspection failed");
            }

        } catch (AuthExceptions.TokenExpiredException | AuthExceptions.InvalidTokenException e) {
            throw e;

        } catch (HttpClientErrorException e) {
            log.warn("Token validation HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new AuthExceptions.InvalidTokenException("Token validation failed: Unauthorized");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new AuthExceptions.InvalidTokenException("Token validation failed: Bad request");
            } else {
                throw new RuntimeException("Token validation service error: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Token validation service error: " + e.getMessage(), e);
        }
    }

    private UserContext extractUserContextFromMap(Map<String, Object> tokenData) {
        Set<String> roles = Set.of("USER");

        try {
            Object realmAccess = tokenData.get("realm_access");
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
            log.debug("Could not extract roles from token: {}", e.getMessage());
        }

        UserContext userContext = UserContext.builder()
                .userId((String) tokenData.get("sub"))
                .username((String) tokenData.get("preferred_username"))
                .email((String) tokenData.get("email"))
                .firstName((String) tokenData.get("given_name"))
                .lastName((String) tokenData.get("family_name"))
                .roles(roles)
                .build();

        log.debug("Extracted user context for: {}", userContext.getUsername());
        return userContext;
    }

    public static class ValidationResult {

        private final boolean valid;
        private final String errorMessage;
        private final UserContext userContext;

        private ValidationResult(boolean valid, String errorMessage, UserContext userContext) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.userContext = userContext;
        }

        public static ValidationResult success(UserContext userContext) {
            return new ValidationResult(true, null, userContext);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public UserContext getUserContext() {
            return userContext;
        }
    }

    @Getter
    public static class AuthenticationResult {

        private final boolean success;
        private final String accessToken;
        private final String refreshToken;
        private final long expiresIn;
        private final UserContext userContext;
        private final String errorMessage;

        private AuthenticationResult(boolean success, String accessToken, String refreshToken,
                long expiresIn, UserContext userContext, String errorMessage) {
            this.success = success;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.userContext = userContext;
            this.errorMessage = errorMessage;
        }

        public static AuthenticationResult success(String accessToken, String refreshToken,
                long expiresIn, UserContext userContext) {
            return new AuthenticationResult(true, accessToken, refreshToken, expiresIn, userContext, null);
        }

        public static AuthenticationResult failure(String errorMessage) {
            return new AuthenticationResult(false, null, null, 0, null, errorMessage);
        }
    }

    public static class RefreshResult {

        private final boolean success;
        private final String accessToken;
        private final String refreshToken;
        private final long expiresIn;
        private final String errorMessage;

        private RefreshResult(boolean success, String accessToken, String refreshToken,
                long expiresIn, String errorMessage) {
            this.success = success;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.errorMessage = errorMessage;
        }

        public static RefreshResult success(String accessToken, String refreshToken, long expiresIn) {
            return new RefreshResult(true, accessToken, refreshToken, expiresIn, null);
        }

        public static RefreshResult failure(String errorMessage) {
            return new RefreshResult(false, null, null, 0, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class LogoutResult {

        private final boolean success;
        private final String message;

        private LogoutResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static LogoutResult success(String message) {
            return new LogoutResult(true, message);
        }

        public static LogoutResult failure(String message) {
            return new LogoutResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
