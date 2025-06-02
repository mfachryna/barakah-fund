package com.barakah.gateway.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponseDto {
    private String userId;
    private String username;
    private String email;
    private String status;
    private Boolean emailVerificationRequired;
    private String message;
    private LocalDateTime registeredAt;
}