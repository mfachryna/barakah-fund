package com.barakah.fund.auth_service.grpc;

import com.barakah.fund.proto.auth.*;
import com.barakah.fund.proto.shared.Role;

import io.grpc.stub.StreamObserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.barakah.fund.auth_service.service.AuthService;
import com.barakah.fund.auth_service.exception.AuthenticationException;
import com.barakah.fund.auth_service.exception.UserAlreadyExistsException;
import com.barakah.fund.auth_service.util.GrpcErrorUtil;
import com.barakah.fund.proto.shared.ErrorCode;
import com.google.rpc.Code;

import java.util.Map;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class AuthServiceGrpcImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    @Autowired
    public AuthServiceGrpcImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {

            Map<String, String> result = authService.registerUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getPhoneNumber()
            );

            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage(sanitizeMessage(result.get("message")))
                    .setUsername(request.getUsername())
                    .setUserId(result.get("userId"))
                    .setRole(Role.USER.toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (UserAlreadyExistsException e) {
            if (e.getMessage().contains("username")) {
                responseObserver.onError(GrpcErrorUtil.userAlreadyExists(request.getUsername()));
            } else {
                responseObserver.onError(GrpcErrorUtil.emailAlreadyExists(request.getEmail()));
            }
        } catch (Exception e) {
            responseObserver.onError(GrpcErrorUtil.internalError(e.getMessage()));
        }
    }

    @Override
    public void authenticate(AuthRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            Map<String, String> authResult = authService.authenticate(
                    request.getUsername(),
                    request.getPassword()
            );

            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Authentication successful")
                    .setUsername(request.getUsername())
                    .setAccessToken(authResult.get("access_token"))
                    .setRefreshToken(authResult.get("refresh_token"))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (AuthenticationException e) {
            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(GrpcErrorUtil.authenticationFailed());
        }
    }

    @Override
    public void refreshToken(TokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            Map<String, Object> tokenResult = authService.refreshToken(request.getRefreshToken());

            String newAccessToken = (String) tokenResult.get("access_token");
            String newRefreshToken = (String) tokenResult.get("refresh_token");

            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Token refreshed successfully")
                    .setAccessToken(newAccessToken)
                    .setRefreshToken(newRefreshToken)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(GrpcErrorUtil.createStatusException(
                    Code.UNAUTHENTICATED_VALUE,
                    ErrorCode.INVALID_REFRESH_TOKEN,
                    "Invalid or expired refresh token",
                    "auth.token",
                    null
            ));
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void validateToken(TokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            boolean isValid = authService.validateToken(request.getRefreshToken());

            if (!isValid) {
                responseObserver.onError(GrpcErrorUtil.invalidToken());
                return;
            }

            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Token is valid")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(GrpcErrorUtil.internalError(e.getMessage()));
        }
    }

    private String sanitizeMessage(String message) {
        return message != null ? message.replaceAll("[^\\p{Print}]", "") : "";
    }
}
