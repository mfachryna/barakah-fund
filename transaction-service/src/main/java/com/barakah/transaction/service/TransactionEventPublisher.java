package com.barakah.transaction.service;

import com.barakah.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {
    
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    
    @Value("${app.kafka.topics.transaction-events:transaction-events}")
    private String transactionEventsTopic;
    
    public void publishTransactionCreated(com.barakah.transaction.entity.Transaction transaction) {
        TransactionEvent event = buildTransactionEvent(transaction, TransactionEvent.EventType.TRANSACTION_CREATED);
        publishEvent(event);
    }
    
    public void publishTransactionUpdated(com.barakah.transaction.entity.Transaction transaction) {
        TransactionEvent event = buildTransactionEvent(transaction, TransactionEvent.EventType.TRANSACTION_UPDATED);
        publishEvent(event);
    }
    
    public void publishTransactionStatusChanged(com.barakah.transaction.entity.Transaction transaction) {
        TransactionEvent event = buildTransactionEvent(transaction, TransactionEvent.EventType.TRANSACTION_STATUS_CHANGED);
        publishEvent(event);
    }
    
    public void publishBalanceUpdateRequired(com.barakah.transaction.entity.Transaction transaction) {
        TransactionEvent event = buildTransactionEvent(transaction, TransactionEvent.EventType.BALANCE_UPDATE_REQUIRED);
        publishEvent(event);
    }
    
    private TransactionEvent buildTransactionEvent(com.barakah.transaction.entity.Transaction transaction, 
                                                  TransactionEvent.EventType eventType) {
        return TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionId(transaction.getTransactionId())
                .referenceNumber(transaction.getReferenceNumber())
                .eventType(eventType)
                .transactionEventType(mapTransactionType(transaction.getType()))
                .fromAccountId(transaction.getFromAccountId())
                .fromAccountNumber(transaction.getFromAccountNumber())
                .toAccountId(transaction.getToAccountId())
                .toAccountNumber(transaction.getToAccountNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .direction(transaction.getDirection().toString())
                .status(transaction.getStatus().toString())
                .balanceBeforeFrom(transaction.getBalanceBefore())
                .balanceAfterFrom(transaction.getBalanceAfter())
                .userId(transaction.getCreatedBy())
                .description(transaction.getDescription())
                .timestamp(LocalDateTime.now())
                .source("transaction-service")
                .build();
    }
    
    private TransactionEvent.TransactionEventType mapTransactionType(com.barakah.transaction.enums.TransactionType type) {
        return switch (type) {
            case TRANSFER -> TransactionEvent.TransactionEventType.TRANSFER;
            case DEPOSIT -> TransactionEvent.TransactionEventType.DEPOSIT;
            case WITHDRAWAL -> TransactionEvent.TransactionEventType.WITHDRAWAL;
            case PAYMENT -> TransactionEvent.TransactionEventType.PAYMENT;
            case REFUND -> TransactionEvent.TransactionEventType.REFUND;
            case FEE -> TransactionEvent.TransactionEventType.FEE;
            case INTEREST -> TransactionEvent.TransactionEventType.INTEREST;
        };
    }
    
    private void publishEvent(TransactionEvent event) {
        try {
            CompletableFuture<SendResult<String, TransactionEvent>> future = 
                kafkaTemplate.send(transactionEventsTopic, event.getTransactionId(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Transaction event published successfully: {} for transaction: {}", 
                            event.getEventType(), event.getTransactionId());
                } else {
                    log.error("Failed to publish transaction event: {} for transaction: {}, error: {}", 
                            event.getEventType(), event.getTransactionId(), ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing transaction event: {} for transaction: {}, error: {}", 
                    event.getEventType(), event.getTransactionId(), e.getMessage());
        }
    }
}