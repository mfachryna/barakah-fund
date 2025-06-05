package com.barakah.gateway.grpc;

import com.barakah.common.proto.v1.PageRequest;
import com.barakah.common.proto.v1.PageResponse;
import com.barakah.common.proto.v1.Status;
import com.barakah.gateway.dto.transaction.TransactionResponseDto;
import com.barakah.gateway.proto.v1.*;
import com.barakah.gateway.service.*;
import com.barakah.common.proto.v1.Empty;
import com.barakah.auth.proto.v1.*;
import com.barakah.shared.util.GrpcErrorHandler;
import com.barakah.user.proto.v1.*;
import com.barakah.account.proto.v1.*;
import com.barakah.transaction.proto.v1.*;
import com.barakah.shared.context.UserContext;
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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GatewayGrpcService extends GatewayServiceGrpc.GatewayServiceImplBase {

    private final AuthGatewayService authGatewayService;
    private final UserGatewayService userGatewayService;
    private final AccountGatewayService accountGatewayService;
    private final TransactionGatewayService transactionGatewayService;

    @RateLimit(endpoint = "gateway-grpc-login")
    @RateLimiter(name = "gateway-auth")
    @Bulkhead(name = "gateway-authentication")
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Login request for user: {}", request.getUsername());

            LoginResponse response = authGatewayService.loginGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Login failed for user: {}", request.getUsername(), e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-register")
    @RateLimiter(name = "gateway-auth")
    @Bulkhead(name = "gateway-authentication")
    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Register request for user: {}", request.getUsername());

            RegisterResponse response = authGatewayService.registerGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Registration failed for user: {}", request.getUsername(), e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-validate-token")
    @RateLimiter(name = "gateway-auth-validation")
    @Cacheable(value = "gateway-token-validation", key = "#request.token")
    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        try {
            log.debug("gRPC Gateway: Validate token request");

            ValidateTokenResponse response = authGatewayService.validateTokenGrpc(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Token validation failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-refresh-token")
    @RateLimiter(name = "gateway-auth")
    @Bulkhead(name = "gateway-authentication")
    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
        try {
            log.debug("gRPC Gateway: Refresh token request");

            RefreshTokenResponse response = authGatewayService.refreshTokenGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Token refresh failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-logout")
    @RateLimiter(name = "gateway-auth")
    @CacheEvict(value = "gateway-token-validation", allEntries = true)
    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Logout request");

            LogoutResponse response = authGatewayService.logoutGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Logout failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-get-current-user")
    @RateLimiter(name = "gateway-user-queries")
    @Cacheable(value = "gateway-grpc-current-user", key = "T(com.barakah.shared.context.UserContextHolder).getCurrentUserId()")
    @Override
    public void getCurrentUser(Empty request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get current user request");

            UserContext currentUser = UserContextHolder.getContext();
            if (currentUser == null) {
                throw new SecurityException("User context not found");
            }

            GetUserResponse response = userGatewayService.getCurrentUserGrpc();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Get current user failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-get-user")
    @RateLimiter(name = "gateway-user-queries")
    @Cacheable(value = "gateway-grpc-users", key = "#request.userId")
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get user request for ID: {}", request.getUserId());

            GetUserResponse response = userGatewayService.getUserByIdGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Get user failed for ID: {}", request.getUserId(), e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-create-account")

    @RateLimiter(name = "gateway-account-mutations")
    @Bulkhead(name = "gateway-account-creation")
    @CacheEvict(value = {"gateway-grpc-accounts", "gateway-grpc-account-lists"}, allEntries = true)
    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Create account request for user: {}", request.getUserId());

            var response = accountGatewayService.createAccountGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Create account failed for user: {}", request.getUserId(), e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-get-account")

    @RateLimiter(name = "gateway-account-queries")
    @Cacheable(value = "gateway-grpc-accounts", key = "#request.accountId")
    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get account request for: {}", request.getAccountId());

            GetAccountResponse response = accountGatewayService.getAccountGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Get account failed for : {}", request.getAccountId(), e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-list-accounts")

    @RateLimiter(name = "gateway-account-queries")
    @Cacheable(value = "gateway-grpc-account-lists",
            key = "#request.userId + '_' + #request.pageRequest.page + '_' + #request.pageRequest.size")
    @Override
    public void listAccounts(ListAccountsRequest request, StreamObserver<ListAccountsResponse> responseObserver) {
        try {
            UserContext currentUser = UserContextHolder.getContext();
            log.info("gRPC Gateway: Get account list for user: {}", currentUser.getUserId());

            ListAccountsResponse response = accountGatewayService.getListAccountsGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: List accounts failed for user: {}", request.getUserId(), e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-get-balance")

    @RateLimiter(name = "gateway-account-queries")
    @Cacheable(value = "gateway-grpc-balances", key = "#request.accountNumber")
    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get account balance for account: {}", request.getAccountNumber());

            GetBalanceResponse response = accountGatewayService.getBalanceGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Get account balance failed for account: {}", request.getAccountNumber(), e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-create-transaction")

    @RateLimiter(name = "gateway-transaction-mutations")
    @Bulkhead(name = "gateway-transaction-creation")
    @CacheEvict(value = {"gateway-grpc-transactions", "gateway-grpc-transaction-lists", "gateway-grpc-balances"}, allEntries = true)
    @Override
    public void createTransaction(CreateTransactionRequest request, StreamObserver<CreateTransactionResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Create transaction request");

            CreateTransactionResponse response = transactionGatewayService.createTransactionGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Create transaction failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-get-transaction")

    @RateLimiter(name = "gateway-transaction-queries")
    @Cacheable(value = "gateway-grpc-transactions", key = "#request.transactionId")
    @Override
    public void getTransaction(GetTransactionRequest request, StreamObserver<GetTransactionResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get transaction detail of: {}", request.getTransactionId());

            GetTransactionResponse response = transactionGatewayService.getTransactionGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Get transaction failed: {}", request.getTransactionId(), e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-list-transactions")

    @RateLimiter(name = "gateway-transaction-queries")
    @Cacheable(value = "gateway-grpc-transaction-lists",
            key = "T(com.barakah.shared.context.UserContextHolder).getCurrentUserId() + '_' + #request.pageRequest.page + '_' + #request.pageRequest.size")
    @Override
    public void listTransactions(ListTransactionsRequest request, StreamObserver<ListTransactionsResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get list transaction request");

            ListTransactionsResponse response = transactionGatewayService.listTransactionsGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: List transactions failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-get-transactions-by-account")

    @RateLimiter(name = "gateway-transaction-queries")
    @Cacheable(value = "gateway-grpc-account-transactions",
            key = "#request.accountNumber + '_' + #request.pageRequest.page + '_' + #request.pageRequest.size")
    @Override
    public void getTransactionsByAccount(GetTransactionsByAccountRequest request, StreamObserver<GetTransactionsByAccountResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get list transaction by account request");

            GetTransactionsByAccountResponse response = transactionGatewayService.getTransactionsByAccountGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Get transactions by account failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-get-transaction-logs")

    @RateLimiter(name = "gateway-transaction-queries")
    @Cacheable(value = "gateway-grpc-transaction-logs",
            key = "#request.pageRequest.page + '_' + #request.pageRequest.size")
    @Override
    public void getTransactionLogs(GetTransactionLogsRequest request, StreamObserver<GetTransactionLogsResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get transaction log request");

            GetTransactionLogsResponse response = transactionGatewayService.getTransactionLogGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Get transaction logs failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-get-category")

    @RateLimiter(name = "gateway-transaction-queries")
    @Cacheable(value = "gateway-grpc-categories", key = "#request.categoryId")
    @Override
    public void getCategory(GetCategoryRequest request, StreamObserver<GetCategoryResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get transaction category : {}", request.getCategoryId());

            GetCategoryResponse response = transactionGatewayService.getCategoryGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Get category failed", e);
            responseObserver.onError(e);
        }
    }

    @RateLimit(endpoint = "gateway-grpc-list-categories")

    @RateLimiter(name = "gateway-transaction-queries")
    @Cacheable(value = "gateway-grpc-category-lists",
            key = "#request.includeSystem + '_' + #request.pageRequest.page + '_' + #request.pageRequest.size")
    @Override
    public void listCategories(ListCategoriesRequest request, StreamObserver<ListCategoriesResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Get transaction category list");

            ListCategoriesResponse response = transactionGatewayService.getListCategoryGrpc(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: List categories failed", e);
            responseObserver.onError(e);
        }
    }

//    @Override
//    public void getUserDashboard(GetUserDashboardRequest request, StreamObserver<GetUserDashboardResponse> responseObserver) {
//        try {
//            log.info("gRPC Gateway: Get user dashboard for user: {}", request.getUserId());
//
//            // Aggregate data from multiple services
//            var userDto = userGatewayService.getUserById(request.getUserId());
//            var accountsPage = accountGatewayService.getAccount(request.getUserId(),
//                    org.springframework.data.domain.PageRequest.of(0, 10));
//            var transactionsPage = transactionGatewayService.getTransaction(request.getUserId(),
//                    org.springframework.data.domain.PageRequest.of(0, 10));
//
//            // Calculate aggregated data
//            var totalBalance = accountsPage.getContent().stream()
//                    .map(account -> account.getBalance())
//                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
//
//            var response = GetUserDashboardResponse.newBuilder()
//                    .setUser(convertToUserProto(userDto))
//                    .addAllAccounts(accountsPage.getContent().stream()
//                            .map(this::convertToAccountProto)
//                            .toList())
//                    .addAllRecentTransactions(transactionsPage.getContent().stream()
//                            .map(this::convertToTransactionProto)
//                            .toList())
//                    .setTotalBalance(totalBalance.toString())
//                    .setTotalAccounts((int) accountsPage.getTotalElements())
//                    .setPendingTransactions(0) // Implement logic as needed
//                    .build();
//
//            responseObserver.onNext(response);
//            responseObserver.onCompleted();
//        } catch (Exception e) {
//            log.error("gRPC Gateway: Get user dashboard failed for user: {}", request.getUserId(), e);
//            responseObserver.onError(e);
//        }
//    }
}
