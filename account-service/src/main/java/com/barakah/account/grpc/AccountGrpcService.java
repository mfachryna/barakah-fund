package com.barakah.account.grpc;

import com.barakah.account.dto.AccountResponse;
import com.barakah.account.enums.AccountType;
import com.barakah.account.mapper.AccountMapper;
import com.barakah.account.proto.v1.*;
import com.barakah.account.service.AccountService;
import com.barakah.shared.context.UserContext;
import com.barakah.shared.context.UserContextHolder;
import com.barakah.shared.exception.AuthExceptions;
import com.barakah.common.proto.v1.PageResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {

        UserContext currentUser = UserContextHolder.getContext();
        log.info("Creating account for user: {} by: {}", request.getUserId(),
                currentUser != null ? currentUser.getUsername() : "system");


        if (currentUser == null) {
            throw new AuthExceptions.InvalidTokenException("You need to login to create wallet");
        }


        validateCreateAccountRequest(request);


        com.barakah.account.dto.CreateAccountRequest createRequest =
                com.barakah.account.dto.CreateAccountRequest.builder()
                        .userId(request.getUserId())
                        .accountType(accountMapper.protoToAccountType(request.getAccountType()))
                        .accountName(request.getAccountName())
                        .initialDeposit(accountMapper.longToBigDecimal(request.getInitialDeposit()))
                        .build();


        if (!currentUser.getRoles().contains("ADMIN")) {
            createRequest.setUserId(currentUser.getUserId());
        }


        AccountResponse result = accountService.createAccount(createRequest);


        CreateAccountResponse response = CreateAccountResponse.newBuilder()
                .setAccount(buildAccountProto(result))
                .setMessage("Account created successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully created account: {} for user: {}", result.getId(), result.getUserId());
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
        UserContext currentUser = UserContextHolder.getContext();
        log.info("Getting account: {} by: {}", request.getAccountId(),
                currentUser != null ? currentUser.getUsername() : "system");


        if (request.getAccountId().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be empty");
        }


        AccountResponse account = accountService.getAccount(request.getAccountId());


        if (currentUser != null && !currentUser.getRoles().contains("ADMIN")
                && !currentUser.getUserId().equals(account.getUserId())) {
            throw new SecurityException("You can only view your own accounts");
        }


        GetAccountResponse response = GetAccountResponse.newBuilder()
                .setAccount(buildAccountProto(account))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully returned account: {}", request.getAccountId());
    }
    @Override
    public void getAccountByNumber(GetAccountByNumberRequest request, StreamObserver<GetAccountResponse> responseObserver) {
        UserContext currentUser = UserContextHolder.getContext();
        log.info("Getting account by number: {} by: {}", request.getNumber(),
                currentUser != null ? currentUser.getUsername() : "system");

        if (request.getNumber().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be empty");
        }


        AccountResponse account = accountService.getAccountByAccountNumber(request.getNumber());

        if (currentUser != null && !currentUser.getRoles().contains("ADMIN")
                && !currentUser.getUserId().equals(account.getUserId())) {
            throw new SecurityException("You can only view your own accounts");
        }


        GetAccountResponse response = GetAccountResponse.newBuilder()
                .setAccount(buildAccountProto(account))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully returned account: {}", request.getNumber());
    }


    @Override
    public void listAccounts(ListAccountsRequest request, StreamObserver<ListAccountsResponse> responseObserver) {

        UserContext currentUser = UserContextHolder.getContext();

        log.info("📋 Listing accounts for user: {} requested by: {}",
                request.getUserId(),
                currentUser != null ? currentUser.getUsername() : "anonymous");

        String targetUserId = request.getUserId();
        if (currentUser != null && !currentUser.isAdmin() && !currentUser.isService()) {
            targetUserId = currentUser.getUserId();
        }

        int page = Math.max(request.getPageRequest().getPage(), 1);
        int size = request.getPageRequest().getSize();

        if (size <= 0) {
            size = 10;
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }

        Pageable pageable = PageRequest.of(
                page > 0 ? page - 1 : 0,
                size
        );


        Page<AccountResponse> accountPage = accountService.listAccounts(targetUserId, pageable);


        ListAccountsResponse.Builder responseBuilder = ListAccountsResponse.newBuilder();

        for (AccountResponse accountDto : accountPage.getContent()) {
            responseBuilder.addAccounts(buildAccountProto(accountDto));
        }


        PageResponse pageResponse = PageResponse.newBuilder()
                .setPage(accountPage.getNumber() + 1)
                .setSize(accountPage.getSize())
                .setTotalElements(accountPage.getTotalElements())
                .setTotalPages(accountPage.getTotalPages())
                .setFirst(accountPage.isFirst())
                .setLast(accountPage.isLast())
                .build();

        responseBuilder.setPageResponse(pageResponse);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();

        log.info("Successfully returned {} accounts for user: {}",
                accountPage.getNumberOfElements(), targetUserId);
    }
    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
        UserContext currentUser = UserContextHolder.getContext();
        log.info("Getting balance for account: {} by: {}", request.getAccountNumber(),
                currentUser != null ? currentUser.getUsername() : "system");


        if (request.getAccountNumber().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }


        AccountResponse account = accountService.getAccountByAccountNumber(request.getAccountNumber());


        if (currentUser != null && !currentUser.getRoles().contains("ADMIN")
                && !currentUser.getUserId().equals(account.getUserId())) {
            throw new SecurityException("You can only view your own account balance");
        }


        GetBalanceResponse response = GetBalanceResponse.newBuilder()
                .setBalance(accountMapper.bigDecimalToLong(account.getBalance()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully returned balance for account: {}", request.getAccountNumber());
    }

    @Override
    public void updateBalance(UpdateBalanceRequest request, StreamObserver<UpdateBalanceResponse> responseObserver) {
        UserContext currentUser = UserContextHolder.getContext();
        log.info("Updating balance for account: {} by: {}", request.getAccountNumber(),
                currentUser != null ? currentUser.getUsername() : "system");


        if (currentUser == null) {
            throw new AuthExceptions.InvalidTokenException("Authentication required");
        }

        if (!currentUser.getRoles().contains("ADMIN")) {
            throw new SecurityException("Only administrators can update account balance");
        }


        validateUpdateBalanceRequest(request);


        BigDecimal amount = accountMapper.longToBigDecimal(request.getAmount());
        AccountResponse updatedAccount = accountService.updateBalance(
                request.getAccountNumber(),
                amount,
                request.getOperation(),
                request.getTransactionId()
        );


        UpdateBalanceResponse response = UpdateBalanceResponse.newBuilder()
                .setSuccess(true)
                .setNewBalance(accountMapper.bigDecimalToLong(updatedAccount.getBalance()))
                .setMessage("Balance updated successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully updated balance for account: {} by: {}",
                request.getAccountNumber(), currentUser.getUsername());
    }


    private Account buildAccountProto(AccountResponse accountDto) {
        Account.Builder builder = Account.newBuilder()
                .setAccountId(accountDto.getId())
                .setUserId(accountDto.getUserId())
                .setAccountType(accountMapper.accountTypeToProto(accountDto.getAccountType()))
                .setAccountName(accountDto.getAccountName())
                .setBalance(accountMapper.bigDecimalToLong(accountDto.getBalance()))
                .setStatus(accountMapper.accountStatusToProtoStatus(accountDto.getStatus()));


        if (accountDto.getAccountNumber() != null) {
            builder.setAccountNumber(accountDto.getAccountNumber());
        }

        return builder.build();
    }

    private void validateCreateAccountRequest(CreateAccountRequest request) {
        if (request.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        if (request.getAccountName().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }

        if (request.getAccountName().length() > 100) {
            throw new IllegalArgumentException("Account name cannot exceed 100 characters");
        }

        if (request.getInitialDeposit() < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }


        try {
            accountMapper.protoToAccountType(request.getAccountType());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid account type: " + request.getAccountType());
        }
    }

    /**
     * Validate UpdateBalanceRequest
     */
    private void validateUpdateBalanceRequest(UpdateBalanceRequest request) {
        if (request.getAccountNumber().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }

        if (request.getAmount() == 0) {
            throw new IllegalArgumentException("Amount cannot be zero");
        }

        if (request.getAmount() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        if (request.getTransactionId().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be empty");
        }

        if (!request.getOperation().equals("DEBIT") && !request.getOperation().equals("CREDIT")) {
            throw new IllegalArgumentException("Operation must be either 'DEBIT' or 'CREDIT'");
        }
    }
}
