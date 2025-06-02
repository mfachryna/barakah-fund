package com.barakah.gateway.service;

import com.barakah.auth.proto.v1.*;
import com.barakah.gateway.dto.auth.*;
import com.barakah.gateway.mapper.AuthMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthGatewayService {

    @GrpcClient("user-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    private final AuthMapper authMapper;

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackLogin")
    @Retry(name = "user-service")
    public LoginResponseDto login(LoginRequestDto request) {
        try {
            LoginRequest grpcRequest = authMapper.toGrpcLoginRequest(request);
            LoginResponse response = authServiceStub.login(grpcRequest);
            return authMapper.toLoginDto(response);
        } catch (Exception e) {
            log.error("Failed to login user: {}", request.getUsername(), e);
            throw new RuntimeException("Failed to login", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackLogin")
    @Retry(name = "user-service")
    public LoginResponse loginGrpc(LoginRequest request) {
        try {
            return authServiceStub.login(request);
        } catch (Exception e) {
            log.error("Failed to login user: {}", request.getUsername(), e);
            throw new RuntimeException("Failed to login", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackRegister")
    @Retry(name = "user-service")
    public RegisterResponseDto register(RegisterRequestDto request) {
        try {
            RegisterRequest grpcRequest = authMapper.toGrpcRegisterRequest(request);
            RegisterResponse response = authServiceStub.register(grpcRequest);
            return authMapper.toRegisterDto(response);
        } catch (Exception e) {
            log.error("Failed to register user: {}", request.getUsername(), e);
            throw new RuntimeException("Failed to register", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackRegister")
    @Retry(name = "user-service")
    public RegisterResponse register(RegisterRequest request) {
        try {
            return authServiceStub.register(request);
        } catch (Exception e) {
            log.error("Failed to register user: {}", request.getUsername(), e);
            throw new RuntimeException("Failed to register", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackRefreshToken")
    @Retry(name = "user-service")
    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        try {
            RefreshTokenRequest grpcRequest = authMapper.toGrpcRefreshRequest(request);
            RefreshTokenResponse response = authServiceStub.refreshToken(grpcRequest);
            return authMapper.toRefreshDto(response);
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new RuntimeException("Failed to refresh token", e);
        }
    }
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackRefreshToken")
    @Retry(name = "user-service")
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        try {

            return authServiceStub.refreshToken(request);
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackLogout")
    @Retry(name = "user-service")
    public LogoutResponseDto logout(LogoutRequestDto request) {
        try {
            LogoutRequest grpcRequest = authMapper.toGrpcLogoutRequest(request);
            LogoutResponse response = authServiceStub.logout(grpcRequest);
            return authMapper.toLogoutDto(response);
        } catch (Exception e) {
            log.error("Failed to logout", e);
            throw new RuntimeException("Failed to logout", e);
        }
    }
  @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackLogout")
    @Retry(name = "user-service")
    public LogoutResponse logoutGrpc(LogoutRequest request) {
        try {
            return authServiceStub.logout(request);
        } catch (Exception e) {
            log.error("Failed to logout", e);
            throw new RuntimeException("Failed to logout", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackValidateToken")
    @Retry(name = "user-service")
    public ValidateTokenResponseDto validateToken(ValidateTokenRequestDto request) {
        try {
            ValidateTokenRequest grpcRequest = authMapper.toGrpcValidateRequest(request);
            ValidateTokenResponse response = authServiceStub.validateToken(grpcRequest);
            return authMapper.toValidateDto(response);
        } catch (Exception e) {
            log.error("Failed to validate token", e);
            throw new RuntimeException("Failed to validate token", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackValidateToken")
    @Retry(name = "user-service")
    public ValidateTokenResponse validateTokenGrpc(ValidateTokenRequest request) {
        try {
            return authServiceStub.validateToken(request);
        } catch (Exception e) {
            log.error("Failed to validate token", e);
            throw new RuntimeException("Failed to validate token", e);
        }
    }

    // Fallback methods
    public LoginResponseDto fallbackLogin(LoginRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to login user: {}", request.getUsername(), ex);
        throw new RuntimeException("Authentication service is currently unavailable", ex);
    }

    public RegisterResponseDto fallbackRegister(RegisterRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to register user: {}", request.getUsername(), ex);
        throw new RuntimeException("Authentication service is currently unavailable", ex);
    }

    public RefreshTokenResponseDto fallbackRefreshToken(RefreshTokenRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to refresh token", ex);
        throw new RuntimeException("Authentication service is currently unavailable", ex);
    }

    public LogoutResponseDto fallbackLogout(LogoutRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to logout", ex);
        throw new RuntimeException("Authentication service is currently unavailable", ex);
    }

    public ValidateTokenResponseDto fallbackValidateToken(ValidateTokenRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to validate token", ex);
        throw new RuntimeException("Authentication service is currently unavailable", ex);
    }
}