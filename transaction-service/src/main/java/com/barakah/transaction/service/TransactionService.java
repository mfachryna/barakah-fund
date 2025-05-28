package com.barakah.transaction.service;

import com.barakah.transaction.dto.AccountInfo;
import com.barakah.transaction.entity.Transaction;
import com.barakah.transaction.enums.TransactionStatus;
import com.barakah.transaction.enums.TransactionType;
import com.barakah.transaction.exception.TransactionExceptions;
import com.barakah.transaction.repository.TransactionRepository;
import com.barakah.shared.context.UserContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.barakah.transaction.enums.TransactionDirection;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionCreationService transactionCreationService;
    private final TransactionProcessingService transactionProcessingService;
    private final TransactionQueryService transactionQueryService;
    private final TransactionValidationService transactionValidationService;
    private final AccountService accountService;
    private final TransactionUtils transactionUtils;

    @Transactional
    public Transaction createTransaction(TransactionType type, String fromAccountNumber, String toAccountNumber,
                                         BigDecimal amount, String description, String notes,
                                         String categoryId, String externalReference, String externalProvider) {

        log.info("Creating transaction: {} from {} to {} amount {}", type, fromAccountNumber, toAccountNumber, amount);
        Transaction transaction = transactionCreationService.createTransaction(type, fromAccountNumber, toAccountNumber,
                                                           amount, description, notes, categoryId,
                                                           externalReference, externalProvider);

        if (transactionUtils.shouldProcessImmediately(type)) {
            log.info("Processing transaction immediately: {}", transaction.getTransactionId());
            return transactionProcessingService.processTransaction(transaction.getTransactionId());
        }

        return transaction;
    }

    @Transactional
    public Transaction processTransaction(String transactionId) {
        log.info("Processing transaction: {}", transactionId);
        return transactionProcessingService.processTransaction(transactionId);
    }

    @Transactional
    public Transaction updateTransactionStatus(String transactionId, TransactionStatus newStatus, String notes) {
        log.info("Updating transaction status: {} to {}", transactionId, newStatus);

        Transaction transaction = getTransactionById(transactionId);
        transactionValidationService.validateStatusTransition(transaction.getStatus(), newStatus);

        TransactionStatus oldStatus = transaction.getStatus();
        transaction.setStatus(newStatus);

        if (notes != null && !notes.isEmpty()) {
            transaction.setNotes(transaction.getNotes() + " | " + notes);
        }

        transaction.setUpdatedBy(UserContextHolder.getCurrentUserId());
        Transaction updatedTransaction = transactionRepository.save(transaction);

        log.info("Transaction status updated: {} from {} to {}", transactionId, oldStatus, newStatus);
        return updatedTransaction;
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionByIdWithCategory(String transactionId) {
        return transactionRepository.findByIdWithCategory(transactionId)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionByReference(String referenceNumber) {
        return transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException("reference", referenceNumber));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listTransactions(String userId, Map<String, String> filters,
                                              String search, Pageable pageable) {
        log.debug("Listing transactions with filters - userId: {}", userId);
        return transactionQueryService.listTransactions(userId, filters, search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByAccount(String accountNumber, LocalDateTime fromDate, LocalDateTime toDate,
                                                      TransactionDirection direction, Pageable pageable) {

        log.debug("Getting transactions by account: {}", accountNumber);

        AccountInfo account = accountService.getAndValidateAccount(accountNumber);

        if (direction != null) {
            return transactionRepository.findByAccountIdAndDirection(account.getAccountId(), direction, pageable);
        }

        Page<Transaction> transactions = transactionRepository.findByAccountNumber(accountNumber, pageable);

        if (fromDate != null || toDate != null) {
            return transactionRepository.findByDateRange(
                    fromDate != null ? fromDate : LocalDateTime.MIN,
                    toDate != null ? toDate : LocalDateTime.MAX,
                    pageable);
        }

        return transactions;
    }
}


