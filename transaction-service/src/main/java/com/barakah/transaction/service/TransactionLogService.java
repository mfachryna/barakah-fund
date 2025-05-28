package com.barakah.transaction.service;

import com.barakah.transaction.client.AccountServiceClient;
import com.barakah.transaction.dto.AccountInfo;
import com.barakah.transaction.entity.TransactionLog;
import com.barakah.transaction.enums.TransactionDirection;
import com.barakah.transaction.enums.TransactionType;
import com.barakah.transaction.repository.TransactionLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.barakah.transaction.entity.Transaction;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionLogService {
    
    private final TransactionLogRepository logRepository;
    private final AccountServiceClient accountServiceClient;

    public void createTransactionLogs(Transaction transaction) {

        if (transaction.getFromAccountId() != null
                && (transaction.getDirection() == TransactionDirection.DEBIT
                || transaction.getType() == TransactionType.TRANSFER)) {

            TransactionLog fromLog = TransactionLog.builder()
                    .transactionId(transaction.getTransactionId())
                    .accountId(transaction.getFromAccountId())
                    .accountNumber(transaction.getFromAccountNumber())
                    .direction(TransactionDirection.DEBIT)
                    .amount(transaction.getAmount())
                    .balanceBefore(transaction.getBalanceBefore())
                    .balanceAfter(transaction.getBalanceAfter())
                    .notes("Transaction: " + transaction.getDescription())
                    .build();

            createLog(fromLog);
        }

        if (transaction.getToAccountId() != null
                && (transaction.getDirection() == TransactionDirection.CREDIT
                || transaction.getType() == TransactionType.TRANSFER)) {

            BigDecimal toBalanceBefore = BigDecimal.ZERO;
            BigDecimal toBalanceAfter = BigDecimal.ZERO;

            if (transaction.getType() == TransactionType.TRANSFER) {
                try {
                    AccountInfo toAccount = accountServiceClient.getAccount(transaction.getToAccountId());
                    toBalanceBefore = toAccount.getBalance().subtract(transaction.getAmount());
                    toBalanceAfter = toAccount.getBalance();
                } catch (Exception e) {
                    log.warn("Could not get target account balance for log: {}", e.getMessage());
                }
            } else {
                toBalanceBefore = transaction.getBalanceBefore();
                toBalanceAfter = transaction.getBalanceAfter();
            }

            TransactionLog toLog = TransactionLog.builder()
                    .transactionId(transaction.getTransactionId())
                    .accountId(transaction.getToAccountId())
                    .accountNumber(transaction.getToAccountNumber())
                    .direction(TransactionDirection.CREDIT)
                    .amount(transaction.getAmount())
                    .balanceBefore(toBalanceBefore)
                    .balanceAfter(toBalanceAfter)
                    .notes("Transaction: " + transaction.getDescription())
                    .build();

            createLog(toLog);
        }
    }

    @Transactional
    public TransactionLog createLog(TransactionLog tlog) {
        log.info("Creating transaction log for transaction: {} account: {}",
                tlog.getTransactionId(), tlog.getAccountNumber());
        
        TransactionLog savedLog = logRepository.save(tlog);
        
        log.debug("Transaction log created: {}", savedLog.getLogId());
        return savedLog;
    }
    
    @Transactional(readOnly = true)
    public List<TransactionLog> getLogsByTransaction(String transactionId) {
        return logRepository.findByTransactionIdOrderByTimestampDesc(transactionId);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionLog> getLogsByTransaction(String transactionId, Pageable pageable) {
        return logRepository.findByTransactionId(transactionId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionLog> getLogsByAccount(String accountId, Pageable pageable) {
        return logRepository.findByAccountId(accountId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionLog> getLogsByAccountNumber(String accountNumber, Pageable pageable) {
        return logRepository.findByAccountNumber(accountNumber, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionLog> getLogsWithFilters(String transactionId, String accountId, 
                                                  TransactionDirection direction, LocalDateTime fromDate, 
                                                  LocalDateTime toDate, Pageable pageable) {
        return logRepository.findLogsWithFilters(transactionId, accountId, direction, fromDate, toDate, pageable);
    }
}