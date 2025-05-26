package com.barakah.auth.service;

import com.barakah.auth.context.UserContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
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

    @PostConstruct
    public void init() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("secret", clientSecret);

        Configuration configuration = new Configuration(
                serverUrl, realm, clientId, credentials, null);

        this.authzClient = AuthzClient.create(configuration);
        this.restTemplate = new RestTemplate();
        log.info("Keycloak AuthzClient initialized");
    }

    public AuthenticationResult login(String username, String password) {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", username);
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

                return AuthenticationResult.success(
                        tokenResponse.getToken(),
                        tokenResponse.getRefreshToken(),
                        tokenResponse.getExpiresIn(),
                        userContext
                );
            } else {
                return AuthenticationResult.failure("Authentication failed: Invalid response");
            }

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return AuthenticationResult.failure("Invalid username or password");
            } else {
                log.error("Login HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                return AuthenticationResult.failure("Authentication failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return AuthenticationResult.failure("Authentication failed: " + e.getMessage());
        }
    }

    public RefreshResult refreshToken(String refreshToken) {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<AccessTokenResponse> response = restTemplate.postForEntity(
                    tokenUrl, request, AccessTokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AccessTokenResponse tokenResponse = response.getBody();

                return RefreshResult.success(
                        tokenResponse.getToken(),
                        tokenResponse.getRefreshToken(),
                        tokenResponse.getExpiresIn()
                );
            } else {
                return RefreshResult.failure("Token refresh failed: Invalid response");
            }

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return RefreshResult.failure("Invalid or expired refresh token");
            } else {
                log.error("Refresh token HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                return RefreshResult.failure("Token refresh failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage(), e);
            return RefreshResult.failure("Token refresh failed: " + e.getMessage());
        }
    }

    public LogoutResult logout(String refreshToken) {
        try {
            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return LogoutResult.success("Logged out successfully");
            } else {
                return LogoutResult.failure("Logout failed: Invalid response");
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
        try {
            boolean tokenRevoked = revokeToken(accessToken, "access_token");
            tokenRevoked = revokeToken(refreshToken, "refresh_token") && tokenRevoked;

            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(accessToken);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && tokenRevoked) {
                return LogoutResult.success("Logged out and token revoked successfully");
            } else if (tokenRevoked) {
                return LogoutResult.success("Token revoked successfully (logout had warnings)");
            } else {
                return LogoutResult.failure("Logout completed but token revocation failed");
            }

        } catch (Exception e) {
            log.warn("Logout error: {}", e.getMessage());

            try {
                revokeToken(accessToken, "access_token");
                revokeToken(refreshToken, "refresh_token");
                return LogoutResult.success("Token revoked (logout had errors)");
            } catch (Exception revokeError) {
                log.error("Both logout and token revocation failed: {}", revokeError.getMessage());
                return LogoutResult.failure("Logout failed and token revocation failed");
            }
        }
    }

    private boolean revokeToken(String token, String tokenTypeHint) {
        try {
            String revokeUrl = String.format("%s/realms/%s/protocol/openid-connect/revoke",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", token);
            body.add("token_type_hint", tokenTypeHint != null ? tokenTypeHint : "access_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(revokeUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Token revoked successfully");
                return true;
            } else {
                log.warn("Token revocation failed with status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Token revocation error: {}", e.getMessage());
            return false;
        }
    }

    public ValidationResult validateToken(String token, String typeHint) {
        try {
            String introspectionUrl = String.format("%s/realms/%s/protocol/openid-connect/token/introspect",
                    serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("token", token);
            body.add("token_type_hint", typeHint);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(introspectionUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenData = response.getBody();

                Boolean active = (Boolean) tokenData.get("active");
                if (active != null && active) {
                    System.out.println("Standard introspection claims: " + tokenData);
                    UserContext userContext = extractUserContextFromMap(tokenData);
                    return ValidationResult.success(userContext);
                } else {
                    return ValidationResult.failure("Token is not active");
                }
            } else {
                return ValidationResult.failure("Token introspection failed");
            }

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return ValidationResult.failure("Token validation failed: " + e.getMessage());
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

        return UserContext.builder()
                .userId((String) tokenData.get("sub"))
                .username((String) tokenData.get("preferred_username"))
                .email((String) tokenData.get("email"))
                .firstName((String) tokenData.get("given_name"))
                .lastName((String) tokenData.get("family_name"))
                .roles(roles)
                .build();
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

        public UserContext getUserContext() {
            return userContext;
        }

        public String getErrorMessage() {
            return errorMessage;
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
