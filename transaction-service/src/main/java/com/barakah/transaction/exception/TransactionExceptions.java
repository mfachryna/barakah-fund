package com.barakah.transaction.exception;

import com.barakah.shared.exception.BusinessException;

public class TransactionExceptions {
    
    public static class TransactionNotFoundException extends RuntimeException implements BusinessException {
        public TransactionNotFoundException(String transactionId) {
            super("Transaction not found: " + transactionId);
        }
        
        public TransactionNotFoundException(String field, String value) {
            super("Transaction not found with " + field + ": " + value);
        }
    }
    
    public static class InvalidTransactionException extends IllegalArgumentException implements BusinessException {
        public InvalidTransactionException(String message) {
            super("Invalid transaction: " + message);
        }
    }
    
    public static class InsufficientBalanceException extends IllegalStateException implements BusinessException {
        public InsufficientBalanceException(String accountNumber, String availableBalance, String requestedAmount) {
            super("Insufficient balance in account " + accountNumber + 
                  ". Available: " + availableBalance + ", Requested: " + requestedAmount);
        }
    }
    
    public static class InvalidTransactionStatusException extends IllegalStateException implements BusinessException {
        public InvalidTransactionStatusException(String transactionId, String currentStatus, String requestedStatus) {
            super("Cannot change transaction " + transactionId + " from " + currentStatus + " to " + requestedStatus);
        }
    }
    
    public static class AccountNotFoundException extends RuntimeException implements BusinessException {
        public AccountNotFoundException(String accountNumber) {
            super("Account not found: " + accountNumber);
        }
    }
    
    public static class SameAccountTransferException extends IllegalArgumentException implements BusinessException {
        public SameAccountTransferException(String accountNumber) {
            super("Cannot transfer to the same account: " + accountNumber);
        }
    }
    
    public static class AccountNotActiveException extends IllegalStateException implements BusinessException {
        public AccountNotActiveException(String accountNumber) {
            super("Account is not active: " + accountNumber);
        }
    }
    
    public static class TransactionProcessingException extends RuntimeException implements BusinessException {
        public TransactionProcessingException(String message) {
            super("Transaction processing failed: " + message);
        }
        
        public TransactionProcessingException(String message, Throwable cause) {
            super("Transaction processing failed: " + message, cause);
        }
    }
    
    public static class DuplicateTransactionException extends IllegalStateException implements BusinessException {
        public DuplicateTransactionException(String referenceNumber) {
            super("Duplicate transaction with reference number: " + referenceNumber);
        }
    }
    
    public static class CategoryNotFoundException extends RuntimeException implements BusinessException {
        public CategoryNotFoundException(String categoryId) {
            super("Transaction category not found: " + categoryId);
        }
    }
    
    public static class CategoryInUseException extends IllegalStateException implements BusinessException {
        public CategoryInUseException(String categoryId) {
            super("Cannot delete category " + categoryId + " as it is being used by transactions");
        }
    }
    
    public static class SystemCategoryException extends IllegalStateException implements BusinessException {
        public SystemCategoryException(String operation) {
            super("Cannot " + operation + " system category");
        }
    }
}