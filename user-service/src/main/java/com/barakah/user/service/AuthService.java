package com.barakah.user.service;

import com.barakah.auth.service.KeycloakAuthService;
import com.barakah.user.dto.UserResponse;
import com.barakah.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final KeycloakAuthService keycloakAuthService;
    private final UserService userService;
    private final UserRepository userRepository;

    public AuthService(KeycloakAuthService keycloakAuthService,
            UserService userService,
            UserRepository userRepository) {
        this.keycloakAuthService = keycloakAuthService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public Map<String, Object> login(String username, String password) {
        try {

            KeycloakAuthService.AuthenticationResult tokenResponse = keycloakAuthService.login(username, password);

            Map<String, Object> response = new HashMap<>();
            if (!(Boolean) tokenResponse.isSuccess()) {
                response.put("success", false);
                response.put("message", "Credential errors: " + tokenResponse.getErrorMessage());
                return null;
            }

            UserResponse user = userService.getUserByUsername(username);

            userService.updateLastLogin(user.getUserId());

            response.put("success", true);
            response.put("access_token", tokenResponse.getAccessToken());
            response.put("refresh_token", tokenResponse.getRefreshToken());
            response.put("expires_in", tokenResponse.getExpiresIn());
            response.put("user_info", createUserInfo(user));
            response.put("message", "Login successful");

            log.info("User logged in successfully: {}", username);
            return response;

        } catch (Exception e) {
            log.error("Login failed for user {}: {}", username, e.getMessage());
            return Map.of(
                    "success", false,
                    "message", "Login failed: " + e.getMessage()
            );
        }
    }

    public KeycloakAuthService.ValidationResult validateToken(String token) {
        try {
            KeycloakAuthService.ValidationResult validationResult = keycloakAuthService.validateToken(token, "access_token");

            if (!(Boolean) validationResult.isValid()) {
                return validationResult;
            }

            String username = (String) validationResult.getUserContext().getUsername();
            UserResponse user = userService.getUserByUsername(username);

            return validationResult;

        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return KeycloakAuthService.ValidationResult.failure("Token validation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        try {

            KeycloakAuthService.RefreshResult refreshResult = keycloakAuthService.refreshToken(refreshToken);
            if (!(Boolean) refreshResult.isSuccess()) {
                return Map.of(
                        "success", false,
                        "message", "Token refresh failed: " + refreshResult.getErrorMessage()
                );
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("access_token", refreshResult.getAccessToken());
            response.put("refresh_token", refreshResult.getRefreshToken());
            response.put("expires_in", refreshResult.getExpiresIn());
            response.put("message", "Token refreshed successfully");
            log.info("Token refreshed successfully");

            return response;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "message", "Token refresh failed"
            );
        }
    }

    public Map<String, Object> logout(String token) {
        try {
            keycloakAuthService.logout(token);
            return Map.of(
                    "success", true,
                    "message", "Logged out successfully"
            );
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return Map.of(
                    "success", true,
                    "message", "Logged out"
            );
        }
    }

    private Map<String, Object> createUserInfo(UserResponse user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user_id", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("first_name", user.getFirstName());
        userInfo.put("last_name", user.getLastName());
        userInfo.put("role", user.getRole());
        userInfo.put("status", user.getStatus());
        return userInfo;
    }
}
