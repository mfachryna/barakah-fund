package com.barakah.gateway.mapper;

import com.barakah.auth.proto.v1.*;
import com.barakah.gateway.dto.auth.*;
import com.barakah.gateway.dto.user.UserResponseDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
                .success(response.getSuccess())
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .expiresIn(response.getExpiresIn())
                .userInfo(
                        UserResponseDto.builder()
                                .userId(response.getUserInfo().getUserId())
                                .email(response.getUserInfo().getEmail())
                                .email(response.getUserInfo().getEmail())
                                .dateOfBirth(!response.getUserInfo().getDateOfBirth().isEmpty()
                                        ? LocalDateTime.parse(response.getUserInfo().getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE).toLocalDate()
                                        : null)
                                .firstName(response.getUserInfo().getFirstName())
                                .lastName(response.getUserInfo().getLastName())
                                .address(response.getUserInfo().getAddress())
                                .status(response.getUserInfo().getStatus().toString())
                                .phoneNumber(response.getUserInfo().getPhoneNumber())
                                .createdAt(!response.getUserInfo().getCreatedAt().isEmpty()
                                        ? LocalDateTime.parse(response.getUserInfo().getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                        : null)
                                .updatedAt(!response.getUserInfo().getUpdatedAt().isEmpty()
                                        ? LocalDateTime.parse(response.getUserInfo().getUpdatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                        : null)
                                .build()
                )
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
            builder.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getAddress() != null) {
            builder.setAddress(dto.getAddress());
        }

        return builder.build();
    }

    public LoginResponseDto toRegisterDto(RegisterResponse response) {
        return LoginResponseDto.builder()
                .success(response.getSuccess())
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .expiresIn(response.getExpiresIn())
                .userInfo(
                        UserResponseDto.builder()
                                .userId(response.getUserInfo().getUserId())
                                .email(response.getUserInfo().getEmail())
                                .email(response.getUserInfo().getEmail())
                                .dateOfBirth(!response.getUserInfo().getDateOfBirth().isEmpty()
                                        ? LocalDateTime.parse(response.getUserInfo().getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE).toLocalDate()
                                        : null)
                                .firstName(response.getUserInfo().getFirstName())
                                .lastName(response.getUserInfo().getLastName())
                                .address(response.getUserInfo().getAddress())
                                .status(response.getUserInfo().getStatus().toString())
                                .phoneNumber(response.getUserInfo().getPhoneNumber())
                                .createdAt(!response.getUserInfo().getCreatedAt().isEmpty()
                                        ? LocalDateTime.parse(response.getUserInfo().getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                        : null)
                                .updatedAt(!response.getUserInfo().getUpdatedAt().isEmpty()
                                        ? LocalDateTime.parse(response.getUserInfo().getUpdatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                        : null)
                                .build()
                )
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
//                .setRefreshToken(dto.getRefreshToken())
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