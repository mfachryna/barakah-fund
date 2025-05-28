package com.barakah.account.config;

import com.barakah.account.exception.AccountExceptions;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@Slf4j
@GrpcAdvice
public class ExceptionConfig {
    @GrpcExceptionHandler(AccountExceptions.AccountNotFoundException.class)
    public StatusRuntimeException handleAccountNotFound(AccountExceptions.AccountNotFoundException e) {
        log.warn("Account not found: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AccountExceptions.AccountAlreadyExistsException.class)
    public StatusRuntimeException handleAccountAlreadyExists(AccountExceptions.AccountAlreadyExistsException e) {
        log.warn("Account already exists: {}", e.getMessage());
        return Status.ALREADY_EXISTS
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AccountExceptions.InsufficientBalanceException.class)
    public StatusRuntimeException handleInsufficientBalance(AccountExceptions.InsufficientBalanceException e) {
        log.warn("Insufficient balance: {}", e.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AccountExceptions.InvalidAccountOperationException.class)
    public StatusRuntimeException handleInvalidAccountOperation(AccountExceptions.InvalidAccountOperationException e) {
        log.warn("Invalid account operation: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AccountExceptions.AccountStatusException.class)
    public StatusRuntimeException handleAccountStatus(AccountExceptions.AccountStatusException e) {
        log.warn("Account status error: {}", e.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AccountExceptions.DuplicateTransactionException.class)
    public StatusRuntimeException handleDuplicateTransaction(AccountExceptions.DuplicateTransactionException e) {
        log.warn("Duplicate transaction: {}", e.getMessage());
        return Status.ALREADY_EXISTS
                .withDescription(e.getMessage())
                .asRuntimeException();
    }
}