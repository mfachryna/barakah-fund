package com.barakah.transaction.service;

import com.barakah.transaction.dto.AccountInfo;
import com.barakah.transaction.enums.TransactionStatus;
import com.barakah.transaction.enums.TransactionType;
import com.barakah.transaction.exception.TransactionExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionValidationService {

    public void validateTransactionRequest(TransactionType type, String fromAccountNumber, String toAccountNumber,
                                          BigDecimal amount) {
        if (type == null) {
            throw new TransactionExceptions.InvalidTransactionException("Transaction type is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionExceptions.InvalidTransactionException("Amount must be positive");
        }

        if (type == TransactionType.TRANSFER) {
            if (fromAccountNumber == null || fromAccountNumber.isEmpty()) {
                throw new TransactionExceptions.InvalidTransactionException("Source account is required for transfer");
            }
            if (toAccountNumber == null || toAccountNumber.isEmpty()) {
                throw new TransactionExceptions.InvalidTransactionException("Target account is required for transfer");
            }
            if (fromAccountNumber.equals(toAccountNumber)) {
                throw new TransactionExceptions.SameAccountTransferException(fromAccountNumber);
            }
        }

        switch (type) {
            case DEPOSIT, REFUND, INTEREST -> {
                if (toAccountNumber == null || toAccountNumber.isEmpty()) {
                    throw new TransactionExceptions.InvalidTransactionException("Target account is required for " + type);
                }
            }
            case WITHDRAWAL, PAYMENT, FEE -> {
                if (fromAccountNumber == null || fromAccountNumber.isEmpty()) {
                    throw new TransactionExceptions.InvalidTransactionException("Source account is required for " + type);
                }
            }
        }
    }

    public void validateAccountPermissions(AccountInfo fromAccount, AccountInfo toAccount, String currentUserId, TransactionType type) {
        if (type == TransactionType.TRANSFER && fromAccount != null) {
            if (!currentUserId.equals(fromAccount.getUserId())) {
                throw new TransactionExceptions.InvalidTransactionException("Insufficient permissions for source account");
            }
        }

        if ((type == TransactionType.WITHDRAWAL || type == TransactionType.PAYMENT || type == TransactionType.FEE)
                && fromAccount != null) {
            if (!currentUserId.equals(fromAccount.getUserId())) {
                throw new TransactionExceptions.InvalidTransactionException("Insufficient permissions for account");
            }
        }

        if ((type == TransactionType.DEPOSIT || type == TransactionType.REFUND || type == TransactionType.INTEREST)
                && toAccount != null) {
            if (!currentUserId.equals(toAccount.getUserId())) {
                log.warn("User {} attempting to {} to account owned by {}", currentUserId, type, toAccount.getUserId());
            }
        }
    }

    public void validateSufficientBalance(AccountInfo account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new TransactionExceptions.InsufficientBalanceException(
                    account.getAccountNumber(),
                    account.getBalance().toString(),
                    amount.toString());
        }
    }

    public void validateStatusTransition(TransactionStatus from, TransactionStatus to) {
        boolean isValidTransition = switch (from) {
            case PENDING -> List.of(TransactionStatus.PROCESSING, TransactionStatus.CANCELLED).contains(to);
            case PROCESSING -> List.of(TransactionStatus.COMPLETED, TransactionStatus.FAILED).contains(to);
            case COMPLETED -> List.of(TransactionStatus.REVERSED).contains(to);
            case FAILED, CANCELLED -> false;
            case REVERSED -> false;
        };

        if (!isValidTransition) {
            throw new TransactionExceptions.InvalidTransactionStatusException("", from.toString(), to.toString());
        }
    }
}