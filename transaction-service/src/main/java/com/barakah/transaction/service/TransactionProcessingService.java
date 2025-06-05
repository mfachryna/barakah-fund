package com.barakah.transaction.service;

import com.barakah.transaction.client.AccountServiceClient;
import com.barakah.transaction.dto.AccountInfo;
import com.barakah.transaction.entity.Transaction;
import com.barakah.transaction.enums.TransactionStatus;
import com.barakah.transaction.enums.TransactionType;
import com.barakah.transaction.exception.TransactionExceptions;
import com.barakah.transaction.repository.TransactionRepository;
import com.barakah.shared.context.UserContextHolder;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessingService {

    private final TransactionRepository transactionRepository;
    private final TransactionLogService logService;
    private final TransactionEventPublisher eventPublisher;
    private final TransactionValidationService validationService;
    private final AccountService accountService;
    private final AccountServiceClient accountServiceClient;
    private final TransactionUtils transactionUtils;

    @Retry(name = "database")
    @Transactional
    public Transaction processTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException(transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new TransactionExceptions.InvalidTransactionStatusException(
                    transactionId, transaction.getStatus().toString(), "PROCESSING");
        }

        try {
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction = transactionRepository.save(transaction);

            processTransactionByType(transaction);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setUpdatedBy(UserContextHolder.getCurrentUserId());
            Transaction completedTransaction = transactionRepository.save(transaction);

            logService.createTransactionLogs(completedTransaction);

            eventPublisher.publishTransactionStatusChanged(completedTransaction);

            log.info("Transaction processed successfully: {}", transactionId);

            return transaction;

        } catch (Exception e) {
            log.error("Failed to process transaction {}: {}", transactionId, e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setNotes(transaction.getNotes() + " | Error: " + e.getMessage());
            transaction.setUpdatedBy(UserContextHolder.getCurrentUserId());
            transactionRepository.save(transaction);

            eventPublisher.publishTransactionStatusChanged(transaction);

            throw new TransactionExceptions.TransactionProcessingException(e.getMessage(), e);
        }
    }

    private void processTransactionByType(Transaction transaction) {
        log.info("Processing transaction type: {} for transaction: {}",
                transaction.getType(), transaction.getTransactionId());

        AccountInfo fromAccount = null;
        AccountInfo toAccount = null;

        if (transaction.getFromAccountId() != null) {
            fromAccount = accountServiceClient.getAccount(transaction.getFromAccountId());
            transaction.setBalanceBefore(fromAccount.getBalance());
        }

        if (transaction.getToAccountId() != null) {
            toAccount = accountServiceClient.getAccount(transaction.getToAccountId());
        }

        switch (transaction.getType()) {
            case TRANSFER ->
                processTransfer(transaction, fromAccount, toAccount);
            case DEPOSIT ->
                processDeposit(transaction, toAccount);
            case WITHDRAWAL ->
                processWithdrawal(transaction, fromAccount);
            case PAYMENT ->
                processPayment(transaction, fromAccount);
            case REFUND ->
                processRefund(transaction, toAccount);
            case FEE ->
                processFee(transaction, fromAccount);
            case INTEREST ->
                processInterest(transaction, toAccount);
        }
    }

    private void processTransfer(Transaction transaction, AccountInfo fromAccount, AccountInfo toAccount) {
        log.info("Processing transfer: {} -> {}",
                transaction.getFromAccountNumber(), transaction.getToAccountNumber());

        if (fromAccount == null || toAccount == null) {
            throw new TransactionExceptions.InvalidTransactionException("Both accounts required for transfer");
        }

        validationService.validateSufficientBalance(fromAccount, transaction.getAmount());

        if (!fromAccount.getUserId().equals(toAccount.getUserId())) {
            log.info("Processing external transfer between users: {} -> {}",
                    fromAccount.getUserId(), toAccount.getUserId());
        } else {
            log.info("Processing internal transfer for user: {}", fromAccount.getUserId());
        }

//        accountServiceClient.debitAccount(fromAccount.getAccountId(), transaction.getAmount());
//
//        accountServiceClient.creditAccount(toAccount.getAccountId(), transaction.getAmount());
        AccountInfo updatedFromAccount = accountServiceClient.getAccount(fromAccount.getAccountId());
        transaction.setBalanceAfter(updatedFromAccount.getBalance());
    }

    private void processDeposit(Transaction transaction, AccountInfo toAccount) {
        log.info("Processing deposit to: {}", transaction.getToAccountNumber());

        if (toAccount == null) {
            throw new TransactionExceptions.InvalidTransactionException("Target account required for deposit");
        }

        accountServiceClient.creditAccount(toAccount.getAccountId(), transaction.getAmount());

        AccountInfo updatedAccount = accountServiceClient.getAccount(toAccount.getAccountId());
        transaction.setBalanceAfter(updatedAccount.getBalance());
    }

    private void processWithdrawal(Transaction transaction, AccountInfo fromAccount) {
        log.info("Processing withdrawal from: {}", transaction.getFromAccountNumber());

        if (fromAccount == null) {
            throw new TransactionExceptions.InvalidTransactionException("Source account required for withdrawal");
        }

        validationService.validateSufficientBalance(fromAccount, transaction.getAmount());

//        accountServiceClient.debitAccount(fromAccount.getAccountId(), transaction.getAmount());
        AccountInfo updatedAccount = accountServiceClient.getAccount(fromAccount.getAccountId());
        transaction.setBalanceAfter(updatedAccount.getBalance());
    }

    private void processPayment(Transaction transaction, AccountInfo fromAccount) {
        log.info("Processing payment from: {}", transaction.getFromAccountNumber());

        if (fromAccount == null) {
            throw new TransactionExceptions.InvalidTransactionException("Source account required for payment");
        }

        validationService.validateSufficientBalance(fromAccount, transaction.getAmount());

//        accountServiceClient.debitAccount(fromAccount.getAccountId(), transaction.getAmount());
        if (transaction.getExternalProvider() != null) {
            log.info("Processing payment with external provider: {}", transaction.getExternalProvider());

        }

        AccountInfo updatedAccount = accountServiceClient.getAccount(fromAccount.getAccountId());
        transaction.setBalanceAfter(updatedAccount.getBalance());
    }

    private void processRefund(Transaction transaction, AccountInfo toAccount) {
        log.info("Processing refund to: {}", transaction.getToAccountNumber());

        if (toAccount == null) {
            throw new TransactionExceptions.InvalidTransactionException("Target account required for refund");
        }

        accountServiceClient.creditAccount(toAccount.getAccountId(), transaction.getAmount());

        AccountInfo updatedAccount = accountServiceClient.getAccount(toAccount.getAccountId());
        transaction.setBalanceAfter(updatedAccount.getBalance());
    }

    private void processFee(Transaction transaction, AccountInfo fromAccount) {
        log.info("Processing fee from: {}", transaction.getFromAccountNumber());

        if (fromAccount == null) {
            throw new TransactionExceptions.InvalidTransactionException("Source account required for fee");
        }

        validationService.validateSufficientBalance(fromAccount, transaction.getAmount());

        AccountInfo updatedAccount = accountServiceClient.getAccount(fromAccount.getAccountId());
        transaction.setBalanceAfter(updatedAccount.getBalance());
    }

    private void processInterest(Transaction transaction, AccountInfo toAccount) {
        log.info("Processing interest to: {}", transaction.getToAccountNumber());

        if (toAccount == null) {
            throw new TransactionExceptions.InvalidTransactionException("Target account required for interest");
        }

        accountServiceClient.creditAccount(toAccount.getAccountId(), transaction.getAmount());

        AccountInfo updatedAccount = accountServiceClient.getAccount(toAccount.getAccountId());
        transaction.setBalanceAfter(updatedAccount.getBalance());
    }
}
