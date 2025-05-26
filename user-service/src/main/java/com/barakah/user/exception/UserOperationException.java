package com.barakah.user.exception;

public class UserOperationException extends UserServiceException {
    
    private final String operation;
    private final String userId;
    
    public UserOperationException(String message, String operation) {
        super(message, "USER_OPERATION_FAILED");
        this.operation = operation;
        this.userId = null;
    }
    
    public UserOperationException(String message, String operation, String userId) {
        super(message, "USER_OPERATION_FAILED");
        this.operation = operation;
        this.userId = userId;
    }
    
    public UserOperationException(String message, String operation, Throwable cause) {
        super(message, "USER_OPERATION_FAILED", cause);
        this.operation = operation;
        this.userId = null;
    }
    
    public UserOperationException(String message, String operation, String userId, Throwable cause) {
        super(message, "USER_OPERATION_FAILED", cause);
        this.operation = operation;
        this.userId = userId;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getUserId() {
        return userId;
    }
}