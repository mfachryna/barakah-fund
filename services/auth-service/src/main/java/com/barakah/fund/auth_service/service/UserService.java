package com.barakah.fund.auth_service.service;

import com.barakah.fund.auth_service.exception.UserAlreadyExistsException;
import com.barakah.fund.auth_service.service.abstracts.user.UserSearchService;
import com.barakah.fund.auth_service.service.abstracts.user.UserManagementService;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.*;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserSearchService userSearchService;
    private final UserManagementService userManagementService;

    @Autowired
    public UserService(
            UserSearchService userSearchService,
            UserManagementService userManagementService) {
        this.userSearchService = userSearchService;
        this.userManagementService = userManagementService;
    }

    public Map<String, String> registerUser(String username, String email, String password, String phoneNumber) {

        if (!userSearchService.findByUsername(username).isEmpty()) {
            throw new UserAlreadyExistsException("User already exists with username: " + username);
        }

        if (!userSearchService.findByEmail(email).isEmpty()) {
            throw new UserAlreadyExistsException("User already exists with email: " + email);
        }

        UserRepresentation user = userManagementService.prepareUserRepresentation(username, email, phoneNumber);

        Response response = userManagementService.createUser(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user. Status: " + response.getStatus());
        }

        String userId = userManagementService.extractUserIdFromResponse(response);
        userManagementService.setPassword(userId, password);

        try {
            userManagementService.assignRole(userId, "user");
        } catch (Exception e) {
            logger.error("Failed to assign user role to: {}", username, e);
        }

        Map<String, String> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", username);
        result.put("message", "User successfully registered");

        return result;
    }
}
