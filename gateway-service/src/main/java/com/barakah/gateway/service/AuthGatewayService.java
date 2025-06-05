package com.barakah.gateway.service;

import com.barakah.auth.proto.v1.*;
import com.barakah.gateway.dto.auth.*;
import com.barakah.gateway.mapper.AuthMapper;
import com.barakah.shared.util.GrpcErrorHandler;
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

    private final AuthMapper authMapper;

    @GrpcClient("user-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    public LoginResponseDto login(LoginRequestDto requestDto) {
        LoginRequest grpcRequest = authMapper.toGrpcLoginRequest(requestDto);
        LoginResponse grpcResponse = loginGrpc(grpcRequest);
        return authMapper.toLoginDto(grpcResponse);
    }

    public LoginResponseDto register(RegisterRequestDto requestDto) {
        RegisterRequest grpcRequest = authMapper.toGrpcRegisterRequest(requestDto);
        RegisterResponse grpcResponse = registerGrpc(grpcRequest);
        return authMapper.toRegisterDto(grpcResponse);
    }

    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto requestDto) {
        RefreshTokenRequest grpcRequest = authMapper.toGrpcRefreshRequest(requestDto);
        RefreshTokenResponse grpcResponse = refreshTokenGrpc(grpcRequest);
        return authMapper.toRefreshDto(grpcResponse);
    }

    public LogoutResponseDto logout(LogoutRequestDto requestDto) {
        LogoutRequest grpcRequest = authMapper.toGrpcLogoutRequest(requestDto);
        LogoutResponse grpcResponse = logoutGrpc(grpcRequest);
        return authMapper.toLogoutDto(grpcResponse);
    }

    public ValidateTokenResponseDto validateToken(ValidateTokenRequestDto requestDto) {
        ValidateTokenRequest grpcRequest = authMapper.toGrpcValidateRequest(requestDto);
        ValidateTokenResponse grpcResponse = validateTokenGrpc(grpcRequest);
        return authMapper.toValidateDto(grpcResponse);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackLogin")
    @Retry(name = "user-service")
    public LoginResponse loginGrpc(LoginRequest request) {
        return authServiceStub.login(request);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackRegister")
    @Retry(name = "user-service")
    public RegisterResponse registerGrpc(RegisterRequest request) {
        return authServiceStub.register(request);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackRefreshToken")
    @Retry(name = "user-service")
    public RefreshTokenResponse refreshTokenGrpc(RefreshTokenRequest request) {
        return authServiceStub.refreshToken(request);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackLogout")
    @Retry(name = "user-service")
    public LogoutResponse logoutGrpc(LogoutRequest request) {
        return authServiceStub.logout(request);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackValidateToken")
    @Retry(name = "user-service")
    public ValidateTokenResponse validateTokenGrpc(ValidateTokenRequest request) {
        return authServiceStub.validateToken(request);
    }

    public LoginResponse fallbackLogin(LoginRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Authentication",
                "login user: " + request.getUsername(),
                "Authentication service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }

    public RegisterResponse fallbackRegister(RegisterRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Authentication",
                "register user: " + request.getUsername(),
                "Authentication service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }

    public RefreshTokenResponse fallbackRefreshToken(RefreshTokenRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Authentication",
                "refresh token",
                "Authentication service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }

    public LogoutResponse fallbackLogout(LogoutRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Authentication",
                "logout user",
                "Authentication service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }

    public ValidateTokenResponse fallbackValidateToken(ValidateTokenRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Authentication",
                "validate token",
                "Authentication service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }
}
