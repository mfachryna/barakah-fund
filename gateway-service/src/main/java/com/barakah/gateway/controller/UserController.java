package com.barakah.gateway.controller;

import com.barakah.gateway.dto.user.*;
import com.barakah.gateway.service.UserGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management operations")
public class UserController {

    private final UserGatewayService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseEntity<UserResponseDto> getCurrentUser() {
        log.info("Request received: GET /api/v1/users/me - Get current user");

        try {
            UserResponseDto user = userService.getCurrentUser();
            log.info("Successfully retrieved current user: {}", user.getUserId());
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            log.error("Failed to get current user", ex);
            throw ex;
        }
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.sub")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable String userId) {
        log.info("Request received: GET /api/v1/users/{} - Get user by ID", userId);

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Invalid user ID provided: {}", userId);
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            UserResponseDto user = userService.getUserById(userId);
            log.info("Successfully retrieved user: {} ({})", user.getUserId(), user.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            log.error("Failed to get user with ID: {}", userId, ex);
            throw ex;
        }
    }

    @GetMapping
    @Operation(summary = "List users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> listUsers(Pageable pageable) {
        log.info("Request received: GET /api/v1/users - List users (page: {}, size: {})",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<UserResponseDto> users = userService.listUsers(pageable);
            log.info("Successfully retrieved {} users (page {} of {})",
                    users.getNumberOfElements(),
                    users.getNumber() + 1,
                    users.getTotalPages());
            return ResponseEntity.ok(users);
        } catch (Exception ex) {
            log.error("Failed to list users", ex);
            throw ex;
        }
    }

    @PostMapping
    @Operation(summary = "Create user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        log.info("Request received: POST /api/v1/users - Create user with username: {}",
                request.getUsername());

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            log.warn("Invalid username provided: {}", request.getUsername());
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            log.warn("Invalid email provided: {}", request.getEmail());
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        try {
            UserResponseDto user = userService.createUser(request);
            log.info("Successfully created user: {} with ID: {}", user.getUsername(), user.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (Exception ex) {
            log.error("Failed to create user with username: {}", request.getUsername(), ex);
            throw ex;
        }
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.sub")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequestDto request) {

        log.info("Request received: PUT /api/v1/users/{} - Update user", userId);

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Invalid user ID provided for update: {}", userId);
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            UserResponseDto user = userService.updateUser(userId, request);
            log.info("Successfully updated user: {} ({})", user.getUserId(), user.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            log.error("Failed to update user with ID: {}", userId, ex);
            throw ex;
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Request received: DELETE /api/v1/users/{} - Delete user", userId);

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Invalid user ID provided for deletion: {}", userId);
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            userService.deleteUser(userId);
            log.info("Successfully deleted user with ID: {}", userId);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Failed to delete user with ID: {}", userId, ex);
            throw ex;
        }
    }
}
