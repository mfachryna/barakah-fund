package com.barakah.gateway.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTokenResponseDto {
    private Boolean valid;
    private String userId;
    private String username;
    private String email;
    private String roles;
    private List<String> permissions;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String tokenType;
}