package com.barakah.account.service;

import com.barakah.account.entity.Account;
import com.barakah.account.entity.AccountBalanceHistory;
import com.barakah.account.enums.BalanceOperation;
import com.barakah.account.exception.AccountExceptions;
import com.barakah.account.repository.AccountRepository;
import com.barakah.account.repository.AccountBalanceHistoryRepository;
import com.barakah.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BalanceUpdateService {

    private final AccountRepository accountRepository;
    private final AccountBalanceHistoryRepository balanceHistoryRepository;

    @Transactional
    public Account updateBalanceFromTransaction(TransactionEvent event) {
        log.info("Processing balance update from transaction: {} for accounts: {} -> {}",
                event.getTransactionId(), event.getFromAccountNumber(), event.getToAccountNumber());

        Account updatedAccount = null;

        if (event.getFromAccountId() != null && event.getFromAccountNumber() != null) {
            updatedAccount = processAccountBalanceUpdate(
                    event.getFromAccountId(),
                    event.getFromAccountNumber(),
                    event.getAmount(),
                    BalanceOperation.DEBIT,
                    event.getTransactionId(),
                    event.getDescription(),
                    event.getBalanceBeforeFrom(),
                    event.getBalanceAfterFrom()
            );
        }


        if (event.getToAccountId() != null && event.getToAccountNumber() != null) {
            Account targetAccount = processAccountBalanceUpdate(
                    event.getToAccountId(),
                    event.getToAccountNumber(),
                    event.getAmount(),
                    BalanceOperation.CREDIT,
                    event.getTransactionId(),
                    event.getDescription(),
                    event.getBalanceBeforeTo(),
                    event.getBalanceAfterTo()
            );


            if (updatedAccount == null) {
                updatedAccount = targetAccount;
            }
        }

        return updatedAccount;
    }

    private Account processAccountBalanceUpdate(String accountId, String accountNumber, BigDecimal amount,
                                                BalanceOperation operation, String transactionId, String description,
                                                BigDecimal balanceBefore, BigDecimal balanceAfter) {

        log.debug("Processing {} of {} for account: {}", operation, amount, accountNumber);


        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountExceptions.AccountNotFoundException(accountId));

        if (!account.getAccountNumber().equals(accountNumber)) {
            throw new AccountExceptions.InvalidAccountException(
                    "Account ID and number mismatch: " + accountId + " vs " + accountNumber);
        }

        if (!account.getStatus().toString().equals("ACTIVE")) {
            throw new AccountExceptions.AccountNotActiveException(accountNumber);
        }
        BigDecimal previousBalance = account.getBalance();


        BigDecimal newBalance;
        switch (operation) {
            case CREDIT -> {
                newBalance = previousBalance.add(amount);
                log.debug("Crediting {} to account {}: {} + {} = {}",
                        amount, accountNumber, previousBalance, amount, newBalance);
            }
            case DEBIT -> {

                if (previousBalance.compareTo(amount) < 0) {
                    throw new AccountExceptions.InsufficientBalanceException(
                            accountNumber, previousBalance.toString(), amount.toString());
                }
                newBalance = previousBalance.subtract(amount);
                log.debug("Debiting {} from account {}: {} - {} = {}",
                        amount, accountNumber, previousBalance, amount, newBalance);
            }
            default -> throw new IllegalArgumentException("Unknown balance operation: " + operation);
        }


        if (balanceBefore != null && !balanceBefore.equals(previousBalance)) {
            log.warn("Balance mismatch detected for account {}: expected {}, actual {}",
                    accountNumber, balanceBefore, previousBalance);

        }

        if (balanceAfter != null && !balanceAfter.equals(newBalance)) {
            log.warn("Expected balance mismatch for account {}: calculated {}, expected {}",
                    accountNumber, newBalance, balanceAfter);

        }


        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        Account updatedAccount = accountRepository.save(account);


        createBalanceHistory(account, operation, amount, previousBalance, newBalance, transactionId, description);

        log.info("Balance updated for account {}: {} -> {} ({})",
                accountNumber, previousBalance, newBalance, operation);

        return updatedAccount;
    }

    private void createBalanceHistory(Account account, BalanceOperation operation, BigDecimal amount,
                                      BigDecimal balanceBefore, BigDecimal balanceAfter,
                                      String transactionId, String description) {

        AccountBalanceHistory history = AccountBalanceHistory.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .operation(operation)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .transactionId(transactionId)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();

        balanceHistoryRepository.save(history);

        log.debug("Balance history created for account {}: {} {} {}",
                account.getAccountNumber(), operation, amount, transactionId);
    }
}