package com.barakah.transaction.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInfo {
    private String accountId;
    private String accountNumber;
    private String accountName;
    private String accountType;
    private String userId;
    private BigDecimal balance;
    private String status;
}