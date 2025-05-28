package com.barakah.user.grpc;

import com.barakah.auth.proto.v1.*;
import com.barakah.user.proto.v1.*;
import com.barakah.shared.service.KeycloakAuthService;
import com.barakah.user.service.UserService;
import com.barakah.user.dto.UserResponse;
import net.devh.boot.grpc.server.service.GrpcService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final KeycloakAuthService keycloakAuthService;
    private final UserService userService;

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        log.info("Login attempt for user: {}", request.getUsername());

        if (request.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        KeycloakAuthService.AuthenticationResult authResult = keycloakAuthService.login(
                request.getUsername(), request.getPassword());

        if (!authResult.isSuccess()) {

            throw new SecurityException(authResult.getErrorMessage());
        }

        UserResponse user = userService.getUserByUsername(request.getUsername());
        
        try {
            userService.updateLastLoginByKeycloakId(user.getKeycloakId());
        } catch (Exception e) {
            log.warn("Could not update last login for user {}: {}", request.getUsername(), e.getMessage());
        }

        User.Builder userBuilder = User.newBuilder();
        if (authResult.getUserContext() != null) {
            var userContext = authResult.getUserContext();
            userBuilder
                    .setUserId(user.getUserId())
                    .setUsername(user.getUsername())
                    .setEmail(user.getEmail() != null ? user.getEmail() : "")
                    .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                    .setLastName(user.getLastName() != null ? user.getLastName() : "")
                    .setRole(userContext.getRoles().contains("ADMIN") ? UserRole.ADMIN : UserRole.USER)
                    .setStatus(user.getStatus().toString().equals("ACTIVE") ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        }

        LoginResponse response = LoginResponse.newBuilder()
                .setSuccess(true)
                .setAccessToken(authResult.getAccessToken())
                .setRefreshToken(authResult.getRefreshToken())
                .setExpiresIn(authResult.getExpiresIn())
                .setUserInfo(userBuilder.build())
                .setMessage("Login successful")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("User logged in successfully: {}", request.getUsername());
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        if (request.getToken().isEmpty()) {
            throw new IllegalArgumentException("Token is required");
        }

        KeycloakAuthService.ValidationResult validationResult = keycloakAuthService.validateToken(
                request.getToken(), "access_token");

        if (!validationResult.isValid()) {

            throw new SecurityException(validationResult.getErrorMessage());
        }

        var userContext = validationResult.getUserContext();
        User userInfo = User.newBuilder()
                .setUserId(userContext.getUserId())
                .setUsername(userContext.getUsername())
                .setEmail(userContext.getEmail() != null ? userContext.getEmail() : "")
                .setFirstName(userContext.getFirstName() != null ? userContext.getFirstName() : "")
                .setLastName(userContext.getLastName() != null ? userContext.getLastName() : "")
                .setRole(userContext.getRoles().contains("ADMIN") ? UserRole.ADMIN : UserRole.USER)
                .setStatus(UserStatus.ACTIVE)
                .build();

        ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserInfo(userInfo)
                .setMessage("Token is valid")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
        if (request.getRefreshToken().isEmpty()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        KeycloakAuthService.RefreshResult refreshResult = keycloakAuthService.refreshToken(request.getRefreshToken());

        if (!refreshResult.isSuccess()) {

            throw new SecurityException(refreshResult.getErrorMessage());
        }

        RefreshTokenResponse response = RefreshTokenResponse.newBuilder()
                .setSuccess(true)
                .setAccessToken(refreshResult.getAccessToken())
                .setRefreshToken(refreshResult.getRefreshToken())
                .setExpiresIn(refreshResult.getExpiresIn())
                .setMessage("Token refreshed successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        if (request.getAccessToken().isEmpty()) {
            throw new IllegalArgumentException("Access token is required");
        }

        KeycloakAuthService.LogoutResult logoutResult = keycloakAuthService.logoutWithAccessToken(
                request.getAccessToken(), request.getRefreshToken());

        if (!logoutResult.isSuccess()) {
            throw new RuntimeException("Logout failed: " + logoutResult.getMessage());
        }

        LogoutResponse response = LogoutResponse.newBuilder()
                .setSuccess(true)
                .setMessage(logoutResult.getMessage())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("User logged out successfully");
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        log.info("Registration attempt for user: {}", request.getUsername());

        validateRegistrationRequest(request);

        Optional<com.barakah.user.entity.User> existingUser = userService.getUserByUsernameOptional(request.getUsername());
        if (existingUser.isPresent()) {
            throw new IllegalStateException("Username already exists");
        }

        existingUser = userService.getUserByEmailOptional(request.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalStateException("Email already exists");
        }

        UserResponse user = userService.createUser(
                CreateUserRequest.newBuilder()
                        .setUsername(request.getUsername())
                        .setEmail(request.getEmail())
                        .setFirstName(request.getFirstName())
                        .setLastName(request.getLastName())
                        .setPhoneNumber(request.getPhoneNumber())
                        .setPassword(request.getPassword())
                        .setAddress(request.getAddress())
                        .setDateOfBirth(request.getDateOfBirth())
                        .setRole(UserRole.USER)
                        .build()
        );

        if (user.getUserId() == null) {
            throw new RuntimeException("Failed to create user: Database error");
        }

        User userInfo = buildUserInfo(user);

        RegisterResponse.Builder responseBuilder = RegisterResponse.newBuilder()
                .setSuccess(true)
                .setUserInfo(userInfo)
                .setMessage("Registration successful");

        if (shouldAutoLoginAfterRegistration()) {
            try {
                KeycloakAuthService.AuthenticationResult authResult = keycloakAuthService.login(
                        request.getUsername(), request.getPassword());

                if (authResult.isSuccess()) {
                    responseBuilder
                            .setAccessToken(authResult.getAccessToken())
                            .setRefreshToken(authResult.getRefreshToken())
                            .setExpiresIn(authResult.getExpiresIn());
                }
            } catch (Exception e) {
                log.warn("Auto-login failed after registration for user {}: {}", request.getUsername(), e.getMessage());
    
            }
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();

        log.info("User registered successfully: {}", request.getUsername());
    }

    private void validateRegistrationRequest(RegisterRequest request) {
        if (request.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.getFirstName().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (request.getLastName().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private User buildUserInfo(UserResponse user) {
        return User.newBuilder()
                .setUserId(user.getUserId())
                .setUsername(user.getUsername())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                .setAddress(user.getAddress() != null ? user.getAddress() : "")
                .setDateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "")
                .setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "")
                .setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "")
                .setRole(UserRole.USER)
                .setStatus(UserStatus.ACTIVE)
                .build();
    }

    private boolean shouldAutoLoginAfterRegistration() {
        return true;
    }
}
