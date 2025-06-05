package com.barakah.transaction.config;

import com.barakah.shared.exception.AuthExceptions;
import com.barakah.transaction.exception.InsufficientFundsException;
import com.barakah.transaction.exception.TransactionExceptions;
import com.barakah.transaction.exception.TransactionNotFoundException;
import io.grpc.Status;
import io.grpc.StatusException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

@Slf4j
@GrpcAdvice
public class TransactionGrpcAdvice {

    @GrpcExceptionHandler
    public StatusException handleTransactionNotFound(TransactionExceptions.TransactionNotFoundException ex) {
        log.warn("Transaction not found: {}", ex.getMessage());
        return Status.NOT_FOUND
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleInvalidTransaction(TransactionExceptions.InvalidTransactionException ex) {
        log.warn("Invalid transaction: {}", ex.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleInsufficientBalance(TransactionExceptions.InsufficientBalanceException ex) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleInvalidTransactionStatus(TransactionExceptions.InvalidTransactionStatusException ex) {
        log.warn("Invalid transaction status change: {}", ex.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleAccountNotFound(TransactionExceptions.AccountNotFoundException ex) {
        log.warn("Account not found: {}", ex.getMessage());
        return Status.NOT_FOUND
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleSameAccountTransfer(TransactionExceptions.SameAccountTransferException ex) {
        log.warn("Same account transfer attempted: {}", ex.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleAccountNotActive(TransactionExceptions.AccountNotActiveException ex) {
        log.warn("Account not active: {}", ex.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleTransactionProcessing(TransactionExceptions.TransactionProcessingException ex) {
        log.error("Transaction processing error: {}", ex.getMessage(), ex);
        return Status.INTERNAL
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleDuplicateTransaction(TransactionExceptions.DuplicateTransactionException ex) {
        log.warn("Duplicate transaction: {}", ex.getMessage());
        return Status.ALREADY_EXISTS
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleCategoryNotFound(TransactionExceptions.CategoryNotFoundException ex) {
        log.warn("Category not found: {}", ex.getMessage());
        return Status.NOT_FOUND
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleCategoryInUse(TransactionExceptions.CategoryInUseException ex) {
        log.warn("Category in use: {}", ex.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleSystemCategory(TransactionExceptions.SystemCategoryException ex) {
        log.warn("System category operation not allowed: {}", ex.getMessage());
        return Status.PERMISSION_DENIED
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Insufficient funds error: {}", ex.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription("Insufficient funds: " + ex.getMessage())
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleTransactionNotFound(TransactionNotFoundException ex) {
        log.warn("Transaction not found: {}", ex.getMessage());
        return Status.NOT_FOUND
                .withDescription("Transaction not found: " + ex.getMessage())
                .withCause(ex)
                .asException();
    }
}
