package com.barakah.gateway.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {
    private String transactionId;
    private String transactionType;
    private String fromAccountId;
    private String fromAccountNumber;
    private String toAccountId;
    private String toAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status;
    private String categoryId;
    private String categoryName;
    private BigDecimal balanceBeforeFrom;
    private BigDecimal balanceAfterFrom;
    private BigDecimal balanceBeforeTo;
    private BigDecimal balanceAfterTo;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String reference;
    private String notes;
}