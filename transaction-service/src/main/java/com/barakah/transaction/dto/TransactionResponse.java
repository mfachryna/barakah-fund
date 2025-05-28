package com.barakah.transaction.dto;

import com.barakah.transaction.enums.TransactionStatus;
import com.barakah.transaction.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private String id;
    private String transactionNumber;
    private String fromAccountId;
    private String toAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private TransactionStatus status;
    private String description;
    private String referenceNumber;
    private BigDecimal fraudScore;
    private String fraudReason;
    private String receiptUrl;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}