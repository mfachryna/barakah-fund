package com.barakah.fund.auth_service.service;

import com.barakah.fund.auth_service.exception.AuthenticationException;
import com.barakah.fund.auth_service.service.abstracts.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Autowired
    public AuthService(
            UserService userService,
            AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    public Map<String, String> registerUser(String username, String email, String password, String phoneNumber) {
        logger.info("Registering user: {}", username);
        try {
            Map<String, String> result = userService.registerUser(username, email, password, phoneNumber);
            logger.info("Registration successful for: {}", username);
            return result;
        } catch (Exception e) {
            logger.error("Registration failed: {}", e.getMessage());
            throw e;
        }
    }

    public Map<String, String> authenticate(String username, String password) {
        logger.info("Authentication attempt: {}", username);

        try {
            Map<String, String> authResult = authenticationService.authenticate(username, password);

            if ("success".equals(authResult.get("status"))) {
                logger.info("Authentication successful: {}", username);
            } else {
                logger.warn("Authentication failed: {}", username);
            }

            return authResult;
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
            throw new AuthenticationException("Authentication error");
        }
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        logger.info("Token refresh requested");

        if (refreshToken == null || refreshToken.isEmpty()) {
            logger.warn("Empty refresh token");
            throw new IllegalArgumentException("Refresh token cannot be empty");
        }

        try {
            Map<String, Object> result = authenticationService.refreshToken(refreshToken);
            
            if (result.containsKey("access_token")) {
                logger.info("Token refresh successful");
            } else {
                logger.warn("Token refresh failed");
            }

            return result;
        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Empty token validation attempt");
            throw new IllegalArgumentException("Token cannot be empty");
        }

        try {
            boolean isValid = authenticationService.validateToken(token);
            logger.info("Token validation: {}", isValid ? "valid" : "invalid");
            return isValid;
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            throw e;
        }
    }
}
