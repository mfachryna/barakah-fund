package com.barakah.account.dto;

import com.barakah.account.enums.AccountStatus;
import com.barakah.account.enums.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {
    private String id;
    private String accountNumber;
    private String userId;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private String accountName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}