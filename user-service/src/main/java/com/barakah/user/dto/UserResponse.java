package com.barakah.user.dto;

import com.barakah.user.entity.UserStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserResponse {
    private String userId;
    private String keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDateTime dateOfBirth;
    private String address;
    private UserStatus status;
    private String role;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}