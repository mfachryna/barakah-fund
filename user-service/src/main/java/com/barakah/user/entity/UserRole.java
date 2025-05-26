package com.barakah.user.entity;

public enum UserRole {
    USER("User"),
    ADMIN("Administrator"),
    MANAGER("Manager"),
    TELLER("Teller");
    
    private final String description;
    
    UserRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}