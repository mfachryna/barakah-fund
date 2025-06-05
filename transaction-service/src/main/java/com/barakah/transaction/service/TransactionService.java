package com.barakah.transaction.service;

import com.barakah.shared.context.UserContextHolder;
import com.barakah.transaction.aspect.RateLimitAspect.RateLimit;
import com.barakah.transaction.client.AccountServiceClient;
import com.barakah.transaction.dto.AccountInfo;
import com.barakah.transaction.entity.Transaction;
import com.barakah.transaction.enums.TransactionStatus;
import com.barakah.transaction.enums.TransactionType;
import com.barakah.transaction.exception.TransactionExceptions;
import com.barakah.transaction.repository.TransactionRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final AccountServiceClient accountServiceClient;

    @RateLimit(endpoint = "create-transaction")
    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackCreateTransaction")
    @Retry(name = "database")
    @Bulkhead(name = "transaction-service")
    @RateLimiter(name = "transaction-creation")
    @CacheEvict(value = {"transactions", "user-transactions"}, allEntries = true)
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

    @RateLimit(endpoint = "process-transaction")
    @Retry(name = "database")
    @Bulkhead(name = "transaction-processing")
    @Transactional
    public Transaction processTransaction(String transactionId) {
        log.info("Processing transaction: {}", transactionId);
        return transactionProcessingService.processTransaction(transactionId);
    }

    @RateLimit(endpoint = "update-transaction")
    @Retry(name = "database")
    @CacheEvict(value = {"transactions", "user-transactions"}, key = "#transactionId")
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

    @RateLimit(endpoint = "query-transaction")
    @Cacheable(value = "transactions", key = "#transactionId")
    @Retry(name = "database")
    @Transactional(readOnly = true)
    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException(transactionId));
    }

    @RateLimit(endpoint = "query-transaction")
    @Cacheable(value = "transactions", key = "#transactionId + '_category'")
    @Retry(name = "database")
    @Transactional(readOnly = true)
    public Transaction getTransactionByIdWithCategory(String transactionId) {
        return transactionRepository.findByIdWithCategory(transactionId)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException(transactionId));
    }

    @RateLimit(endpoint = "query-transaction")
    @Cacheable(value = "transactions", key = "'ref_' + #referenceNumber")
    @Retry(name = "database")
    @Transactional(readOnly = true)
    public Transaction getTransactionByReference(String referenceNumber) {
        return transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException("reference", referenceNumber));
    }

    @RateLimit(endpoint = "list-transactions")
    @Cacheable(value = "user-transactions", key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #filters.hashCode()")
    @Retry(name = "database")
    @Transactional(readOnly = true)
    public Page<Transaction> listTransactions(String userId, Map<String, String> filters,
                                              String search, Pageable pageable) {
        log.debug("Listing transactions with filters - userId: {}", userId);
        return transactionQueryService.listTransactions(userId, filters, search, pageable);
    }

    @RateLimit(endpoint = "list-transactions")
    @Cacheable(value = "account-transactions", key = "#accountNumber + '_' + #direction + '_' + #pageable.pageNumber")
    @Retry(name = "database")
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

//    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackValidateAccountBalance")
//    @Retry(name = "account-service")
//    private void validateAccountBalance(String accountId, BigDecimal amount) {
//        BigDecimal balance = accountServiceClient.getAccountBalance(accountId);
//        if (balance.compareTo(amount) < 0) {
//            throw new RuntimeException("Insufficient balance");
//        }
//    }

//    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackProcessTransfer")
//    @Retry(name = "account-service")
//    private void processTransfer(Transaction transaction) {
//        accountServiceClient.(transaction.getFromAccountId(), transaction.getAmount());
//        accountServiceClient.creditAccount(transaction.getToAccountId(), transaction.getAmount());

//        transaction.setStatus("COMPLETED");
//        transactionRepository.save(transaction);
//    }

    // Fallback Methods
    public Transaction fallbackCreateTransaction(String fromAccountId, String toAccountId,
                                               BigDecimal amount, String description, Exception ex) {
        log.error("Failed to create transaction, using fallback: {}", ex.getMessage());
        throw new RuntimeException("Transaction service is temporarily unavailable");
    }

    public Transaction fallbackProcessTransaction(String transactionId, Exception ex) {
        log.error("Failed to process transaction {}, using fallback: {}", transactionId, ex.getMessage());
        throw new RuntimeException("Transaction processing is temporarily unavailable");
    }

    public Transaction fallbackUpdateTransactionStatus(String transactionId, TransactionStatus newStatus, String notes, Exception ex) {
        log.error("Failed to update transaction status {}, using fallback: {}", transactionId, ex.getMessage());
        throw new RuntimeException("Transaction update is temporarily unavailable");
    }

    public Transaction fallbackGetTransactionById(String transactionId, Exception ex) {
        log.error("Failed to get transaction {}, using fallback: {}", transactionId, ex.getMessage());
        throw new RuntimeException("Transaction retrieval is temporarily unavailable");
    }

    public Transaction fallbackGetTransactionByIdWithCategory(String transactionId, Exception ex) {
        log.error("Failed to get transaction with category {}, using fallback: {}", transactionId, ex.getMessage());
        throw new RuntimeException("Transaction retrieval is temporarily unavailable");
    }

    public Transaction fallbackGetTransactionByReference(String referenceNumber, Exception ex) {
        log.error("Failed to get transaction by reference {}, using fallback: {}", referenceNumber, ex.getMessage());
        throw new RuntimeException("Transaction retrieval is temporarily unavailable");
    }

    public Page<Transaction> fallbackListTransactions(String userId, Map<String, String> filters, String search, Pageable pageable, Exception ex) {
        log.error("Failed to list transactions for user {}, using fallback: {}", userId, ex.getMessage());
        throw new RuntimeException("Transaction listing is temporarily unavailable");
    }

    public Page<Transaction> fallbackGetTransactionsByAccount(String accountNumber, LocalDateTime fromDate, LocalDateTime toDate, TransactionDirection direction, Pageable pageable, Exception ex) {
        log.error("Failed to get transactions for account {}, using fallback: {}", accountNumber, ex.getMessage());
        throw new RuntimeException("Transaction listing is temporarily unavailable");
    }
}


