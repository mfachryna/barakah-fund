package com.barakah.user.exception;

public class UserNotFoundException extends RuntimeException {

    private final String userId;
    private final String username;
    private final String email;

    public UserNotFoundException(String message) {
        super(message);
        this.userId = null;
        this.username = null;
        this.email = null;
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.userId = null;
        this.username = null;
        this.email = null;
    }


    public static UserNotFoundException byId(String userId) {
        return new UserNotFoundException("User not found with ID: " + userId) {
            @Override
            public String getUserId() {
                return userId;
            }
        };
    }

    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("User not found with username: " + username) {
            @Override
            public String getUsername() {
                return username;
            }
        };
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email) {
            @Override
            public String getEmail() {
                return email;
            }
        };
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}