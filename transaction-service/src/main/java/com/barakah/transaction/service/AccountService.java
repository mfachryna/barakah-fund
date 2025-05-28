package com.barakah.transaction.service;

import com.barakah.transaction.client.AccountServiceClient;
import com.barakah.transaction.dto.AccountInfo;
import com.barakah.transaction.exception.TransactionExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountServiceClient accountServiceClient;

    public AccountInfo getAndValidateAccount(String accountNumber) {
        try {
            AccountInfo account = accountServiceClient.getAccountByNumber(accountNumber);
            if (account == null) {
                throw new TransactionExceptions.AccountNotFoundException(accountNumber);
            }

            if (!"ACTIVE".equals(account.getStatus())) {
                throw new TransactionExceptions.AccountNotActiveException(accountNumber);
            }

            return account;
        } catch (Exception e) {
            if (e instanceof TransactionExceptions.AccountNotFoundException
                    || e instanceof TransactionExceptions.AccountNotActiveException) {
                throw e;
            }
            log.error("Error fetching account {}: {}", accountNumber, e.getMessage());
            throw new TransactionExceptions.AccountNotFoundException(accountNumber);
        }
    }

    public boolean hasAccountAccess(String accountId, String userId) {
        return accountServiceClient.hasAccountAccess(accountId, userId);
    }
}