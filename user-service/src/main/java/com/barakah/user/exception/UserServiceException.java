package com.barakah.user.exception;

public class UserServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public UserServiceException(String message) {
        super(message);
        this.errorCode = "USER_SERVICE_ERROR";
    }
    
    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "USER_SERVICE_ERROR";
    }
    
    public UserServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public UserServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}