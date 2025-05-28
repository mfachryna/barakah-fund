package com.barakah.shared.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {

    private String eventId;
    private String transactionId;
    private String referenceNumber;
    private EventType eventType;
    private TransactionEventType transactionEventType;

    private String fromAccountId;
    private String fromAccountNumber;
    private String toAccountId;
    private String toAccountNumber;

    private BigDecimal amount;
    private String currency;
    private String direction;
    private String status;

    private BigDecimal balanceBeforeFrom;
    private BigDecimal balanceAfterFrom;
    private BigDecimal balanceBeforeTo;
    private BigDecimal balanceAfterTo;

    private String userId;
    private String description;
    private LocalDateTime timestamp;
    private String source;

    public enum EventType {
        TRANSACTION_CREATED,
        TRANSACTION_UPDATED,
        TRANSACTION_STATUS_CHANGED,
        BALANCE_UPDATE_REQUIRED
    }

    public enum TransactionEventType {
        TRANSFER,
        DEPOSIT,
        WITHDRAWAL,
        PAYMENT,
        REFUND,
        FEE,
        INTEREST
    }
}
