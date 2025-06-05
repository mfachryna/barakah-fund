package com.barakah.gateway.service;

import com.barakah.account.proto.v1.AccountServiceGrpc;
import com.barakah.common.proto.v1.Empty;
import com.barakah.gateway.dto.health.ServiceHealthDto;
import com.barakah.transaction.proto.v1.TransactionServiceGrpc;
import com.barakah.user.proto.v1.UserServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class HealthCheckService {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountServiceStub;

    @GrpcClient("transaction-service")
    private TransactionServiceGrpc.TransactionServiceBlockingStub transactionServiceStub;

    public Map<String, ServiceHealthDto> checkAllServices() {
        Map<String, ServiceHealthDto> healthMap = new HashMap<>();

        CompletableFuture<ServiceHealthDto> userHealthFuture
                = CompletableFuture.supplyAsync(this::checkUserService);
        CompletableFuture<ServiceHealthDto> accountHealthFuture
                = CompletableFuture.supplyAsync(this::checkAccountService);
        CompletableFuture<ServiceHealthDto> transactionHealthFuture
                = CompletableFuture.supplyAsync(this::checkTransactionService);

        try {

            CompletableFuture.allOf(userHealthFuture, accountHealthFuture, transactionHealthFuture)
                    .get(5, TimeUnit.SECONDS);

            healthMap.put("user-service", userHealthFuture.get());
            healthMap.put("account-service", accountHealthFuture.get());
            healthMap.put("transaction-service", transactionHealthFuture.get());
        } catch (Exception e) {
            log.error("Health check failed", e);

            if (!userHealthFuture.isDone()) {
                healthMap.put("user-service", createFailedHealth("Health check timeout"));
            }
            if (!accountHealthFuture.isDone()) {
                healthMap.put("account-service", createFailedHealth("Health check timeout"));
            }
            if (!transactionHealthFuture.isDone()) {
                healthMap.put("transaction-service", createFailedHealth("Health check timeout"));
            }
        }

        return healthMap;
    }

    private ServiceHealthDto checkUserService() {
        long startTime = System.currentTimeMillis();
        try {

            userServiceStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getCurrentUser(Empty.newBuilder().build());

            long responseTime = System.currentTimeMillis() - startTime;
            return ServiceHealthDto.builder()
                    .status("UP")
                    .message("Service is healthy")
                    .responseTime(responseTime)
                    .lastChecked(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.debug("User service health check failed: {}", e.getMessage());
            return ServiceHealthDto.builder()
                    .status("DOWN")
                    .message("Service unavailable: " + e.getMessage())
                    .responseTime(responseTime)
                    .lastChecked(LocalDateTime.now())
                    .build();
        }
    }

    private ServiceHealthDto checkAccountService() {
        long startTime = System.currentTimeMillis();
        try {

            accountServiceStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .listAccounts(com.barakah.account.proto.v1.ListAccountsRequest.getDefaultInstance());

            long responseTime = System.currentTimeMillis() - startTime;
            return ServiceHealthDto.builder()
                    .status("UP")
                    .message("Service is healthy")
                    .responseTime(responseTime)
                    .lastChecked(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.debug("Account service health check failed: {}", e.getMessage());
            return ServiceHealthDto.builder()
                    .status("DOWN")
                    .message("Service unavailable: " + e.getMessage())
                    .responseTime(responseTime)
                    .lastChecked(LocalDateTime.now())
                    .build();
        }
    }

    private ServiceHealthDto checkTransactionService() {
        long startTime = System.currentTimeMillis();
        try {

            transactionServiceStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .listTransactions(com.barakah.transaction.proto.v1.ListTransactionsRequest.getDefaultInstance());

            long responseTime = System.currentTimeMillis() - startTime;
            return ServiceHealthDto.builder()
                    .status("UP")
                    .message("Service is healthy")
                    .responseTime(responseTime)
                    .lastChecked(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.debug("Transaction service health check failed: {}", e.getMessage());
            return ServiceHealthDto.builder()
                    .status("DOWN")
                    .message("Service unavailable: " + e.getMessage())
                    .responseTime(responseTime)
                    .lastChecked(LocalDateTime.now())
                    .build();
        }
    }

    private ServiceHealthDto createFailedHealth(String message) {
        return ServiceHealthDto.builder()
                .status("DOWN")
                .message(message)
                .responseTime(0L)
                .lastChecked(LocalDateTime.now())
                .build();
    }
}
