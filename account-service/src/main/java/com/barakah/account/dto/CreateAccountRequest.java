package com.barakah.account.dto;

import com.barakah.account.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateAccountRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    
    @NotBlank(message = "Account name is required")
    private String accountName;
    
    @PositiveOrZero(message = "Initial deposit must be zero or positive")
    private BigDecimal initialDeposit;
}