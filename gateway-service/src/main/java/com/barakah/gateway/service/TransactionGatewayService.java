package com.barakah.gateway.service;

import com.barakah.common.proto.v1.PageRequest;
import com.barakah.gateway.dto.transaction.*;
import com.barakah.gateway.mapper.TransactionMapper;
import com.barakah.shared.annotation.RateLimit;
import com.barakah.shared.util.GrpcErrorHandler;
import com.barakah.transaction.proto.v1.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionGatewayService {

    @GrpcClient("transaction-service")
    private TransactionServiceGrpc.TransactionServiceBlockingStub transactionServiceStub;

    @GrpcClient("transaction-service")
    private TransactionCategoryServiceGrpc.TransactionCategoryServiceBlockingStub transactionCategoryServiceStub;

    private final TransactionMapper transactionMapper;

    @RateLimit(endpoint = "gateway-create-transaction")
    @RateLimiter(name = "gateway-financial")
    @Bulkhead(name = "gateway-transaction-creation")
    @CacheEvict(value = {"gateway-transactions", "gateway-account-transactions"}, allEntries = true)
    public TransactionResponseDto createTransaction(CreateTransactionRequestDto requestDto) {
        CreateTransactionRequest grpcRequest = transactionMapper.toGrpcCreateRequest(requestDto);
        CreateTransactionResponse grpcResponse = createTransactionGrpc(grpcRequest);
        return transactionMapper.toDto(grpcResponse);
    }

    @RateLimit(endpoint = "gateway-get-transaction")
    @RateLimiter(name = "gateway-queries")
    @Cacheable(value = "gateway-transactions", key = "#transactionId")
    public TransactionResponseDto getTransaction(String transactionId) {
        GetTransactionRequest grpcRequest = GetTransactionRequest.newBuilder()
                .setTransactionId(transactionId)
                .build();
        GetTransactionResponse grpcResponse = getTransactionGrpc(grpcRequest);
        return transactionMapper.toDto(grpcResponse.getTransaction());
    }

    @RateLimit(endpoint = "gateway-get-account-transactions")
    @RateLimiter(name = "gateway-queries")
    @Cacheable(value = "gateway-account-transactions", 
               key = "#accountId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<TransactionResponseDto> getTransactionsByAccount(String accountId, Pageable pageable) {
        GetTransactionsByAccountRequest grpcRequest = GetTransactionsByAccountRequest.newBuilder()
                .setAccountNumber(accountId)
                .setPageRequest(createPageRequest(pageable))
                .build();

        GetTransactionsByAccountResponse grpcResponse = getTransactionsByAccountGrpc(grpcRequest);

        List<TransactionResponseDto> transactions = grpcResponse.getTransactionsList()
                .stream()
                .map(transactionMapper::toDto)
                .toList();

        return new PageImpl<>(
                transactions,
                pageable,
                grpcResponse.getPageResponse().getTotalElements()
        );
    }

    @RateLimit(endpoint = "gateway-list-transactions")
    @RateLimiter(name = "gateway-queries")
    @Cacheable(value = "gateway-user-transactions", 
               key = "#root.target.getCurrentUserId() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<TransactionResponseDto> listTransactions(Pageable pageable) {
        ListTransactionsRequest grpcRequest = ListTransactionsRequest.newBuilder()
                .setPageRequest(createPageRequest(pageable))
                .build();

        ListTransactionsResponse grpcResponse = listTransactionsGrpc(grpcRequest);

        List<TransactionResponseDto> transactions = grpcResponse.getTransactionsList()
                .stream()
                .map(transactionMapper::toDto)
                .toList();

        return new PageImpl<>(
                transactions,
                pageable,
                grpcResponse.getPageResponse().getTotalElements()
        );
    }

    @RateLimit(endpoint = "gateway-get-category")
    @RateLimiter(name = "gateway-queries")
    @Cacheable(value = "gateway-categories", key = "#categoryId")
    public TransactionCategoryResponseDto getCategory(String categoryId) {
        GetCategoryRequest grpcRequest = GetCategoryRequest.newBuilder()
                .setCategoryId(categoryId)
                .build();

        GetCategoryResponse grpcResponse = getCategoryGrpc(grpcRequest);
        return transactionMapper.toCategoryDto(grpcResponse.getCategory());
    }

    @RateLimit(endpoint = "gateway-list-categories")
    @RateLimiter(name = "gateway-queries")
    @Cacheable(value = "gateway-category-lists", 
               key = "#includeSystem + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<TransactionCategoryResponseDto> listCategories(boolean includeSystem, Pageable pageable) {
        ListCategoriesRequest grpcRequest = ListCategoriesRequest.newBuilder()
                .setPageRequest(createPageRequest(pageable))
                .setIncludeSystem(includeSystem)
                .build();
        ListCategoriesResponse grpcResponse = getListCategoryGrpc(grpcRequest);

        List<TransactionCategoryResponseDto> categories = grpcResponse.getCategoriesList()
                .stream()
                .map(transactionMapper::toCategoryDto)
                .toList();

        return new PageImpl<>(
                categories,
                pageable,
                grpcResponse.getPageResponse().getTotalElements()
        );
    }

    @RateLimit(endpoint = "grpc-create-transaction")
    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackCreateTransaction")
    @Retry(name = "transaction-service")
    @RateLimiter(name = "gateway-financial")
    @Bulkhead(name = "gateway-transaction-creation")
    public CreateTransactionResponse createTransactionGrpc(CreateTransactionRequest request) {
        return transactionServiceStub.createTransaction(request);
    }

    @RateLimit(endpoint = "grpc-get-account-transactions")
    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackGetTransactionsByAccount")
    @Retry(name = "transaction-service")
    @RateLimiter(name = "gateway-queries")
    public GetTransactionsByAccountResponse getTransactionsByAccountGrpc(
            GetTransactionsByAccountRequest request) {
        return transactionServiceStub.getTransactionsByAccount(request);
    }

    @RateLimit(endpoint = "grpc-get-transaction")
    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackGetTransaction")
    @Retry(name = "transaction-service")
    @RateLimiter(name = "gateway-queries")
    public GetTransactionResponse getTransactionGrpc(GetTransactionRequest request) {
        return transactionServiceStub.getTransaction(request);
    }

    @RateLimit(endpoint = "grpc-list-transactions")
    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackListTransactions")
    @Retry(name = "transaction-service")
    @RateLimiter(name = "gateway-queries")
    public ListTransactionsResponse listTransactionsGrpc(ListTransactionsRequest request) {
        return transactionServiceStub.listTransactions(request);
    }

    @RateLimit(endpoint = "grpc-get-transaction-logs")
    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackGetTransactionLogs")
    @Retry(name = "transaction-service")
    @RateLimiter(name = "gateway-queries")
    public GetTransactionLogsResponse getTransactionLogGrpc(GetTransactionLogsRequest request) {
        return transactionServiceStub.getTransactionLogs(request);
    }

    @RateLimit(endpoint = "grpc-get-category")
    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackGetCategory")
    @Retry(name = "transaction-service")
    @RateLimiter(name = "gateway-queries")
    public GetCategoryResponse getCategoryGrpc(GetCategoryRequest request) {
        return transactionCategoryServiceStub.getCategory(request);
    }

    @RateLimit(endpoint = "grpc-list-categories")
    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackListCategories")
    @Retry(name = "transaction-service")
    @RateLimiter(name = "gateway-queries")
    public ListCategoriesResponse getListCategoryGrpc(ListCategoriesRequest request) {
        return transactionCategoryServiceStub.listCategories(request);
    }

    public CreateTransactionResponse fallbackCreateTransaction(CreateTransactionRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Transaction",
                "create transaction",
                "Transaction service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }

    public GetTransactionsByAccountResponse fallbackGetTransactionsByAccount(
            GetTransactionsByAccountRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Transaction",
                "get transactions for account: " + request.getAccountNumber(),
                ex
        );
        return null;
    }

    public GetTransactionResponse fallbackGetTransaction(GetTransactionRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Transaction",
                "get transaction: " + request.getTransactionId(),
                ex
        );
        return null;
    }

    public ListTransactionsResponse fallbackListTransactions(ListTransactionsRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Transaction", "list transactions", ex);
        return null;
    }

    public GetTransactionLogsResponse fallbackGetTransactionLogs(GetTransactionLogsRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Transaction",
                "get transaction logs: " + request.getTransactionId(),
                ex
        );
        return null;
    }

    public GetCategoryResponse fallbackGetCategory(GetCategoryRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "Transaction",
                "get category: " + request.getCategoryId(),
                ex
        );
        return null;
    }

    public ListCategoriesResponse fallbackListCategories(ListCategoriesRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError("Transaction", "list categories", ex);
        return null;
    }

    public String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                return jwt.getClaimAsString("sub");
            }
            return auth.getName();
        } catch (Exception e) {
            log.warn("Failed to get current user ID: {}", e.getMessage());
            return "anonymous";
        }
    }

    public com.barakah.common.proto.v1.PageRequest createPageRequest(Pageable pageable) {
        com.barakah.common.proto.v1.PageRequest.Builder builder
                = com.barakah.common.proto.v1.PageRequest.newBuilder()
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
}
