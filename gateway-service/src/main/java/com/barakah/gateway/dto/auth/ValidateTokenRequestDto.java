package com.barakah.gateway.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTokenRequestDto {
    
    @NotBlank(message = "Token is required")
    private String token;
}