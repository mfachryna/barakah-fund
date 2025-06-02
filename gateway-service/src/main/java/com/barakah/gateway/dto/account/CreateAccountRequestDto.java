package com.barakah.gateway.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequestDto {
    
    @NotBlank(message = "Account type is required")
    private String accountType;
    
    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name must not exceed 100 characters")
    private String accountName;
    
    private String userId; // Optional, will use current user if not provided
    
    @NotNull(message = "Initial balance is required")
    private BigDecimal initialBalance;
    
    private String currency = "IDR";
}