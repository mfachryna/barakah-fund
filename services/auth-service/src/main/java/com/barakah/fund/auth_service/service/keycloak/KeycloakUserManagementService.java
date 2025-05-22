package com.barakah.fund.auth_service.service.keycloak;

import com.barakah.fund.auth_service.config.KeycloakConfig;
import com.barakah.fund.auth_service.service.abstracts.user.UserManagementService;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.*;

@Service
public class KeycloakUserManagementService implements UserManagementService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakUserManagementService.class);

    private final KeycloakConfig keycloakConfig;

    @Value("${keycloak.realm}")
    private String realm;

    @Autowired
    public KeycloakUserManagementService(KeycloakConfig keycloakConfig) {
        this.keycloakConfig = keycloakConfig;
        logger.debug("KeycloakUserManagementService initialized with realm: {}", realm);
    }

    @Override
    public Response createUser(UserRepresentation user) {
        logger.info("Creating new user with username: {}", user.getUsername());
        try {
            Response response = keycloakConfig.keycloak().realm(realm).users().create(user);
            logger.debug("User creation response status: {}", response.getStatus());
            return response;
        } catch (Exception e) {
            logger.error("Failed to create user: {}", user.getUsername(), e);
            throw e;
        }
    }

    @Override
    public void setPassword(String userId, String password) {
        logger.info("Setting password for user ID: {}", userId);
        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);

            keycloakConfig.keycloak().realm(realm).users().get(userId).resetPassword(credential);
            logger.debug("Password successfully set for user ID: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to set password for user ID: {}", userId, e);
            throw e;
        }
    }

    @Override
    public void assignRole(String userId, String roleName) {
        logger.info("Assigning role '{}' to user ID: {}", roleName, userId);
        try {
            RoleRepresentation role = keycloakConfig.keycloak().realm(realm)
                    .roles().get(roleName).toRepresentation();

            keycloakConfig.keycloak().realm(realm).users().get(userId)
                    .roles().realmLevel().add(Collections.singletonList(role));

            logger.debug("Successfully assigned role '{}' to user ID: {}", roleName, userId);
        } catch (Exception e) {
            logger.error("Failed to assign role '{}' to user ID: {}", roleName, userId, e);
            throw new RuntimeException("Failed to assign role: " + roleName, e);
        }
    }

    @Override
    public String extractUserIdFromResponse(Response response) {
        try {
            String location = response.getHeaderString("Location");
            if (location == null) {
                logger.warn("No Location header found in response");
                throw new IllegalArgumentException("No Location header found in response");
            }
            String userId = location.substring(location.lastIndexOf("/") + 1);
            logger.debug("Extracted user ID: {} from response", userId);
            return userId;
        } catch (Exception e) {
            logger.error("Failed to extract user ID from response", e);
            throw e;
        }
    }

    @Override
    public UserRepresentation prepareUserRepresentation(String username, String email, String phoneNumber) {
        logger.debug("Preparing user representation for username: {}, email: {}", username, email);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setFirstName(username);
        user.setLastName("User");

        Map<String, List<String>> attributes = new HashMap<>();
        // Don't log full phone number - privacy concern
        logger.debug("Adding phone number attribute for user: {}", username);
        attributes.put("phone_number", Collections.singletonList(phoneNumber));
        user.setAttributes(attributes);

        user.setRequiredActions(new ArrayList<>());

        return user;
    }

}
