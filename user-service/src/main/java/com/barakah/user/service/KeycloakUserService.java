package com.barakah.user.service;

import com.barakah.user.entity.UserRole;
import com.barakah.user.proto.v1.CreateUserRequest;
import com.barakah.user.proto.v1.UpdateUserRequest;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;

@Service
public class KeycloakUserService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserService.class);

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${keycloak.realm:barakah}")
    private String realm;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    private Keycloak keycloak;
    private RealmResource realmResource;

    @PostConstruct
    public void init() {
        try {
            this.keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm("master")
                    .clientId(adminClientId)
                    .username(adminUsername)
                    .password(adminPassword)
                    .build();

            this.realmResource = keycloak.realm(realm);
            log.info("Keycloak admin client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Keycloak admin client: {}", e.getMessage());
            throw new RuntimeException("Keycloak initialization failed", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (keycloak != null) {
            keycloak.close();
        }
    }

    public String createUser(String username, String email, String firstName, String lastName, String password) {
        try {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(false);

            UsersResource usersResource = realmResource.users();
            jakarta.ws.rs.core.Response response = usersResource.create(user);

            if (response.getStatus() == 201) {

                String location = response.getLocation().getPath();
                String userId = location.substring(location.lastIndexOf('/') + 1);

                setUserPassword(userId, password, false);

                log.info("User created in Keycloak: {} with ID: {}", username, userId);
                return userId;
            } else {
                String errorMsg = String.format("Failed to create user in Keycloak. Status: %d", response.getStatus());
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Error creating user in Keycloak: {}", e.getMessage());
            throw new RuntimeException("Failed to create user in Keycloak", e);
        }
    }

    public void updateUser(String keycloakId, UpdateUserRequest request) {
        try {
            UserResource userResource = realmResource.users().get(keycloakId);
            UserRepresentation user = userResource.toRepresentation();

            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());

            userResource.update(user);
            log.info("User updated in Keycloak: {}", keycloakId);

        } catch (Exception e) {
            log.error("Error updating user in Keycloak: {}", e.getMessage());
            throw new RuntimeException("Failed to update user in Keycloak", e);
        }
    }

    public void setUserPassword(String keycloakId, String password, boolean temporary) {
        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(temporary);

            UserResource userResource = realmResource.users().get(keycloakId);
            userResource.resetPassword(credential);

            log.info("Password set for user: {}", keycloakId);

        } catch (Exception e) {
            log.error("Error setting password for user: {}", e.getMessage());
            throw new RuntimeException("Failed to set user password", e);
        }
    }

    public void assignRole(String keycloakId, String roleName) {
        try {
            UserResource userResource = realmResource.users().get(keycloakId);

            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();

            userResource.roles().realmLevel().add(Collections.singletonList(role));

            log.info("Role '{}' assigned to user: {}", roleName, keycloakId);

        } catch (Exception e) {
            log.error("Error assigning role to user: {}", e.getMessage());

            log.warn("Continuing without role assignment");
        }
    }

    public void removeRole(String keycloakId, String roleName) {
        try {
            UserResource userResource = realmResource.users().get(keycloakId);

            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();

            userResource.roles().realmLevel().remove(Collections.singletonList(role));

            log.info("Role '{}' removed from user: {}", roleName, keycloakId);

        } catch (Exception e) {
            log.error("Error removing role from user: {}", e.getMessage());
        }
    }

    public void updateUserStatus(String keycloakId, boolean enabled) {
        try {
            UserResource userResource = realmResource.users().get(keycloakId);
            UserRepresentation user = userResource.toRepresentation();

            user.setEnabled(enabled);
            userResource.update(user);

            log.info("User status updated in Keycloak: {} -> {}", keycloakId, enabled ? "ENABLED" : "DISABLED");

        } catch (Exception e) {
            log.error("Error updating user status in Keycloak: {}", e.getMessage());
            throw new RuntimeException("Failed to update user status in Keycloak", e);
        }
    }

    public void deleteUser(String keycloakId) {
        try {
            realmResource.users().delete(keycloakId);
            log.info("User deleted from Keycloak: {}", keycloakId);

        } catch (Exception e) {
            log.error("Error deleting user from Keycloak: {}", e.getMessage());
            throw new RuntimeException("Failed to delete user from Keycloak", e);
        }
    }

    public UserRepresentation getUser(String keycloakId) {
        try {
            return realmResource.users().get(keycloakId).toRepresentation();
        } catch (Exception e) {
            log.error("Error getting user from Keycloak: {}", e.getMessage());
            throw new RuntimeException("Failed to get user from Keycloak", e);
        }
    }

    public List<UserRepresentation> searchUsers(String search) {
        try {
            return realmResource.users().search(search);
        } catch (Exception e) {
            log.error("Error searching users in Keycloak: {}", e.getMessage());
            throw new RuntimeException("Failed to search users in Keycloak", e);
        }
    }

    public List<RoleRepresentation> getUserRoles(String keycloakId) {
        try {
            UserResource userResource = realmResource.users().get(keycloakId);
            return userResource.roles().realmLevel().listAll();
        } catch (Exception e) {
            log.error("Error getting user roles from Keycloak: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean userExists(String keycloakId) {
        try {
            realmResource.users().get(keycloakId).toRepresentation();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void sendVerificationEmail(String keycloakId) {
        try {
            UserResource userResource = realmResource.users().get(keycloakId);
            userResource.sendVerifyEmail();
            log.info("Verification email sent for user: {}", keycloakId);
        } catch (Exception e) {
            log.error("Error sending verification email: {}", e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String keycloakId) {
        try {
            UserResource userResource = realmResource.users().get(keycloakId);
            userResource.executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
            log.info("Password reset email sent for user: {}", keycloakId);
        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage());
        }
    }
}
