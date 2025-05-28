package com.barakah.account.exception;

public class AccountExceptions {
    
    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String identifier) {
            super("Account not found: " + identifier);
        }
        
        public AccountNotFoundException(String field, String value) {
            super("Account not found with " + field + ": " + value);
        }
    }
    
    public static class AccountAlreadyExistsException extends IllegalStateException {
        public AccountAlreadyExistsException(String userId, String accountType) {
            super("Account already exists for user " + userId + " with type: " + accountType);
        }
    }
    
    public static class InsufficientBalanceException extends IllegalStateException {
        public InsufficientBalanceException(String accountNumber, String available, String requested) {
            super("Insufficient balance in account " + accountNumber + ". Available: " + available + ", Requested: " + requested);
        }
    }
    
    public static class InvalidAccountOperationException extends IllegalArgumentException {
        public InvalidAccountOperationException(String operation, String reason) {
            super("Invalid account operation '" + operation + "': " + reason);
        }
    }
    
    public static class AccountStatusException extends IllegalStateException {
        public AccountStatusException(String accountNumber, String status, String operation) {
            super("Cannot perform '" + operation + "' on account " + accountNumber + " with status: " + status);
        }
    }
    
    public static class DuplicateTransactionException extends IllegalStateException {
        public DuplicateTransactionException(String transactionId) {
            super("Transaction already processed: " + transactionId);
        }
    }
    
    public static class InvalidAccountException extends IllegalArgumentException {
        public InvalidAccountException(String message) {
            super("Invalid account: " + message);
        }
        
        public InvalidAccountException(String accountIdentifier, String reason) {
            super("Invalid account " + accountIdentifier + ": " + reason);
        }
    }
    
    public static class AccountNotActiveException extends IllegalStateException {
        public AccountNotActiveException(String accountNumber) {
            super("Account is not active: " + accountNumber);
        }
        
        public AccountNotActiveException(String accountNumber, String currentStatus) {
            super("Account " + accountNumber + " is not active. Current status: " + currentStatus);
        }
    }
}