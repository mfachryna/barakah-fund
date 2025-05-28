package com.barakah.transaction.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceUpdateResponse {
    private boolean success;
    private String accountNumber;
    private BigDecimal newBalance;
    private String message;
}