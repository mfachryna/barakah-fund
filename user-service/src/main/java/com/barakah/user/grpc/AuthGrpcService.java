package com.barakah.user.grpc;

import com.barakah.auth.proto.v1.*;
import com.barakah.user.proto.v1.*;
import com.barakah.common.proto.v1.*;
import com.barakah.auth.service.KeycloakAuthService;
import com.barakah.user.service.UserService;
import com.barakah.user.dto.UserResponse;
import net.devh.boot.grpc.server.service.GrpcService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AuthGrpcService.class);

    private final KeycloakAuthService keycloakAuthService;

    private final UserService userService;

    public AuthGrpcService(KeycloakAuthService keycloakAuthService, UserService userService) {
        this.keycloakAuthService = keycloakAuthService;
        this.userService = userService;
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            log.info("Login attempt for user: {}", request.getUsername());

            KeycloakAuthService.AuthenticationResult authResult = keycloakAuthService.login(
                    request.getUsername(), request.getPassword());

            if (authResult.isSuccess()) {

                UserResponse user = null;
                try {
                    user = userService.getUserByUsername(request.getUsername());
                    userService.updateLastLogin(user.getUserId());
                } catch (Exception e) {
                    log.warn("Could not update last login for user {}: {}", request.getUsername(), e.getMessage());
                }

                UserInfo.Builder userInfoBuilder = UserInfo.newBuilder();
                if (authResult.getUserContext() != null) {
                    var userContext = authResult.getUserContext();
                    userInfoBuilder
                            .setUserId(user.getUserId())
                            .setUsername(user.getUsername())
                            .setEmail(user.getEmail() != null ? user.getEmail() : "")
                            .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                            .setLastName(user.getLastName() != null ? user.getLastName() : "")
                            .setRole(userContext.getRoles().contains("ADMIN") ? "ADMIN" : "USER")
                            .setStatus("ACTIVE");
                }

                LoginResponse response = LoginResponse.newBuilder()
                        .setSuccess(true)
                        .setAccessToken(authResult.getAccessToken())
                        .setRefreshToken(authResult.getRefreshToken())
                        .setExpiresIn(authResult.getExpiresIn())
                        .setUserInfo(userInfoBuilder.build())
                        .setMessage("Login successful")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();

                log.info("User logged in successfully: {}", request.getUsername());

            } else {
                LoginResponse response = LoginResponse.newBuilder()
                        .setSuccess(false)
                        .setAccessToken("")
                        .setMessage(authResult.getErrorMessage())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Login failed for user {}: {}", request.getUsername(), e.getMessage());
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("Login failed")
                    .asRuntimeException());
        }
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        try {
            KeycloakAuthService.ValidationResult validationResult = keycloakAuthService.validateToken(request.getToken(), "access_token");

            ValidateTokenResponse.Builder responseBuilder = ValidateTokenResponse.newBuilder()
                    .setValid(validationResult.isValid());

            if (validationResult.isValid()) {
                var userContext = validationResult.getUserContext();

                UserInfo userInfo = UserInfo.newBuilder()
                        .setUserId(userContext.getUserId())
                        .setUsername(userContext.getUsername())
                        .setEmail(userContext.getEmail() != null ? userContext.getEmail() : "")
                        .setFirstName(userContext.getFirstName() != null ? userContext.getFirstName() : "")
                        .setLastName(userContext.getLastName() != null ? userContext.getLastName() : "")
                        .setRole(userContext.getRoles().contains("ADMIN") ? "ADMIN" : "USER")
                        .setStatus("ACTIVE")
                        .build();

                responseBuilder.setUserInfo(userInfo).setMessage("Token is valid");
            } else {
                responseObserver.onError(Status.UNAUTHENTICATED
                        .withDescription(validationResult.getErrorMessage())
                        .asException());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Token validation error: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Token validation failed")
                    .asException());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
        try {
            KeycloakAuthService.RefreshResult refreshResult = keycloakAuthService.refreshToken(request.getRefreshToken());

            RefreshTokenResponse.Builder responseBuilder = RefreshTokenResponse.newBuilder()
                    .setSuccess(refreshResult.isSuccess());

            if (refreshResult.isSuccess()) {
                responseBuilder
                        .setAccessToken(refreshResult.getAccessToken())
                        .setRefreshToken(refreshResult.getRefreshToken())
                        .setExpiresIn(refreshResult.getExpiresIn())
                        .setMessage("Token refreshed successfully");
            } else {
                responseBuilder.setMessage(refreshResult.getErrorMessage());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Token refresh failed")
                    .asRuntimeException());
        }
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            KeycloakAuthService.LogoutResult logoutResult = keycloakAuthService.logoutWithAccessToken(request.getAccessToken(), request.getRefreshToken());

            LogoutResponse response = LogoutResponse.newBuilder()
                    .setSuccess(logoutResult.isSuccess())
                    .setMessage(logoutResult.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("User logged out successfully");

        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Logout failed")
                    .asRuntimeException());
        }
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            log.info("Registration attempt for user: {}", request.getUsername());

            try {
                UserResponse existingUser = userService.getUserByUsername(request.getUsername());
                if (existingUser != null) {
                    RegisterResponse response = RegisterResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Username already exists")
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }
            } catch (Exception e) {

            }

            try {
                UserResponse existingUser = userService.getUserByEmail(request.getEmail());
                if (existingUser != null) {
                    RegisterResponse response = RegisterResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Email already exists")
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }
            } catch (Exception e) {

            }

            UserResponse user = userService.createUser(
                    CreateUserRequest.newBuilder()
                            .setUsername(request.getUsername())
                            .setEmail(request.getEmail())
                            .setFirstName(request.getFirstName())
                            .setLastName(request.getLastName())
                            .setPhoneNumber(request.getPhoneNumber())
                            .setPhoneNumber(request.getPhoneNumber())
                            .setPassword(request.getPassword())
                            .setRole(UserRole.USER_ROLE_USER)
                            .build()
            );

            try {
                if (user.getUserId() != null) {

                    UserInfo userInfo = UserInfo.newBuilder()
                            .setUserId(user.getUserId())
                            .setUsername(user.getUsername())
                            .setEmail(user.getEmail())
                            .setFirstName(user.getFirstName())
                            .setLastName(user.getLastName())
                            .setRole("USER")
                            .setStatus("ACTIVE")
                            .build();

                    RegisterResponse.Builder responseBuilder = RegisterResponse.newBuilder()
                            .setSuccess(true)
                            .setUserInfo(userInfo)
                            .setMessage("Registration successful");

                    if (shouldAutoLoginAfterRegistration()) {
                        KeycloakAuthService.AuthenticationResult authResult = keycloakAuthService.login(
                                request.getUsername(), request.getPassword());

                        if (authResult.isSuccess()) {
                            responseBuilder
                                    .setAccessToken(authResult.getAccessToken())
                                    .setRefreshToken(authResult.getRefreshToken())
                                    .setExpiresIn(authResult.getExpiresIn());
                        }
                    }

                    responseObserver.onNext(responseBuilder.build());
                    responseObserver.onCompleted();

                    log.info("User registered successfully: {}", request.getUsername());

                } else {
                    RegisterResponse response = RegisterResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Registration failed: User creation in database failed")
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            } catch (Exception e) {

                userService.deleteUser(user.getUserId());

                log.error("Failed to create user in database after Keycloak creation: {}", e.getMessage());
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Registration failed: Database error")
                        .asRuntimeException());
            }

        } catch (Exception e) {
            log.error("Registration failed for user {}: {}", request.getUsername(), e.getMessage());
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Registration failed")
                    .asRuntimeException());
        }
    }

    private boolean shouldAutoLoginAfterRegistration() {

        return true;
    }
}
