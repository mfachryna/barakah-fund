package com.barakah.transaction.service;

import com.barakah.transaction.dto.AccountInfo;
import com.barakah.transaction.enums.TransactionDirection;
import com.barakah.transaction.enums.TransactionType;
import com.barakah.transaction.enums.TransferType;
import org.springframework.stereotype.Component;

@Component
public class TransactionUtils {

    public TransactionDirection determineTransactionDirection(TransactionType type, AccountInfo fromAccount, String currentUserId) {
        return switch (type) {
            case WITHDRAWAL, PAYMENT, FEE -> TransactionDirection.DEBIT;
            case DEPOSIT, REFUND, INTEREST -> TransactionDirection.CREDIT;
            case TRANSFER -> {
                if (fromAccount != null && currentUserId.equals(fromAccount.getUserId())) {
                    yield TransactionDirection.DEBIT;
                } else {
                    yield TransactionDirection.CREDIT;
                }
            }
        };
    }

    public TransferType determineTransferType(AccountInfo fromAccount, AccountInfo toAccount) {
        if (fromAccount != null && toAccount != null) {
            if (fromAccount.getUserId().equals(toAccount.getUserId())) {
                return TransferType.INTERNAL;
            } else {
                return TransferType.EXTERNAL;
            }
        }
        return null;
    }

    public boolean shouldProcessImmediately(TransactionType type) {
        return switch (type) {
            case TRANSFER, PAYMENT -> true;
            case DEPOSIT, WITHDRAWAL -> false;
            case REFUND, FEE, INTEREST -> true;
        };
    }
}