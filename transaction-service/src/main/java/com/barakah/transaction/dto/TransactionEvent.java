package com.barakah.transaction.dto;

import com.barakah.transaction.enums.TransactionEventType;
import com.barakah.transaction.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionEvent {
    private String eventId;
    private TransactionEventType eventType;
    private String transactionId;
    private String transactionNumber;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private TransactionStatus status;
    private BigDecimal fraudScore;
    private String failureReason;
    private String createdBy;
    private LocalDateTime timestamp;
}