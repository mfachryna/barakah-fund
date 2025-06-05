package com.barakah.gateway.service;

import com.barakah.account.proto.v1.*;
import com.barakah.gateway.dto.account.*;
import com.barakah.gateway.mapper.AccountMapper;
import com.barakah.shared.util.GrpcErrorHandler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import com.barakah.shared.annotation.RateLimit;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountGatewayService extends BaseService {

    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountServiceStub;

    private final AccountMapper accountMapper;


    @RateLimit(endpoint = "gateway-list-accounts")
    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackListUserAccounts")
    @Retry(name = "account-service")
    @RateLimiter(name = "gateway-queries")
    @Cacheable(value = "gateway-user-accounts", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + T(com.barakah.shared.context.UserContextHolder).getCurrentUserId()")
    public Page<AccountResponseDto> listUserAccounts(Pageable pageable) {
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
    }

    @RateLimit(endpoint = "gateway-get-account")
    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackGetAccount")
    @Retry(name = "account-service")
    @RateLimiter(name = "gateway-queries")
    @Cacheable(value = "gateway-accounts", key = "#accountId")
    public AccountResponseDto getAccount(String accountId, PageRequest pageRequest) {
        GetAccountRequest request = GetAccountRequest.newBuilder()
                .setAccountId(accountId)
                .build();
        GetAccountResponse response = accountServiceStub.getAccount(request);
        return accountMapper.toDto(response.getAccount());
    }

    @RateLimit(endpoint = "gateway-create-account")
    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackCreateAccount")
    @Retry(name = "account-service")
    @RateLimiter(name = "gateway-mutations")
    @Bulkhead(name = "gateway-account-creation")
    @CacheEvict(value = {"gateway-user-accounts", "gateway-accounts"}, allEntries = true)
    public AccountResponseDto createAccount(CreateAccountRequestDto request) {
        if (request.getUserId() == null) {
            request.setUserId(getCurrentUserId());
        }

        CreateAccountRequest grpcRequest = accountMapper.toGrpcCreateRequest(request);
        CreateAccountResponse response = accountServiceStub.createAccount(grpcRequest);
        return accountMapper.toDto(response.getAccount());
    }

    @RateLimit(endpoint = "gateway-get-balance")
    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackGetBalance")
    @Retry(name = "account-service")
    @RateLimiter(name = "gateway-queries")
    @Cacheable(value = "gateway-balances", key = "#accountId", 
               condition = "#accountId != null")
    public BalanceResponseDto getBalance(String accountId) {
        GetBalanceRequest request = GetBalanceRequest.newBuilder()
                .setAccountNumber(accountId)
                .build();
        GetBalanceResponse response = accountServiceStub.getBalance(request);
        return accountMapper.toBalanceDto(response);
    }

    @RateLimit(endpoint = "gateway-credit-account")
    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackCreditAccount")
    @Retry(name = "account-service")
    @RateLimiter(name = "gateway-financial")
    @Bulkhead(name = "gateway-financial-operations")
    @CacheEvict(value = {"gateway-balances", "gateway-accounts"}, key = "#accountId")
    public AccountResponseDto creditAccount(String accountId, CreditDebitRequestDto request) {
        CreditAccountRequest grpcRequest = accountMapper.toCreditRequest(accountId, request);
        CreditAccountResponse response = accountServiceStub.creditAccount(grpcRequest);

        return AccountResponseDto.builder()
                .accountId(accountId)
                .balance(BigDecimal.valueOf(response.getNewBalance()))
                .build();
    }

    @RateLimit(endpoint = "gateway-debit-account")
    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackDebitAccount")
    @Retry(name = "account-service")
    @RateLimiter(name = "gateway-financial")
    @Bulkhead(name = "gateway-financial-operations")
    @CacheEvict(value = {"gateway-balances", "gateway-accounts"}, key = "#accountId")
    public AccountResponseDto debitAccount(String accountId, CreditDebitRequestDto request) {
        DebitAccountRequest grpcRequest = accountMapper.toDebitRequest(accountId, request);
        DebitAccountResponse response = accountServiceStub.debitAccount(grpcRequest);
        return AccountResponseDto.builder()
                .accountId(accountId)
                .balance(BigDecimal.valueOf(response.getNewBalance()))
                .build();

    }


    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackCreateAccountGrpc")
    @Retry(name = "account-service")
    public CreateAccountResponse createAccountGrpc(CreateAccountRequest request) {
        if (request.getUserId().isEmpty()) {
            request = CreateAccountRequest.newBuilder()
                    .mergeFrom(request)
                    .setUserId(getCurrentUserId())
                    .build();
        }
        return accountServiceStub.createAccount(request);
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackGetAccountGrpc")
    @Retry(name = "account-service")
    public GetAccountResponse getAccountGrpc(GetAccountRequest request) {
        return accountServiceStub.getAccount(request);
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackListAccountsGrpc")
    @Retry(name = "account-service")
    public ListAccountsResponse getListAccountsGrpc(ListAccountsRequest request) {
        return accountServiceStub.listAccounts(request);
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackGetBalanceGrpc")
    @Retry(name = "account-service")
    public GetBalanceResponse getBalanceGrpc(GetBalanceRequest request) {
        return accountServiceStub.getBalance(request);
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackCreditAccountGrpc")
    @Retry(name = "account-service")
    public CreditAccountResponse creditAccountGrpc(CreditAccountRequest request) {
        return accountServiceStub.creditAccount(request);
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "fallbackDebitAccountGrpc")
    @Retry(name = "account-service")
    public DebitAccountResponse debitAccountGrpc(DebitAccountRequest request) {
        return accountServiceStub.debitAccount(request);
    }


    public Page<AccountResponseDto> fallbackListUserAccounts(Pageable pageable, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "list user accounts", ex);
        return null;
    }

    public AccountResponseDto fallbackGetAccount(String accountId, PageRequest pageRequest, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "get account: " + accountId, ex);
        return null;
    }

    public AccountResponseDto fallbackCreateAccount(CreateAccountRequestDto request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Account",
                "create account",
                "Account service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }

    public BalanceResponseDto fallbackGetBalance(String accountId, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "get balance: " + accountId, ex);
        return null;
    }

    public AccountResponseDto fallbackCreditAccount(String accountId, CreditDebitRequestDto request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "credit account: " + accountId, ex);
        return null;
    }

    public AccountResponseDto fallbackDebitAccount(String accountId, CreditDebitRequestDto request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "debit account: " + accountId, ex);
        return null;
    }


    public CreateAccountResponse fallbackCreateAccountGrpc(CreateAccountRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "create account gRPC", ex);
        return null;
    }

    public GetAccountResponse fallbackGetAccountGrpc(GetAccountRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "get account gRPC: " + request.getAccountId(), ex);
        return null;
    }

    public ListAccountsResponse fallbackListAccountsGrpc(ListAccountsRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "list accounts gRPC", ex);
        return null;
    }

    public GetBalanceResponse fallbackGetBalanceGrpc(GetBalanceRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "get balance gRPC: " + request.getAccountNumber(), ex);
        return null;
    }

    public CreditAccountResponse fallbackCreditAccountGrpc(CreditAccountRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "credit account gRPC: " + request.getAccountId(), ex);
        return null;
    }

    public DebitAccountResponse fallbackDebitAccountGrpc(DebitAccountRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Account", "debit account gRPC: " + request.getAccountId(), ex);
        return null;
    }
}