package com.barakah.transaction.service;

import com.barakah.transaction.dto.AccountInfo;
import com.barakah.transaction.entity.Transaction;
import com.barakah.transaction.enums.*;
import com.barakah.transaction.exception.TransactionExceptions;
import com.barakah.transaction.repository.TransactionRepository;
import com.barakah.shared.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCreationService {

    private final TransactionRepository transactionRepository;
    private final TransactionCategoryService categoryService;
    private final TransactionValidationService validationService;
    private final TransactionEventPublisher eventPublisher;
    private final AccountService accountService;
    private final TransactionUtils transactionUtils; 

    @Transactional
    public Transaction createTransaction(TransactionType type, String fromAccountNumber, String toAccountNumber,
                                         BigDecimal amount, String description, String notes,
                                         String categoryId, String externalReference, String externalProvider) {

        
        String currentUserId = UserContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            throw new TransactionExceptions.InvalidTransactionException("User context not found");
        }

        
        validationService.validateTransactionRequest(type, fromAccountNumber, toAccountNumber, amount);

        
        AccountInfo fromAccount = null;
        AccountInfo toAccount = null;

        if (fromAccountNumber != null && !fromAccountNumber.isEmpty()) {
            fromAccount = accountService.getAndValidateAccount(fromAccountNumber);
        }

        if (toAccountNumber != null && !toAccountNumber.isEmpty()) {
            toAccount = accountService.getAndValidateAccount(toAccountNumber);
        }

        
        validationService.validateAccountPermissions(fromAccount, toAccount, currentUserId, type);

        
        TransactionDirection direction = transactionUtils.determineTransactionDirection(type, fromAccount, currentUserId);
        TransferType transferType = transactionUtils.determineTransferType(fromAccount, toAccount);

        
        if (categoryId != null && !categoryId.isEmpty()) {
            categoryService.getCategoryById(categoryId);
        }

        
        if (externalReference != null && !externalReference.isEmpty()) {
            Optional<Transaction> existingTxn = transactionRepository.findByReferenceNumber(externalReference);
            if (existingTxn.isPresent()) {
                throw new TransactionExceptions.DuplicateTransactionException(externalReference);
            }
        }

        
        if (direction == TransactionDirection.DEBIT && fromAccount != null) {
            validationService.validateSufficientBalance(fromAccount, amount);
        }

        
        Transaction transaction = Transaction.builder()
                .type(type)
                .status(TransactionStatus.PENDING)
                .direction(direction)
                .transferType(transferType)
                .fromAccountId(fromAccount != null ? fromAccount.getAccountId() : null)
                .fromAccountNumber(fromAccountNumber)
                .toAccountId(toAccount != null ? toAccount.getAccountId() : null)
                .toAccountNumber(toAccountNumber)
                .amount(amount)
                .description(description)
                .notes(notes)
                .categoryId(categoryId)
                .externalReference(externalReference)
                .externalProvider(externalProvider)
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Transaction created successfully: {}", savedTransaction.getTransactionId());

        
        eventPublisher.publishTransactionCreated(savedTransaction);

        
        return savedTransaction;
    }
}