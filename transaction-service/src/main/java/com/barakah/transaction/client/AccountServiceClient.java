package com.barakah.transaction.client;

import com.barakah.account.proto.v1.*;
import com.barakah.transaction.dto.AccountInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClient {

    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountServiceStub;

//    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackDebitAccount")
//    @Retry(name = "account-service")
//    public void debitAccount(String accountId, BigDecimal amount) {
//        try {
//            log.info("Debiting account {} with amount {}", accountId, amount);
//
//            DebitAccountRequest request = DebitAccountRequest.newBuilder()
//                    .setAccountId(accountId)
//                    .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
//                    .setDescription("Transfer debit")
//                    .build();
//
//            var response = accountServiceStub.debitAccount(request);
//
//            if (!response.getSuccess()) {
//                throw new RuntimeException("Failed to debit account: " + response.getMessage());
//            }
//
//            log.info("Successfully debited account {}", accountId);
//
//        } catch (Exception e) {
//            log.error("Failed to debit account {}", accountId, e);
//            throw new RuntimeException("Account debit failed", e);
//        }
//    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackCreditAccount")
    @Retry(name = "account-service")
    @RateLimiter(name = "account-service-calls")
    @CacheEvict(value = {"account-info", "balances"}, key = "#accountId")
    public void creditAccount(String accountId, BigDecimal amount) {
        try {
            log.info("Crediting account {} with amount {}", accountId, amount);

            CreditAccountRequest request = CreditAccountRequest.newBuilder()
                    .setAccountId(accountId)
                    .setAmount(amount.longValue())
                    .setDescription("Transfer credit")
                    .build();

            var response = accountServiceStub.creditAccount(request);

            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to credit account: " + response.getMessage());
            }

            log.info("Successfully credited account {}", accountId);

        } catch (Exception e) {
            log.error("Failed to credit account {}", accountId, e);
            throw new RuntimeException("Account credit failed", e);
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackAccountExists")
    @Retry(name = "account-service")
    @RateLimiter(name = "account-service-calls")
    @Cacheable(value = "account-existence", key = "#accountId")
    public boolean accountExists(String accountId) {
        try {
            GetAccountRequest request = GetAccountRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();

            var response = accountServiceStub.getAccount(request);
            return response.hasAccount();

        } catch (Exception e) {

            log.warn("Failed to check if account exists: {}", accountId, e);

            throw e;
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackHasAccountAccess")
    @Retry(name = "account-service")
    @RateLimiter(name = "account-service-calls")
    @Cacheable(value = "account-access", key = "#accountId + '_' + #userId")
    public boolean hasAccountAccess(String accountId, String userId) {
        try {
            CheckAccountAccessRequest request = CheckAccountAccessRequest.newBuilder()
                    .setAccountId(accountId)
                    .setUserId(userId)
                    .build();

            var response = accountServiceStub.checkAccountAccess(request);
            return response.getHasAccess();

        } catch (Exception e) {
            log.warn("Failed to check account access for user {} on account {}", userId, accountId, e);
            throw e;
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackGetAccountByNumber")
    @Retry(name = "account-service")
    @RateLimiter(name = "account-service-calls")
    @Cacheable(value = "account-info", key = "#accountId")
    public AccountInfo getAccountByNumber(String accountId) {
        try {
            GetAccountByNumberRequest request = GetAccountByNumberRequest.newBuilder()
                    .setNumber(accountId)
                    .build();

            GetAccountResponse accountResponse = accountServiceStub.getAccountByNumber(request);
            return AccountInfo
                    .builder()
                    .accountId(accountResponse.getAccount().getAccountId())
                    .accountNumber(accountResponse.getAccount().getAccountNumber())
                    .balance(BigDecimal.valueOf(accountResponse.getAccount().getBalance() / 100.0))
                    .accountType(accountResponse.getAccount().getAccountType().toString())
                    .status(accountResponse.getAccount().getStatus().toString())
                    .userId(accountResponse.getAccount().getUserId())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("Failed to check account access for account {}", accountId, e);
            throw e;
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackGetAccount")
    @Retry(name = "account-service")
    @RateLimiter(name = "account-service-calls")
    @Cacheable(value = "account-info", key = "#accountId")
    public AccountInfo getAccount(String accountId) {
        try {
            GetAccountRequest request = GetAccountRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();

            GetAccountResponse accountResponse = accountServiceStub.getAccount(request);
            return AccountInfo
                    .builder()
                    .accountId(accountResponse.getAccount().getAccountId())
                    .accountNumber(accountResponse.getAccount().getAccountNumber())
                    .balance(BigDecimal.valueOf(accountResponse.getAccount().getBalance() / 100.0))
                    .accountType(accountResponse.getAccount().getAccountType().toString())
                    .status(accountResponse.getAccount().getStatus().toString())
                    .userId(accountResponse.getAccount().getUserId())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("Failed to check account access for account {}", accountId, e);
            throw e;
        }
    }
//    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackHasAccountAccess")
//    @Retry(name = "account-service")
//    public AccountInfo getAccountById(String accountId) {
//        try {
//            GetAccountRequest request = GetAccountRequest.newBuilder()
//                    .setNumber(accountId)
//                    .build();
//
//            GetAccountResponse accountResponse =  accountServiceStub.getAccount(request);
//            return AccountInfo
//                    .builder()
//                    .accountId(accountResponse.getAccount().getAccountId())
//                    .accountNumber(accountResponse.getAccount().getAccountNumber())
//                    .balance(BigDecimal.valueOf(accountResponse.getAccount().getBalance() / 100.0))
//                    .accountType(accountResponse.getAccount().getAccountType().toString())
//                    .status(accountResponse.getAccount().getStatus().toString())
//                    .userId(accountResponse.getAccount().getUserId())
//                    .build();
//        } catch (Exception e) {
//            log.warn("Failed to check account access for account {}", accountId, e);
//            return null;
//        }
//    }

    public void fallbackDebitAccount(String accountId, BigDecimal amount, Exception ex) {
        log.error("Account service unavailable for debit operation on account: {}", accountId, ex);
        throw new RuntimeException("Account service unavailable", ex);
    }

    public void fallbackCreditAccount(String accountId, BigDecimal amount, Exception ex) {
        log.error("Account service unavailable for credit operation on account: {}", accountId, ex);
        throw new RuntimeException("Account service unavailable", ex);
    }

    public boolean fallbackAccountExists(String accountId, Exception ex) {
        log.warn("Account service unavailable, cannot verify account existence: {}", accountId, ex);
        return false;

    }

    public boolean fallbackHasAccountAccess(String accountId, String userId, Exception ex) {
        log.warn("Account service unavailable, denying account access for user {} on account {}", userId, accountId, ex);
        return false;
    }

    public AccountInfo fallbackGetAccountByNumber(String accountId, Exception ex) {
        log.error("Account service unavailable for getAccountByNumber: {}", accountId, ex);
        throw new RuntimeException("Account service unavailable", ex);
    }

    public AccountInfo fallbackGetAccount(String accountId, Exception ex) {
        log.error("Account service unavailable for getAccount: {}", accountId, ex);
        throw new RuntimeException("Account service unavailable", ex);
    }
}