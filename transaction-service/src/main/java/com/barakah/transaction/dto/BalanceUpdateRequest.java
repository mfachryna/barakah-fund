package com.barakah.transaction.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceUpdateRequest {
    private String accountNumber;
    private BigDecimal amount;
    private String operation;
    private String transactionId;
    private String description;
}