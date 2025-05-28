package com.barakah.transaction.entity;

import com.barakah.transaction.enums.TransactionDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_logs", indexes = {
    @Index(name = "idx_log_transaction", columnList = "transactionId"),
    @Index(name = "idx_log_account", columnList = "accountId"),
    @Index(name = "idx_log_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {
    
    @Id
    @Column(name = "log_id", length = 36)
    private String logId;
    
    @Column(name = "transaction_id", nullable = false, length = 36)
    private String transactionId;
    
    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;
    
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransactionDirection direction;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        if (logId == null) {
            logId = java.util.UUID.randomUUID().toString();
        }
    }
}