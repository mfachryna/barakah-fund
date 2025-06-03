package com.barakah.transaction.entity;

import com.barakah.transaction.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "reference_number", nullable = false, unique = true, length = 50)
    private String referenceNumber;

     @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

     @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransactionDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type")
    private TransferType transferType;

    @Column(name = "from_account_id")
    private String fromAccountId;

    @Column(name = "from_account_number", length = 20)
    private String fromAccountNumber;

    @Column(name = "to_account_id")
    private String toAccountId;

    @Column(name = "to_account_number", length = 20)
    private String toAccountNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "IDR";

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "category_id")
    private String categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"transactions", "hibernateLazyInitializer", "handler"})
    // @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private TransactionCategory category;

    @Column(name = "balance_before", precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "external_provider", length = 50)
    private String externalProvider;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (referenceNumber == null || referenceNumber.trim().isEmpty()) {
            referenceNumber = generateReferenceNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    private String generateReferenceNumber() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return String.format("TXN-%s-%s", timestamp, random);
    }

    @Override
    public String toString() {
        return String.format("Transaction{id='%s', type=%s, status=%s, amount=%s, reference='%s'}", 
                transactionId, type, status, amount, referenceNumber);
    }
}