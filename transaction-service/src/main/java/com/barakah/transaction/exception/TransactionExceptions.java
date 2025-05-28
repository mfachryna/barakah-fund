package com.barakah.transaction.exception;

public class TransactionExceptions {
    
    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String transactionId) {
            super("Transaction not found: " + transactionId);
        }
        
        public TransactionNotFoundException(String field, String value) {
            super("Transaction not found with " + field + ": " + value);
        }
    }
    
    public static class InvalidTransactionException extends IllegalArgumentException {
        public InvalidTransactionException(String message) {
            super("Invalid transaction: " + message);
        }
    }
    
    public static class InsufficientBalanceException extends IllegalStateException {
        public InsufficientBalanceException(String accountNumber, String availableBalance, String requestedAmount) {
            super("Insufficient balance in account " + accountNumber + 
                  ". Available: " + availableBalance + ", Requested: " + requestedAmount);
        }
    }
    
    public static class InvalidTransactionStatusException extends IllegalStateException {
        public InvalidTransactionStatusException(String transactionId, String currentStatus, String requestedStatus) {
            super("Cannot change transaction " + transactionId + " from " + currentStatus + " to " + requestedStatus);
        }
    }
    
    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String accountNumber) {
            super("Account not found: " + accountNumber);
        }
    }
    
    public static class SameAccountTransferException extends IllegalArgumentException {
        public SameAccountTransferException(String accountNumber) {
            super("Cannot transfer to the same account: " + accountNumber);
        }
    }
    
    public static class AccountNotActiveException extends IllegalStateException {
        public AccountNotActiveException(String accountNumber) {
            super("Account is not active: " + accountNumber);
        }
    }
    
    public static class TransactionProcessingException extends RuntimeException {
        public TransactionProcessingException(String message) {
            super("Transaction processing failed: " + message);
        }
        
        public TransactionProcessingException(String message, Throwable cause) {
            super("Transaction processing failed: " + message, cause);
        }
    }
    
    public static class DuplicateTransactionException extends IllegalStateException {
        public DuplicateTransactionException(String referenceNumber) {
            super("Duplicate transaction with reference number: " + referenceNumber);
        }
    }
    
    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(String categoryId) {
            super("Transaction category not found: " + categoryId);
        }
    }
    
    public static class CategoryInUseException extends IllegalStateException {
        public CategoryInUseException(String categoryId) {
            super("Cannot delete category " + categoryId + " as it is being used by transactions");
        }
    }
    
    public static class SystemCategoryException extends IllegalStateException {
        public SystemCategoryException(String operation) {
            super("Cannot " + operation + " system category");
        }
    }
}