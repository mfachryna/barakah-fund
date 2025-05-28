package com.barakah.account.entity;

import com.barakah.account.enums.BalanceOperation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_balance_history", indexes = {
    @Index(name = "idx_balance_history_account", columnList = "accountId"),
    @Index(name = "idx_balance_history_transaction", columnList = "transactionId"),
    @Index(name = "idx_balance_history_timestamp", columnList = "timestamp"),
    @Index(name = "idx_balance_history_account_timestamp", columnList = "accountId, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceHistory {
    
    @Id
    @Column(name = "history_id", length = 36)
    private String historyId;
    
    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;
    
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false)
    private BalanceOperation operation;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    
    @Column(name = "transaction_id", length = 36)
    private String transactionId;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        if (historyId == null) {
            historyId = java.util.UUID.randomUUID().toString();
        }
    }
}