package com.barakah.user.exception;

public class UserAlreadyExistsException extends RuntimeException {
    
    private final String username;
    private final String email;
    private final ConflictField conflictField;
    
    public enum ConflictField {
        USERNAME,
        EMAIL,
        BOTH
    }
    
    public UserAlreadyExistsException(String message) {
        super(message);
        this.username = null;
        this.email = null;
        this.conflictField = null;
    }
    
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
        this.username = null;
        this.email = null;
        this.conflictField = null;
    }
    
    private UserAlreadyExistsException(String message, String username, String email, ConflictField conflictField) {
        super(message);
        this.username = username;
        this.email = email;
        this.conflictField = conflictField;
    }
    
    public static UserAlreadyExistsException byUsername(String username) {
        return new UserAlreadyExistsException(
                "User already exists with username: " + username,
                username,
                null,
                ConflictField.USERNAME
        );
    }
    
    public static UserAlreadyExistsException byEmail(String email) {
        return new UserAlreadyExistsException(
                "User already exists with email: " + email,
                null,
                email,
                ConflictField.EMAIL
        );
    }
    
    public static UserAlreadyExistsException byUsernameAndEmail(String username, String email) {
        return new UserAlreadyExistsException(
                "User already exists with username: " + username + " and email: " + email,
                username,
                email,
                ConflictField.BOTH
        );
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public ConflictField getConflictField() {
        return conflictField;
    }
    
    public boolean isUsernameConflict() {
        return conflictField == ConflictField.USERNAME || conflictField == ConflictField.BOTH;
    }
    
    public boolean isEmailConflict() {
        return conflictField == ConflictField.EMAIL || conflictField == ConflictField.BOTH;
    }
}