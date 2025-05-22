package com.barakah.fund.auth_service.exception;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationException extends RuntimeException {
    
    private final Map<String, Object> details;
    
    public AuthenticationException(String message) {
        super(message);
        this.details = new HashMap<>();
    }
    
    public AuthenticationException(String message, Map<String, Object> details) {
        super(message);
        this.details = details != null ? details : new HashMap<>();
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.details = new HashMap<>();
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public String getErrorCode() {
        return details.containsKey("error_code") ? 
               (String) details.get("error_code") : "authentication_error";
    }
}