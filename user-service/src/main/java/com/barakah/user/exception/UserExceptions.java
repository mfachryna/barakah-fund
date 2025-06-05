package com.barakah.user.exception;

import com.barakah.shared.exception.BusinessException;

public class UserExceptions {
    
    public static class UserNotFoundException extends RuntimeException implements BusinessException {
        public UserNotFoundException(String identifier) {
            super("User not found: " + identifier);
        }
        
        public UserNotFoundException(String field, String value) {
            super("User not found with " + field + ": " + value);
        }
    }
    
    public static class UserAlreadyExistsException extends IllegalStateException implements BusinessException {
        public UserAlreadyExistsException(String field, String value) {
            super("User already exists with " + field + ": " + value);
        }
        
        public UserAlreadyExistsException(String message) {
            super("User already exists: " + message);
        }
    }
    
    public static class InvalidUserDataException extends IllegalArgumentException implements BusinessException {
        public InvalidUserDataException(String message) {
            super("Invalid user data: " + message);
        }
        
        public InvalidUserDataException(String field, String reason) {
            super("Invalid " + field + ": " + reason);
        }
    }
    
    public static class UserCreationFailedException extends RuntimeException implements BusinessException {
        public UserCreationFailedException(String message) {
            super("Failed to create user: " + message);
        }
        
        public UserCreationFailedException(String message, Throwable cause) {
            super("Failed to create user: " + message, cause);
        }
    }
    
    public static class UserUpdateFailedException extends RuntimeException implements BusinessException {
        public UserUpdateFailedException(String message) {
            super("Failed to update user: " + message);
        }
        
        public UserUpdateFailedException(String message, Throwable cause) {
            super("Failed to update user: " + message, cause);
        }
    }
    
    public static class UserDeletionFailedException extends RuntimeException implements BusinessException {
        public UserDeletionFailedException(String message) {
            super("Failed to delete user: " + message);
        }
        
        public UserDeletionFailedException(String message, Throwable cause) {
            super("Failed to delete user: " + message, cause);
        }
    }
    
    public static class InvalidUserStatusException extends IllegalStateException implements BusinessException {
        public InvalidUserStatusException(String userId, String currentStatus, String requestedOperation) {
            super("Cannot perform '" + requestedOperation + "' on user " + userId + " with status: " + currentStatus);
        }
        
        public InvalidUserStatusException(String message) {
            super("Invalid user status: " + message);
        }
    }
    
    public static class UserPermissionException extends SecurityException implements BusinessException {
        public UserPermissionException(String action) {
            super("Permission denied for action: " + action);
        }
        
        public UserPermissionException(String userId, String action) {
            super("Permission denied for user " + userId + " to perform: " + action);
        }
    }
}