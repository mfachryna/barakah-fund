package com.barakah.gateway.controller;

import com.barakah.gateway.dto.auth.*;
import com.barakah.gateway.service.AuthGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication operations")
public class AuthController {

    private final AuthGatewayService authService;

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Request received: POST /api/v1/auth/login - Login attempt for username: {}",
                request.getUsername());

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            log.warn("Invalid username provided: {}", request.getUsername());
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            log.warn("Invalid password provided for username: {}", request.getUsername());
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            LoginResponseDto response = authService.login(request);
            log.info("Successfully authenticated user: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to authenticate user: {}", request.getUsername(), ex);
            throw ex;
        }
    }

    @PostMapping("/register")
    @Operation(summary = "User registration")
    public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Request received: POST /api/v1/auth/register - Registration attempt for username: {} and email: {}",
                request.getUsername(), request.getEmail());

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            log.warn("Invalid username provided for registration: {}", request.getUsername());
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            log.warn("Invalid email provided for registration: {}", request.getEmail());
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            log.warn("Invalid password provided for registration of username: {}", request.getUsername());
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (!request.getEmail().contains("@")) {
            log.warn("Invalid email format provided: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid email format");
        }

        try {
            LoginResponseDto response = authService.register(request);
            log.info("Successfully registered user: {} with email: {}", request.getUsername(), request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Failed to register user: {} with email: {}", request.getUsername(), request.getEmail(), ex);
            throw ex;
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token")
    public ResponseEntity<RefreshTokenResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Request received: POST /api/v1/auth/refresh - Refresh token attempt");

        if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            log.warn("Invalid refresh token provided");
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }

        try {
            RefreshTokenResponseDto response = authService.refreshToken(request);
            log.info("Successfully refreshed token");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to refresh token", ex);
            throw ex;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout")
    public ResponseEntity<LogoutResponseDto> logout(@Valid @RequestBody LogoutRequestDto request) {
        log.info("Request received: POST /api/v1/auth/logout - Logout attempt");

        if (request.getAccessToken() == null || request.getAccessToken().trim().isEmpty()) {
            log.warn("Invalid access token provided for logout");
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }

        try {
            LogoutResponseDto response = authService.logout(request);
            log.info("Successfully logged out user");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to logout user", ex);
            throw ex;
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate token")
    public ResponseEntity<ValidateTokenResponseDto> validateToken(@Valid @RequestBody ValidateTokenRequestDto request) {
        log.info("Request received: POST /api/v1/auth/validate - Token validation attempt");

        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            log.warn("Invalid token provided for validation");
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            ValidateTokenResponseDto response = authService.validateToken(request);
            log.info("Token validation completed - Valid: {}", response.getValid());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to validate token", ex);
            throw ex;
        }
    }
}