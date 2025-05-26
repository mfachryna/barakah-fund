package com.barakah.auth.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    public boolean isUser() {
        return hasRole("USER");
    }
}