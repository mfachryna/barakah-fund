package com.barakah.shared.config;

import com.barakah.shared.exception.AuthExceptions;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

import java.sql.SQLException;
import java.util.concurrent.TimeoutException;


@Slf4j
@GrpcAdvice
public class GrpcExceptionConfig {

    @GrpcExceptionHandler(AuthExceptions.InvalidCredentialsException.class)
    public StatusRuntimeException handleInvalidCredentials(AuthExceptions.InvalidCredentialsException e) {
        log.warn("Invalid credentials: {}", e.getMessage());
        return Status.UNAUTHENTICATED
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AuthExceptions.TokenExpiredException.class)
    public StatusRuntimeException handleTokenExpired(AuthExceptions.TokenExpiredException e) {
        log.warn("Token expired: {}", e.getMessage());
        return Status.UNAUTHENTICATED
                .withDescription("Token has expired, please login again")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AuthExceptions.InvalidTokenException.class)
    public StatusRuntimeException handleInvalidToken(AuthExceptions.InvalidTokenException e) {
        log.warn("Invalid token: {}", e.getMessage());
        return Status.UNAUTHENTICATED
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AuthExceptions.UserAlreadyExistsException.class)
    public StatusRuntimeException handleUserAlreadyExists(AuthExceptions.UserAlreadyExistsException e) {
        log.warn("User already exists: {}", e.getMessage());
        return Status.ALREADY_EXISTS
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AuthExceptions.RegistrationFailedException.class)
    public StatusRuntimeException handleRegistrationFailed(AuthExceptions.RegistrationFailedException e) {
        log.error("Registration failed: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Registration failed due to internal error")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AuthExceptions.AccountDisabledException.class)
    public StatusRuntimeException handleAccountDisabled(AuthExceptions.AccountDisabledException e) {
        log.warn("Account disabled: {}", e.getMessage());
        return Status.PERMISSION_DENIED
                .withDescription("Account is disabled. Please contact administrator.")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AuthExceptions.AccountLockedException.class)
    public StatusRuntimeException handleAccountLocked(AuthExceptions.AccountLockedException e) {
        log.warn("Account locked: {}", e.getMessage());
        return Status.PERMISSION_DENIED
                .withDescription("Account is temporarily locked. Please try again later or contact administrator.")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AuthExceptions.InsufficientPermissionsException.class)
    public StatusRuntimeException handleInsufficientPermissions(AuthExceptions.InsufficientPermissionsException e) {
        log.warn("Insufficient permissions: {}", e.getMessage());
        return Status.PERMISSION_DENIED
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(AuthExceptions.AuthenticationServiceException.class)
    public StatusRuntimeException handleAuthenticationService(AuthExceptions.AuthenticationServiceException e) {
        log.error("Authentication service error: {}", e.getMessage(), e);
        return Status.UNAVAILABLE
                .withDescription("Authentication service temporarily unavailable")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(IllegalStateException.class)
    public StatusRuntimeException handleIllegalState(IllegalStateException e) {
        log.warn("Invalid state: {}", e.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(SecurityException.class)
    public StatusRuntimeException handleSecurity(SecurityException e) {
        log.warn("Security violation: {}", e.getMessage());
        return Status.PERMISSION_DENIED
                .withDescription("Access denied")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public StatusRuntimeException handleJpaEntityNotFound(jakarta.persistence.EntityNotFoundException e) {
        log.warn("JPA Entity not found: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription("Requested resource not found")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(jakarta.persistence.EntityExistsException.class)
    public StatusRuntimeException handleJpaEntityExists(jakarta.persistence.EntityExistsException e) {
        log.warn("JPA Entity already exists: {}", e.getMessage());
        return Status.ALREADY_EXISTS
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public StatusRuntimeException handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException e) {
        log.error("Data integrity violation: {}", e.getMessage());
        
        String message = "Data constraint violation";
        if (e.getMessage() != null) {
            String errorMsg = e.getMessage().toLowerCase();
            if (errorMsg.contains("duplicate key") || errorMsg.contains("unique constraint")) {
                if (errorMsg.contains("username")) {
                    message = "Username already exists";
                } else if (errorMsg.contains("email")) {
                    message = "Email already exists";
                } else if (errorMsg.contains("account_number")) {
                    message = "Account number already exists";
                } else if (errorMsg.contains("phone")) {
                    message = "Phone number already exists";
                } else {
                    message = "Resource with this identifier already exists";
                }
            } else if (errorMsg.contains("foreign key")) {
                if (errorMsg.contains("user_id")) {
                    message = "Referenced user does not exist";
                } else if (errorMsg.contains("account_id")) {
                    message = "Referenced account does not exist";
                } else {
                    message = "Referenced resource does not exist";
                }
            } else if (errorMsg.contains("check constraint")) {
                if (errorMsg.contains("balance")) {
                    message = "Invalid balance amount";
                } else if (errorMsg.contains("status")) {
                    message = "Invalid status value";
                } else {
                    message = "Data validation constraint failed";
                }
            }
        }
        
        return Status.ABORTED
                .withDescription(message)
                .asRuntimeException();
    }

    @GrpcExceptionHandler({
            java.util.NoSuchElementException.class,
            org.springframework.dao.EmptyResultDataAccessException.class
    })
    public StatusRuntimeException handleDataNotFound(Exception e) {
        log.warn("Data not found: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription("Requested resource not found")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(org.springframework.dao.DataAccessException.class)
    public StatusRuntimeException handleDataAccess(org.springframework.dao.DataAccessException e) {
        log.error("Data access error: {}", e.getMessage(), e);
        
        String message = "Database operation failed";
        if (e.getMessage() != null) {
            String errorMsg = e.getMessage().toLowerCase();
            if (errorMsg.contains("connection") || errorMsg.contains("timeout")) {
                message = "Database connection error";
            } else if (errorMsg.contains("deadlock")) {
                message = "Database deadlock detected, please retry";
            } else if (errorMsg.contains("lock")) {
                message = "Resource is currently locked, please retry";
            }
        }
        
        return Status.UNAVAILABLE
                .withDescription(message)
                .asRuntimeException();
    }

    @GrpcExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public StatusRuntimeException handleOptimisticLocking(org.springframework.dao.OptimisticLockingFailureException e) {
        log.warn("Optimistic locking failure: {}", e.getMessage());
        return Status.ABORTED
                .withDescription("Resource was modified by another user, please refresh and try again")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public StatusRuntimeException handlePessimisticLocking(org.springframework.dao.PessimisticLockingFailureException e) {
        log.warn("Pessimistic locking failure: {}", e.getMessage());
        return Status.RESOURCE_EXHAUSTED
                .withDescription("Resource is currently locked, please try again later")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public StatusRuntimeException handleConstraintViolation(jakarta.validation.ConstraintViolationException e) {
        log.warn("Validation constraint violation: {}", e.getMessage());
        
        StringBuilder messageBuilder = new StringBuilder("Validation failed: ");
        e.getConstraintViolations().forEach(violation -> {
            messageBuilder.append(violation.getPropertyPath())
                    .append(" ")
                    .append(violation.getMessage())
                    .append("; ");
        });
        
        return Status.INVALID_ARGUMENT
                .withDescription(messageBuilder.toString())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public StatusRuntimeException handleMethodArgumentNotValid(org.springframework.web.bind.MethodArgumentNotValidException e) {
        log.warn("Method argument validation failed: {}", e.getMessage());
        
        StringBuilder messageBuilder = new StringBuilder("Validation failed: ");
        e.getBindingResult().getFieldErrors().forEach(error -> {
            messageBuilder.append(error.getField())
                    .append(" ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        });
        
        return Status.INVALID_ARGUMENT
                .withDescription(messageBuilder.toString())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(java.util.concurrent.TimeoutException.class)
    public StatusRuntimeException handleTimeout(java.util.concurrent.TimeoutException e) {
        log.warn("Operation timeout: {}", e.getMessage());
        return Status.DEADLINE_EXCEEDED
                .withDescription("Operation timed out")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(java.net.SocketTimeoutException.class)
    public StatusRuntimeException handleSocketTimeout(java.net.SocketTimeoutException e) {
        log.warn("Socket timeout: {}", e.getMessage());
        return Status.DEADLINE_EXCEEDED
                .withDescription("Network operation timed out")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(java.util.concurrent.RejectedExecutionException.class)
    public StatusRuntimeException handleRejectedExecution(java.util.concurrent.RejectedExecutionException e) {
        log.warn("Task execution rejected: {}", e.getMessage());
        return Status.RESOURCE_EXHAUSTED
                .withDescription("System is currently overloaded, please try again later")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(java.io.IOException.class)
    public StatusRuntimeException handleIOException(java.io.IOException e) {
        log.error("IO operation failed: {}", e.getMessage());
        return Status.INTERNAL
                .withDescription("File or network operation failed")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(RuntimeException.class)
    public StatusRuntimeException handleRuntimeException(RuntimeException e) {
        log.error("Runtime exception: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleGenericException(Exception e) {
        log.error("Unexpected exception: {}", e.getMessage(), e);
        return Status.UNKNOWN
                .withDescription("An unexpected error occurred")
                .asRuntimeException();
    }



    @GrpcExceptionHandler
    public StatusException handleNullPointer(NullPointerException ex) {
        log.error("Null pointer exception: {}", ex.getMessage(), ex);
        return Status.INTERNAL
                .withDescription("Internal error: Required data is missing")
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleSQLException(SQLException ex) {
        log.error("Database error: {}", ex.getMessage(), ex);
        return Status.INTERNAL
                .withDescription("Database error occurred")
                .withCause(ex)
                .asException();
    }

    @GrpcExceptionHandler
    public StatusException handleCircuitBreakerOpen(io.github.resilience4j.circuitbreaker.CallNotPermittedException ex) {
        log.warn("Circuit breaker is open: {}", ex.getMessage());
        return Status.UNAVAILABLE
                .withDescription("Service temporarily unavailable. Please try again later.")
                .withCause(ex)
                .asException();
    }

//    @GrpcExceptionHandler
//    public StatusException handleConcurrentModification(java.util.concurrent.ConcurrentModificationException ex) {
//        log.warn("Concurrent modification error: {}", ex.getMessage());
//        return Status.ABORTED
//                .withDescription("Transaction was modified concurrently. Please retry.")
//                .withCause(ex)
//                .asException();
//    }

//    @GrpcExceptionHandler
//    public StatusException handleOptimisticLocking(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
//        log.warn("Optimistic locking failure: {}", ex.getMessage());
//        return Status.ABORTED
//                .withDescription("Data was modified by another process. Please refresh and try again.")
//                .withCause(ex)
//                .asException();
//    }
}