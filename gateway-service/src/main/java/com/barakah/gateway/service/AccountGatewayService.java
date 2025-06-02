package com.barakah.gateway.service;

import com.barakah.account.proto.v1.*;
import com.barakah.gateway.dto.account.*;
import com.barakah.gateway.mapper.AccountMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountGatewayService {

    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountServiceStub;

    private final AccountMapper accountMapper;

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackListUserAccounts")
    @Retry(name = "account-service")
    public Page<AccountResponseDto> listUserAccounts(Pageable pageable) {
        try {
            String userId = getCurrentUserId();
            ListAccountsRequest request = ListAccountsRequest.newBuilder()
                    .setUserId(userId)
                    .setPageRequest(createPageRequest(pageable))
                    .build();

            ListAccountsResponse response = accountServiceStub.listAccounts(request);
            List<AccountResponseDto> accounts = response.getAccountsList().stream()
                    .map(accountMapper::toDto)
                    .toList();

            return new PageImpl<>(accounts, pageable, response.getPageResponse().getTotalElements());
        } catch (Exception e) {
            log.error("Failed to list user accounts", e);
            throw new RuntimeException("Failed to list accounts", e);
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackGetAccount")
    @Retry(name = "account-service")
    public AccountResponseDto getAccount(String accountId, PageRequest pageRequest) {
        try {
            GetAccountRequest request = GetAccountRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();
            GetAccountResponse response = accountServiceStub.getAccount(request);
            return accountMapper.toDto(response.getAccount());
        } catch (Exception e) {
            log.error("Failed to get account: {}", accountId, e);
            throw new RuntimeException("Failed to get account: " + accountId, e);
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackCreateAccount")
    @Retry(name = "account-service")
    public AccountResponseDto createAccount(CreateAccountRequestDto request) {
        try {
            if (request.getUserId() == null) {
                request.setUserId(getCurrentUserId());
            }

            CreateAccountRequest grpcRequest = accountMapper.toGrpcCreateRequest(request);
            CreateAccountResponse response = accountServiceStub.createAccount(grpcRequest);
            return accountMapper.toDto(response.getAccount());
        } catch (Exception e) {
            log.error("Failed to create account", e);
            throw new RuntimeException("Failed to create account", e);
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackCreateAccount")
    @Retry(name = "account-service")
    public CreateAccountResponse createAccountGrpc(CreateAccountRequest request) {
        try {
            if (request.getUserId().isEmpty()) {
                request = CreateAccountRequest.newBuilder()
                        .setUserId(getCurrentUserId())
                        .build();
            }

            return accountServiceStub.createAccount(request);
        } catch (Exception e) {
            log.error("Failed to create account", e);
            throw new RuntimeException("Failed to create account", e);
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackGetBalance")
    @Retry(name = "account-service")
    public BalanceResponseDto getBalance(String accountId) {
        try {
            GetBalanceRequest request = GetBalanceRequest.newBuilder()
                    .setAccountNumber(accountId)
                    .build();
            GetBalanceResponse response = accountServiceStub.getBalance(request);
            return accountMapper.toBalanceDto(response);
        } catch (Exception e) {
            log.error("Failed to get balance for account: {}", accountId, e);
            throw new RuntimeException("Failed to get balance", e);
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackCreditAccount")
    @Retry(name = "account-service")
    public AccountResponseDto creditAccount(String accountId, CreditDebitRequestDto request) {
        try {
            CreditAccountRequest grpcRequest = accountMapper.toCreditRequest(accountId, request);
            CreditAccountResponse response = accountServiceStub.creditAccount(grpcRequest);
//            return AccountResponseDto.builder().
//                    accountId(response.getAccountId())
//                    .accountNumber(response.getAccount().getAccountNumber())
//                    .accountType(response.getAccount().getAccountType().toString())
//                    .userId(response.getAccount().getUserId())
//                    .accountName(response.getAccount().getAccountName())
//                    .balance(new BigDecimal(response.getAccount().getBalance()))
//                    .currency("IDR")
//                    .status(response.getAccount().getStatus().toString())
//                    .build();
            return AccountResponseDto.builder().build();
        } catch (Exception e) {
            log.error("Failed to credit account: {}", accountId, e);
            throw new RuntimeException("Failed to credit account", e);
        }
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackDebitAccount")
    @Retry(name = "account-service")
    public AccountResponseDto debitAccount(String accountId, CreditDebitRequestDto request) {
        try {
            DebitAccountRequest grpcRequest = accountMapper.toDebitRequest(accountId, request);
            DebitAccountResponse response = accountServiceStub.debitAccount(grpcRequest);

            return AccountResponseDto.builder().build();
//            return accountMapper.toDto(response.getAccount());
        } catch (Exception e) {
            log.error("Failed to debit account: {}", accountId, e);
            throw new RuntimeException("Failed to debit account", e);
        }
    }

    // Helper methods
    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private com.barakah.common.proto.v1.PageRequest createPageRequest(Pageable pageable) {
        com.barakah.common.proto.v1.PageRequest.Builder builder =
                com.barakah.common.proto.v1.PageRequest.newBuilder()
                        .setPage(pageable.getPageNumber())
                        .setSize(pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                builder.setSort(order.getProperty());
                builder.setDirection(order.getDirection().name());
            });
        }

        return builder.build();
    }

    // Fallback methods
    public Page<AccountResponseDto> fallbackListUserAccounts(Pageable pageable, Exception ex) {
        log.warn("Fallback: Failed to list user accounts", ex);
        throw new RuntimeException("Account service is currently unavailable", ex);
    }

    public AccountResponseDto fallbackGetAccount(String accountId, Exception ex) {
        log.warn("Fallback: Failed to get account: {}", accountId, ex);
        throw new RuntimeException("Account service is currently unavailable", ex);
    }

    public AccountResponseDto fallbackCreateAccount(CreateAccountRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to create account", ex);
        throw new RuntimeException("Account service is currently unavailable", ex);
    }

    public BalanceResponseDto fallbackGetBalance(String accountId, Exception ex) {
        log.warn("Fallback: Failed to get balance for account: {}", accountId, ex);
        throw new RuntimeException("Account service is currently unavailable", ex);
    }

    public AccountResponseDto fallbackCreditAccount(String accountId, CreditDebitRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to credit account: {}", accountId, ex);
        throw new RuntimeException("Account service is currently unavailable", ex);
    }

    public AccountResponseDto fallbackDebitAccount(String accountId, CreditDebitRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to debit account: {}", accountId, ex);
        throw new RuntimeException("Account service is currently unavailable", ex);
    }
}