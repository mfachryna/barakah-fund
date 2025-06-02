package com.barakah.gateway.service;

import com.barakah.gateway.dto.transaction.*;
import com.barakah.gateway.mapper.TransactionMapper;
import com.barakah.transaction.proto.v1.*;
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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionGatewayService {

    @GrpcClient("transaction-service")
    private TransactionServiceGrpc.TransactionServiceBlockingStub transactionServiceStub;

    private final TransactionMapper transactionMapper;

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackListTransactions")
    @Retry(name = "transaction-service")
    public Page<TransactionResponseDto> listTransactions(
            Map<String, String> filters,
            String search,
            Pageable pageable) {
        try {
            String userId = getCurrentUserId();
            ListTransactionsRequest.Builder builder = ListTransactionsRequest.newBuilder()
                    .setPageRequest(createPageRequest(pageable));

            if (search != null && !search.trim().isEmpty()) {
                builder.setSearch(search);
            }

            if (filters != null) {
                filters.forEach(builder::putFilters);
            }

            ListTransactionsRequest request = builder.build();
            ListTransactionsResponse response = transactionServiceStub.listTransactions(request);

            List<TransactionResponseDto> transactions = response.getTransactionsList().stream()
                    .map(transactionMapper::toDto)
                    .toList();

            return new PageImpl<>(transactions, pageable, response.getPageResponse().getTotalElements());
        } catch (Exception e) {
            log.error("Failed to list transactions", e);
            throw new RuntimeException("Failed to list transactions", e);
        }
    }

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackGetTransaction")
    @Retry(name = "transaction-service")
    public TransactionResponseDto getTransaction(String transactionId, PageRequest pageRequest) {
        try {
            GetTransactionRequest request = GetTransactionRequest.newBuilder()
                    .setTransactionId(transactionId)
                    .build();
            GetTransactionResponse response = transactionServiceStub.getTransaction(request);
            return transactionMapper.toDto(response.getTransaction());
        } catch (Exception e) {
            log.error("Failed to get transaction: {}", transactionId, e);
            throw new RuntimeException("Failed to get transaction: " + transactionId, e);
        }
    }

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackCreateTransaction")
    @Retry(name = "transaction-service")
    public TransactionResponseDto createTransaction(CreateTransactionRequestDto request) {
        try {
            CreateTransactionRequest grpcRequest = transactionMapper.toGrpcCreateRequest(request);
            CreateTransactionResponse response = transactionServiceStub.createTransaction(grpcRequest);
            return transactionMapper.toDto(response.getTransaction());
        } catch (Exception e) {
            log.error("Failed to create transaction", e);
            throw new RuntimeException("Failed to create transaction", e);
        }
    }

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackCreateTransaction")
    @Retry(name = "transaction-service")
    public CreateTransactionResponse createTransactionGrpc(CreateTransactionRequest request) {
        try {
            return transactionServiceStub.createTransaction(request);
        } catch (Exception e) {
            log.error("Failed to create transaction", e);
            throw new RuntimeException("Failed to create transaction", e);
        }
    }

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackTransfer")
    @Retry(name = "transaction-service")
    public TransactionResponseDto transfer(TransferRequestDto request) {
        try {
            CreateTransactionRequest grpcRequest = transactionMapper.toGrpcTransferRequest(request);
            CreateTransactionResponse response = transactionServiceStub.createTransaction(grpcRequest);
            return transactionMapper.toDto(response.getTransaction());
        } catch (Exception e) {
            log.error("Failed to transfer money", e);
            throw new RuntimeException("Failed to transfer money", e);
        }
    }

//    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackListCategories")
//    @Retry(name = "transaction-service")
//    public Page<TransactionCategoryResponseDto> listCategories(Pageable pageable) {
//        try {
//            ListCategoriesRequest request = ListCategoriesRequest.newBuilder()
//                    .setPageRequest(createPageRequest(pageable))
//                    .build();
//            ListCategoriesResponse response = transactionServiceStub(request);
//
//            List<TransactionCategoryResponseDto> categories = response.getCategoriesList().stream()
//                    .map(transactionMapper::toCategoryDto)
//                    .toList();
//
//            return new PageImpl<>(categories, pageable, response.getPageResponse().getTotalElements());
//        } catch (Exception e) {
//            log.error("Failed to list categories", e);
//            throw new RuntimeException("Failed to list categories", e);
//        }
//    }

//    @CircuitBreaker(name = "transaction-service", fallbackMethod = "fallbackCreateCategory")
//    @Retry(name = "transaction-service")
//    public TransactionCategoryResponseDto createCategory(CreateCategoryRequestDto request) {
//        try {
//            CreateCategoryRequest grpcRequest = transactionMapper.toGrpcCategoryRequest(request);
//            CreateCategoryResponse response = transactionServiceStub.createCategory(grpcRequest);
//            return transactionMapper.toCategoryDto(response.getCategory());
//        } catch (Exception e) {
//            log.error("Failed to create category", e);
//            throw new RuntimeException("Failed to create category", e);
//        }
//    }

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
    public Page<TransactionResponseDto> fallbackListTransactions(
            Map<String, String> filters, String search, Pageable pageable, Exception ex) {
        log.warn("Fallback: Failed to list transactions", ex);
        throw new RuntimeException("Transaction service is currently unavailable", ex);
    }

    public TransactionResponseDto fallbackGetTransaction(String transactionId, Exception ex) {
        log.warn("Fallback: Failed to get transaction: {}", transactionId, ex);
        throw new RuntimeException("Transaction service is currently unavailable", ex);
    }

    public TransactionResponseDto fallbackCreateTransaction(CreateTransactionRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to create transaction", ex);
        throw new RuntimeException("Transaction service is currently unavailable", ex);
    }

    public TransactionResponseDto fallbackTransfer(TransferRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to transfer money", ex);
        throw new RuntimeException("Transaction service is currently unavailable", ex);
    }

    public Page<TransactionCategoryResponseDto> fallbackListCategories(Pageable pageable, Exception ex) {
        log.warn("Fallback: Failed to list categories", ex);
        throw new RuntimeException("Transaction service is currently unavailable", ex);
    }

    public TransactionCategoryResponseDto fallbackCreateCategory(CreateCategoryRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to create category", ex);
        throw new RuntimeException("Transaction service is currently unavailable", ex);
    }
}