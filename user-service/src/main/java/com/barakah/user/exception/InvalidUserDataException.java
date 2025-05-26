package com.barakah.user.exception;

import java.util.List;
import java.util.Map;

public class InvalidUserDataException extends UserServiceException {
    
    private final Map<String, List<String>> fieldErrors;
    
    public InvalidUserDataException(String message) {
        super(message, "INVALID_USER_DATA");
        this.fieldErrors = Map.of();
    }
    
    public InvalidUserDataException(String message, Map<String, List<String>> fieldErrors) {
        super(message, "INVALID_USER_DATA");
        this.fieldErrors = fieldErrors != null ? fieldErrors : Map.of();
    }
    
    public InvalidUserDataException(String message, Throwable cause) {
        super(message, "INVALID_USER_DATA", cause);
        this.fieldErrors = Map.of();
    }
    
    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
    
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}