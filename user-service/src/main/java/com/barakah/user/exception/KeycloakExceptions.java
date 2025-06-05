package com.barakah.user.exception;

import com.barakah.shared.exception.BusinessException;
import com.barakah.shared.exception.NonRetryableException;

public class KeycloakExceptions {

    public static class KeycloakUserNotFoundException extends RuntimeException implements BusinessException {

        public KeycloakUserNotFoundException(String keycloakId) {
            super("User not found in Keycloak: " + keycloakId);
        }
    }

    public static class KeycloakConnectionException extends RuntimeException implements NonRetryableException {

        public KeycloakConnectionException(String message) {
            super("Keycloak connection error: " + message);
        }

        public KeycloakConnectionException(String message, Throwable cause) {
            super("Keycloak connection error: " + message, cause);
        }
    }

    public static class KeycloakInvalidConfigException extends RuntimeException implements NonRetryableException {

        public KeycloakInvalidConfigException(String message) {
            super("Invalid Keycloak configuration: " + message);
        }

        public KeycloakInvalidConfigException(String message, Throwable cause) {
            super("Invalid Keycloak configuration: " + message, cause);
        }
    }

    public static class KeycloakUserCreationException extends RuntimeException {

        public KeycloakUserCreationException(String username, String reason) {
            super("Failed to create user '" + username + "' in Keycloak: " + reason);
        }

        public KeycloakUserCreationException(String message, Throwable cause) {
            super("Keycloak user creation failed: " + message, cause);
        }
    }

    public static class KeycloakUserUpdateException extends RuntimeException {

        public KeycloakUserUpdateException(String keycloakId, String reason) {
            super("Failed to update user '" + keycloakId + "' in Keycloak: " + reason);
        }

        public KeycloakUserUpdateException(String message, Throwable cause) {
            super("Keycloak user update failed: " + message, cause);
        }
    }

    public static class KeycloakUserDeletionException extends RuntimeException {

        public KeycloakUserDeletionException(String keycloakId, String reason) {
            super("Failed to delete user '" + keycloakId + "' from Keycloak: " + reason);
        }

        public KeycloakUserDeletionException(String message, Throwable cause) {
            super("Keycloak user deletion failed: " + message, cause);
        }
    }

    public static class KeycloakRoleException extends RuntimeException {

        public KeycloakRoleException(String keycloakId, String roleName, String operation) {
            super("Failed to " + operation + " role '" + roleName + "' for user '" + keycloakId + "'");
        }

        public KeycloakRoleException(String message, Throwable cause) {
            super("Keycloak role operation failed: " + message, cause);
        }
    }

    public static class KeycloakInvalidResponseException extends RuntimeException {

        public KeycloakInvalidResponseException(int statusCode, String operation) {
            super("Keycloak returned invalid response for " + operation + ": HTTP " + statusCode);
        }

        public KeycloakInvalidResponseException(String message) {
            super("Invalid Keycloak response: " + message);
        }
    }
}
