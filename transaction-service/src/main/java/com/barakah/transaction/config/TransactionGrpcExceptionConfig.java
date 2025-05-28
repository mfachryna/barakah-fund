package com.barakah.user.config;

import com.barakah.transaction.exception.TransactionExceptions;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@Slf4j
@GrpcAdvice
public class TransactionGrpcExceptionConfig {
    @GrpcExceptionHandler(TransactionExceptions.TransactionNotFoundException.class)
    public StatusRuntimeException handleTransactionNotFound(TransactionExceptions.TransactionNotFoundException e) {
        log.warn("Transaction not found: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(TransactionExceptions.InvalidTransactionException.class)
    public StatusRuntimeException handleInvalidTransaction(TransactionExceptions.InvalidTransactionException e) {
        log.warn("Invalid transaction: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(TransactionExceptions.InsufficientBalanceException.class)
    public StatusRuntimeException handleInsufficientBalance(TransactionExceptions.InsufficientBalanceException e) {
        log.warn("Insufficient balance: {}", e.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(TransactionExceptions.InvalidTransactionStatusException.class)
    public StatusRuntimeException handleInvalidTransactionStatus(TransactionExceptions.InvalidTransactionStatusException e) {
        log.warn("Invalid transaction status: {}", e.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(TransactionExceptions.AccountNotFoundException.class)
    public StatusRuntimeException handleAccountNotFound(TransactionExceptions.AccountNotFoundException e) {
        log.warn("Account not found: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(TransactionExceptions.TransactionProcessingException.class)
    public StatusRuntimeException handleTransactionProcessing(TransactionExceptions.TransactionProcessingException e) {
        log.error("Transaction processing error: {}", e.getMessage());
        return Status.INTERNAL
                .withDescription("Transaction processing failed")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(TransactionExceptions.CategoryNotFoundException.class)
    public StatusRuntimeException handleCategoryNotFound(TransactionExceptions.CategoryNotFoundException e) {
        log.warn("Category not found: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(TransactionExceptions.CategoryInUseException.class)
    public StatusRuntimeException handleCategoryInUse(TransactionExceptions.CategoryInUseException e) {
        log.warn("Category in use: {}", e.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException();
    }
}