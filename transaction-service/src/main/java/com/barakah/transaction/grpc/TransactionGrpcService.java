package com.barakah.transaction.grpc;

import com.barakah.common.proto.v1.*;
import com.barakah.transaction.entity.Transaction;
import com.barakah.transaction.enums.TransactionDirection;
import com.barakah.transaction.mapper.TransactionMapper;
import com.barakah.transaction.proto.v1.*;
import com.barakah.transaction.service.TransactionService;
import com.barakah.shared.context.UserContextHolder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.barakah.shared.annotation.RateLimit;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionService transactionService;
    private final TransactionMapper mapper;

    @RateLimit(endpoint = "grpc-create-transaction")
    @Bulkhead(name = "transaction-creation")
    @RateLimiter(name = "transaction-creation")
    @CacheEvict(value = {"user-transactions", "account-transactions", "user-statistics"}, allEntries = true)
    @Override
    public void createTransaction(CreateTransactionRequest request, StreamObserver<CreateTransactionResponse> responseObserver) {
        log.info("Creating transaction: {} from {} to {} amount {}",
                request.getType(), request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());

        Transaction transaction = transactionService.createTransaction(
                mapper.mapTransactionType(request.getType()),
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                convertToBigDecimal(request.getAmount()),
                request.getDescription(),
                request.getNotes(),
                request.getCategoryId(),
                request.getExternalReference(),
                request.getExternalProvider()
        );

        CreateTransactionResponse response = CreateTransactionResponse.newBuilder()
                .setTransaction(mapper.toProto(transaction))
                .setMessage("Transaction created successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void getTransaction(GetTransactionRequest request, StreamObserver<GetTransactionResponse> responseObserver) {
        log.debug("Getting transaction: {}", request.getTransactionId());

        Transaction transaction = transactionService.getTransactionByIdWithCategory(request.getTransactionId());

        GetTransactionResponse response = GetTransactionResponse.newBuilder()
                .setTransaction(mapper.toProto(transaction))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @RateLimit(endpoint = "grpc-list-transactions")
    @Cacheable(value = "user-transactions", 
        key = "T(com.barakah.shared.context.UserContextHolder).getCurrentUserId() + '_grpc_' + #request.pageRequest.page + '_' + #request.pageRequest.size + '_' + #request.filtersMap.hashCode() + '_' + #request.search")
    @Retry(name = "database")
    @RateLimiter(name = "transaction-queries")
    @Override
    public void listTransactions(ListTransactionsRequest request, StreamObserver<ListTransactionsResponse> responseObserver) {
        log.debug("Listing transactions with filters");

        Pageable pageable = createPageable(request.getPageRequest());

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;

        String userId = UserContextHolder.getCurrentUserId();

        Page<Transaction> transactionPage = transactionService.listTransactions(
                userId, request.getFiltersMap(),request.getSearch(), pageable);

        ListTransactionsResponse response = ListTransactionsResponse.newBuilder()
                .addAllTransactions(mapper.toProtoList(transactionPage.getContent()))
                .setPageResponse(createPageResponse(transactionPage))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @RateLimit(endpoint = "grpc-update-transaction")
    @Retry(name = "database")
    @RateLimiter(name = "transaction-processing")
    @Caching(evict = {
        @CacheEvict(value = "transactions", key = "#request.transactionId"),
        @CacheEvict(value = "user-transactions", allEntries = true),
        @CacheEvict(value = "account-transactions", allEntries = true),
        @CacheEvict(value = "user-statistics", allEntries = true)
    })
    @Override
    public void updateTransactionStatus(UpdateTransactionStatusRequest request, StreamObserver<UpdateTransactionStatusResponse> responseObserver) {
        log.info("Updating transaction status: {} to {}", request.getTransactionId(), request.getStatus());

        Transaction transaction = transactionService.updateTransactionStatus(
                request.getTransactionId(),
                mapper.mapTransactionStatus(request.getStatus()),
                request.getNotes()
        );

        UpdateTransactionStatusResponse response = UpdateTransactionStatusResponse.newBuilder()
                .setTransaction(mapper.toProto(transaction))
                .setMessage("Transaction status updated successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @RateLimit(endpoint = "grpc-get-account-transactions")
    @Cacheable(value = "account-transactions", 
        key = "#request.accountNumber + '_grpc_' + #request.direction + '_' + #request.pageRequest.page + '_' + #request.fromDate + '_' + #request.toDate")
    @Retry(name = "database")
    @RateLimiter(name = "transaction-queries")
    @Override
    public void getTransactionsByAccount(GetTransactionsByAccountRequest request, StreamObserver<GetTransactionsByAccountResponse> responseObserver) {

        log.debug("Getting transactions by account: {}", request.getAccountNumber());

        Pageable pageable = createPageable(request.getPageRequest());

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;

        if (!request.getFromDate().isEmpty()) {
            fromDate = LocalDateTime.parse(request.getFromDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        if (!request.getToDate().isEmpty()) {
            toDate = LocalDateTime.parse(request.getToDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        TransactionDirection direction = mapper.mapTransactionDirection(request.getDirection());

        Page<Transaction> transactionPage = transactionService.getTransactionsByAccount(
                request.getAccountNumber(), fromDate, toDate, direction, pageable);

        GetTransactionsByAccountResponse response = GetTransactionsByAccountResponse.newBuilder()
                .addAllTransactions(mapper.toProtoList(transactionPage.getContent()))
                .setPageResponse(createPageResponse(transactionPage))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @RateLimit(endpoint = "grpc-get-transaction-logs")
    @Cacheable(value = "transaction-logs", 
        key = "#request.pageRequest.page + '_' + #request.pageRequest.size + '_logs'")
    @Retry(name = "database")
    @RateLimiter(name = "transaction-queries")
    @Override
    public void getTransactionLogs(GetTransactionLogsRequest request, StreamObserver<GetTransactionLogsResponse> responseObserver) {

        log.debug("Getting transaction logs");

        GetTransactionLogsResponse response = GetTransactionLogsResponse.newBuilder()
                .setPageResponse(PageResponse.newBuilder()
                        .setPage(0)
                        .setSize(0)
                        .setTotalElements(0)
                        .setTotalPages(0)
                        .setFirst(true)
                        .setLast(true)
                        .build())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    // Helper methods
    private Pageable createPageable(PageRequest pageRequest) {
        if (pageRequest == null) {
            return org.springframework.data.domain.PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (!pageRequest.getSort().isEmpty()) {
            Sort.Direction direction =
                    "ASC".equalsIgnoreCase(pageRequest.getDirection()) ?
                            Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, pageRequest.getSort());
        }

        return org.springframework.data.domain.PageRequest.of(
                pageRequest.getPage() < 0 ? pageRequest.getPage() : 0,
                pageRequest.getSize() == 0 ? Math.min(pageRequest.getSize(), 100) : 20,
                sort
        );
    }

    private PageResponse createPageResponse(Page<?> page) {
        return PageResponse.newBuilder()
                .setPage(page.getNumber())
                .setSize(page.getSize())
                .setTotalElements(page.getTotalElements())
                .setTotalPages(page.getTotalPages())
                .setFirst(page.isFirst())
                .setLast(page.isLast())
                .build();
    }

    private BigDecimal convertToBigDecimal(long amount) {
        return BigDecimal.valueOf(amount);
    }
}