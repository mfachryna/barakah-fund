package com.barakah.account.service;

import com.barakah.account.dto.AccountResponse;
import com.barakah.account.dto.CreateAccountRequest;
import com.barakah.account.entity.Account;
import com.barakah.account.enums.AccountStatus;
import com.barakah.account.enums.AccountType;
import com.barakah.account.exception.AccountExceptions;
import com.barakah.account.mapper.AccountMapper;
import com.barakah.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.NotActiveException;
import java.math.BigDecimal;
import java.nio.channels.AcceptPendingException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for user: {}, type: {}", request.getUserId(), request.getAccountType());

        validateAccountCreation(request);

        String accountNumber = generateAccountNumber(request.getAccountType());

        Account account = Account.builder()
                .userId(request.getUserId())
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .accountName(request.getAccountName())
                .balance(request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        try {
            Account savedAccount = accountRepository.save(account);
            log.info("Account created successfully: {}", savedAccount.getAccountNumber());
            return accountMapper.toResponse(savedAccount);
        } catch (Exception e) {
            log.error("Failed to save account: {}", e.getMessage());
            throw new RuntimeException("Failed to create account: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        log.info("Getting account: {}", accountId);

        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be empty");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountExceptions.AccountNotFoundException("ID", accountId));

        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public Account getAccountById(String accountId) {
        log.info("Getting account entity: {}", accountId);

        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be empty");
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountExceptions.AccountNotFoundException("ID", accountId));

    }

    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        log.info("Getting account entity by number: {}", accountNumber);

        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }

        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountExceptions.AccountNotFoundException("account number", accountNumber));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        log.info("Getting account by number: {}", accountNumber);

        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountExceptions.AccountNotFoundException("account number", accountNumber));

        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAccounts(String userId, Pageable pageable) {
        log.info("Listing accounts for user: {}", userId);

        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }

        Page<Account> accounts;
        if (userId != null && !userId.trim().isEmpty()) {
            accounts = accountRepository.findByUserId(userId, pageable);
        } else {
            accounts = accountRepository.findAll(pageable);
        }

        return accounts.map(accountMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountId) {
        log.info("Getting balance for account: {}", accountId);

        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be empty");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountExceptions.AccountNotFoundException("ID", accountId));

        return account.getBalance();
    }

    public AccountResponse updateBalance(String accountNumber, BigDecimal amount, String operation, String transactionId) {
        log.info("Updating balance for account: {}, operation: {}, amount: {}, transactionId: {}",
                accountNumber, operation, amount, transactionId);

        validateUpdateBalanceRequest(accountNumber, amount, operation, transactionId);

        if (isDuplicateTransaction(transactionId)) {
            throw new AccountExceptions.DuplicateTransactionException(transactionId);
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountExceptions.AccountNotFoundException("account number", accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountExceptions.AccountStatusException(
                    accountNumber,
                    account.getStatus().toString(),
                    operation
            );
        }

        performBalanceOperation(account, amount, operation);

        try {
            Account savedAccount = accountRepository.save(account);
            log.info("Balance updated successfully for account: {}, new balance: {}",
                    accountNumber, savedAccount.getBalance());
            return accountMapper.toResponse(savedAccount);
        } catch (Exception e) {
            log.error("Failed to update balance for account {}: {}", accountNumber, e.getMessage());
            throw new RuntimeException("Failed to update account balance: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Account updateAccountBalance(String accountNumber, BigDecimal amount, String operation,
            String transactionId, String description) {
        log.info("Updating account balance: {} {} {}", accountNumber, operation, amount);

        Account account = getAccountByNumber(accountNumber);

        if (!account.getStatus().equals("ACTIVE")) {
            throw new AccountExceptions.AccountNotActiveException(accountNumber);
        }

        BigDecimal previousBalance = account.getBalance();
        BigDecimal newBalance;

        if ("CREDIT".equals(operation)) {
            newBalance = previousBalance.add(amount);
        } else if ("DEBIT".equals(operation)) {
            if (previousBalance.compareTo(amount) < 0) {
                throw new AccountExceptions.InsufficientBalanceException(
                        accountNumber, previousBalance.toString(), amount.toString());
            }
            newBalance = previousBalance.subtract(amount);
        } else {
            throw new IllegalArgumentException("Invalid operation: " + operation);
        }

        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        Account updatedAccount = accountRepository.save(account);

        log.info("Account balance updated: {} from {} to {}", accountNumber, previousBalance, newBalance);
        return updatedAccount;
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(String accountId) {
        Account account = getAccountById(accountId);
        return account.getBalance();
    }

    @Transactional(readOnly = true)
    public boolean validateAccount(String accountNumber) {
        try {
            Account account = getAccountByNumber(accountNumber);
            return "ACTIVE".equals(account.getStatus());
        } catch (AccountExceptions.AccountNotFoundException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean accountExists(String userId, AccountType accountType) {
        return accountRepository.existsByUserIdAndAccountTypeAndStatus(userId, accountType, AccountStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean accountExistsByNumber(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }

    private void validateAccountCreation(CreateAccountRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create account request cannot be null");
        }

        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        if (request.getAccountType() == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }

        if (request.getAccountName() == null || request.getAccountName().trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }

        if (request.getAccountType() == AccountType.SAVINGS || request.getAccountType() == AccountType.SPENDING) {
            boolean exists = accountRepository.existsByUserIdAndAccountTypeAndStatus(
                    request.getUserId(), request.getAccountType(), AccountStatus.ACTIVE);

            if (exists) {
                throw new AccountExceptions.AccountAlreadyExistsException(
                        request.getUserId(),
                        request.getAccountType().toString()
                );
            }
        }

        if (request.getInitialDeposit() != null && request.getInitialDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }
    }

    private void validateUpdateBalanceRequest(String accountNumber, BigDecimal amount, String operation, String transactionId) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }

        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (operation == null || operation.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation cannot be empty");
        }

        if (!operation.equalsIgnoreCase("CREDIT") && !operation.equalsIgnoreCase("DEBIT")) {
            throw new AccountExceptions.InvalidAccountOperationException(
                    operation,
                    "Operation must be either 'CREDIT' or 'DEBIT'"
            );
        }

        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be empty");
        }
    }

    private void performBalanceOperation(Account account, BigDecimal amount, String operation) {
        switch (operation.toUpperCase()) {
            case "CREDIT":
                account.credit(amount);
                log.debug("Credited {} to account {}, new balance: {}",
                        amount, account.getAccountNumber(), account.getBalance());
                break;

            case "DEBIT":

                if (account.getBalance().compareTo(amount) < 0) {
                    throw new AccountExceptions.InsufficientBalanceException(
                            account.getAccountNumber(),
                            account.getBalance().toString(),
                            amount.toString()
                    );
                }
                account.debit(amount);
                log.debug("Debited {} from account {}, new balance: {}",
                        amount, account.getAccountNumber(), account.getBalance());
                break;

            default:
                throw new AccountExceptions.InvalidAccountOperationException(
                        operation,
                        "Unsupported operation"
                );
        }
    }

    private boolean isDuplicateTransaction(String transactionId) {

        return false;
    }

    private String generateAccountNumber(AccountType accountType) {
        String prefix = getAccountTypePrefix(accountType);

        String accountNumber;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            if (attempts >= maxAttempts) {
                throw new RuntimeException("Failed to generate unique account number after " + maxAttempts + " attempts");
            }

            accountNumber = prefix + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
            attempts++;

        } while (accountRepository.existsByAccountNumber(accountNumber));

        log.debug("Generated account number: {} after {} attempts", accountNumber, attempts);
        return accountNumber;
    }

    private String getAccountTypePrefix(AccountType accountType) {
        return switch (accountType) {
            case SAVINGS ->
                "SAV";
            case SPENDING ->
                "SPE";
            default ->
                "ACC";
        };
    }
}
