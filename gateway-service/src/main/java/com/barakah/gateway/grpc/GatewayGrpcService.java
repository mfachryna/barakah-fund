package com.barakah.gateway.grpc;

import com.barakah.common.proto.v1.Status;
import com.barakah.gateway.proto.v1.*;
import com.barakah.gateway.service.*;
import com.barakah.common.proto.v1.Empty;
import com.barakah.auth.proto.v1.*;
import com.barakah.user.proto.v1.*;
import com.barakah.account.proto.v1.*;
import com.barakah.transaction.proto.v1.*;
import com.barakah.shared.context.UserContext;
import com.barakah.shared.context.UserContextHolder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GatewayGrpcService extends GatewayServiceGrpc.GatewayServiceImplBase {

    private final AuthGatewayService authGatewayService;
    private final UserGatewayService userGatewayService;
    private final AccountGatewayService accountGatewayService;
    private final TransactionGatewayService transactionGatewayService;

    // ========== AUTH OPERATIONS ==========
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

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            log.info("gRPC Gateway: Register request for user: {}", request.getUsername());

            RegisterResponse response = authGatewayService.register(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Registration failed for user: {}", request.getUsername(), e);
            responseObserver.onError(e);
        }
    }

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

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
        try {
            log.debug("gRPC Gateway: Refresh token request");

            RefreshTokenResponse response = authGatewayService.refreshToken(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Gateway: Token refresh failed", e);
            responseObserver.onError(e);
        }
    }

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

    // ========== USER OPERATIONS ==========
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

    // ========== ACCOUNT OPERATIONS ==========
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

//    @Override
//    public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
//        try {
//            log.info("gRPC Gateway: Get account request for ID: {}", request.getAccountId());
//
//            GetAccountResponse response = accountGatewayService.getAccountById(request.getAccountId());
//
//            responseObserver.onNext(response);
//            responseObserver.onCompleted();
//        } catch (Exception e) {
//            log.error("gRPC Gateway: Get account failed for ID: {}", request.getAccountId(), e);
//            responseObserver.onError(e);
//        }
//    }

    // ========== TRANSACTION OPERATIONS ==========
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

    // ========== GATEWAY-SPECIFIC OPERATIONS ==========
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