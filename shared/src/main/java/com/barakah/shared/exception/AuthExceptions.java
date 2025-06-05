package com.barakah.shared.exception;

public class AuthExceptions {
    
    public static class InvalidCredentialsException extends RuntimeException implements BusinessException {
        public InvalidCredentialsException(String message) {
            super("Invalid credentials: " + message);
        }
        
        public InvalidCredentialsException() {
            super("Invalid username or password");
        }
    }
    
    public static class TokenExpiredException extends RuntimeException implements BusinessException {
        public TokenExpiredException() {
            super("Token has expired");
        }
        
        public TokenExpiredException(String message) {
            super("Token expired: " + message);
        }
    }
    
    public static class InvalidTokenException extends RuntimeException implements BusinessException {
        public InvalidTokenException(String message) {
            super("Invalid token: " + message);
        }
        
        public InvalidTokenException() {
            super("Invalid or malformed token");
        }
    }
    
    public static class UserAlreadyExistsException extends IllegalStateException implements BusinessException {
        public UserAlreadyExistsException(String identifier) {
            super("User already exists: " + identifier);
        }
        
        public UserAlreadyExistsException(String field, String value) {
            super("User already exists with " + field + ": " + value);
        }
    }
    
    public static class RegistrationFailedException extends RuntimeException implements BusinessException {
        public RegistrationFailedException(String message) {
            super("Registration failed: " + message);
        }
        
        public RegistrationFailedException(String message, Throwable cause) {
            super("Registration failed: " + message, cause);
        }
    }
    
    public static class AccountDisabledException extends SecurityException implements BusinessException {
        public AccountDisabledException(String username) {
            super("Account is disabled: " + username);
        }
        
        public AccountDisabledException() {
            super("Account is disabled");
        }
    }
    
    public static class AccountLockedException extends SecurityException implements BusinessException {
        public AccountLockedException(String username) {
            super("Account is locked: " + username);
        }
        
        public AccountLockedException() {
            super("Account is temporarily locked");
        }
    }
    
    public static class InsufficientPermissionsException extends SecurityException implements BusinessException {
        public InsufficientPermissionsException(String action) {
            super("Insufficient permissions for: " + action);
        }
        
        public InsufficientPermissionsException(String user, String action) {
            super("User " + user + " lacks permission for: " + action);
        }
    }
    
    public static class AuthenticationServiceException extends RuntimeException implements BusinessException {
        public AuthenticationServiceException(String message) {
            super("Authentication service error: " + message);
        }
        
        public AuthenticationServiceException(String message, Throwable cause) {
            super("Authentication service error: " + message, cause);
        }
    }
}