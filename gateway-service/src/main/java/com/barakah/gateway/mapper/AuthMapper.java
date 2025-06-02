package com.barakah.gateway.mapper;

import com.barakah.auth.proto.v1.*;
import com.barakah.gateway.dto.auth.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AuthMapper {

    public LoginRequest toGrpcLoginRequest(LoginRequestDto dto) {
        return LoginRequest.newBuilder()
                .setUsername(dto.getUsername())
                .setPassword(dto.getPassword())
                .build();
    }

    public LoginResponseDto toLoginDto(LoginResponse response) {
        return LoginResponseDto.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .expiresIn(response.getExpiresIn())
                .userId(response.getUserInfo().getUserId())
                .username(response.getUserInfo().getUsername())
                .email(response.getUserInfo().getEmail())
                .build();
    }

    public RegisterRequest toGrpcRegisterRequest(RegisterRequestDto dto) {
        RegisterRequest.Builder builder = RegisterRequest.newBuilder()
                .setUsername(dto.getUsername())
                .setEmail(dto.getEmail())
                .setFirstName(dto.getFirstName())
                .setLastName(dto.getLastName())
                .setPassword(dto.getPassword());

        if (dto.getPhoneNumber() != null) {
            builder.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getDateOfBirth() != null) {
            builder.setDateOfBirth(dto.getDateOfBirth().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (dto.getAddress() != null) {
            builder.setAddress(dto.getAddress());
        }

        return builder.build();
    }

    public RegisterResponseDto toRegisterDto(RegisterResponse response) {
        return RegisterResponseDto.builder()
                .userId(response.getUserInfo().getUserId())
                .username(response.getUserInfo().getUsername())
                .email(response.getUserInfo().getEmail())
//                .status(response.getSt())
//                .emailVerificationRequired(response.getEmailVerificationRequired())
                .message(response.getMessage())
                .build();
    }

    public RefreshTokenRequest toGrpcRefreshRequest(RefreshTokenRequestDto dto) {
        return RefreshTokenRequest.newBuilder()
                .setRefreshToken(dto.getRefreshToken())
                .build();
    }

    public RefreshTokenResponseDto toRefreshDto(RefreshTokenResponse response) {
        return RefreshTokenResponseDto.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .expiresIn(response.getExpiresIn())
                .build();
    }

    public LogoutRequest toGrpcLogoutRequest(LogoutRequestDto dto) {
        LogoutRequest.Builder builder = LogoutRequest.newBuilder()
                .setRefreshToken(dto.getRefreshToken())
                .setAccessToken(dto.getAccessToken());

        if (dto.getRefreshToken() != null) {
            builder.setRefreshToken(dto.getRefreshToken());
        }

        return builder.build();
    }

    public LogoutResponseDto toLogoutDto(LogoutResponse response) {
        return LogoutResponseDto.builder()
                .message(response.getMessage())
                .success(response.getSuccess())
                .build();
    }

    public ValidateTokenRequest toGrpcValidateRequest(ValidateTokenRequestDto dto) {
        return ValidateTokenRequest.newBuilder()
                .setToken(dto.getToken())
                .build();
    }

    public ValidateTokenResponseDto toValidateDto(ValidateTokenResponse response) {
        return ValidateTokenResponseDto.builder()
                .valid(response.getValid())
                .userId(response.getUserInfo().getUserId())
                .username(response.getUserInfo().getUsername())
                .email(response.getUserInfo().getEmail())
                .build();
    }
}