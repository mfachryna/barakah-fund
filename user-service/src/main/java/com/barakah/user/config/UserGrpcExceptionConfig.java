package com.barakah.user.config;

import com.barakah.user.exception.UserExceptions;
import com.barakah.user.exception.KeycloakExceptions;
import com.barakah.shared.exception.AuthExceptions;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@Slf4j
@GrpcAdvice
public class UserGrpcExceptionConfig {
    @GrpcExceptionHandler(UserExceptions.UserNotFoundException.class)
    public StatusRuntimeException handleUserNotFound(UserExceptions.UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(UserExceptions.UserAlreadyExistsException.class)
    public StatusRuntimeException handleUserAlreadyExists(UserExceptions.UserAlreadyExistsException e) {
        log.warn("User already exists: {}", e.getMessage());
        return Status.ALREADY_EXISTS
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(UserExceptions.InvalidUserDataException.class)
    public StatusRuntimeException handleInvalidUserData(UserExceptions.InvalidUserDataException e) {
        log.warn("Invalid user data: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(UserExceptions.UserCreationFailedException.class)
    public StatusRuntimeException handleUserCreationFailed(UserExceptions.UserCreationFailedException e) {
        log.error("User creation failed: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Failed to create user due to internal error")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(UserExceptions.UserUpdateFailedException.class)
    public StatusRuntimeException handleUserUpdateFailed(UserExceptions.UserUpdateFailedException e) {
        log.error("User update failed: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Failed to update user due to internal error")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(UserExceptions.UserDeletionFailedException.class)
    public StatusRuntimeException handleUserDeletionFailed(UserExceptions.UserDeletionFailedException e) {
        log.error("User deletion failed: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Failed to delete user due to internal error")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(UserExceptions.InvalidUserStatusException.class)
    public StatusRuntimeException handleInvalidUserStatus(UserExceptions.InvalidUserStatusException e) {
        log.warn("Invalid user status operation: {}", e.getMessage());
        return Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(UserExceptions.UserPermissionException.class)
    public StatusRuntimeException handleUserPermission(UserExceptions.UserPermissionException e) {
        log.warn("User permission denied: {}", e.getMessage());
        return Status.PERMISSION_DENIED
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(KeycloakExceptions.KeycloakConnectionException.class)
    public StatusRuntimeException handleKeycloakConnection(KeycloakExceptions.KeycloakConnectionException e) {
        log.error("Keycloak connection error: {}", e.getMessage(), e);
        return Status.UNAVAILABLE
                .withDescription("Authentication service temporarily unavailable")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(KeycloakExceptions.KeycloakUserCreationException.class)
    public StatusRuntimeException handleKeycloakUserCreation(KeycloakExceptions.KeycloakUserCreationException e) {
        log.error("Keycloak user creation failed: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Failed to create user in authentication service")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(KeycloakExceptions.KeycloakUserUpdateException.class)
    public StatusRuntimeException handleKeycloakUserUpdate(KeycloakExceptions.KeycloakUserUpdateException e) {
        log.error("Keycloak user update failed: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Failed to update user in authentication service")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(KeycloakExceptions.KeycloakUserDeletionException.class)
    public StatusRuntimeException handleKeycloakUserDeletion(KeycloakExceptions.KeycloakUserDeletionException e) {
        log.error("Keycloak user deletion failed: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Failed to delete user from authentication service")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(KeycloakExceptions.KeycloakRoleException.class)
    public StatusRuntimeException handleKeycloakRole(KeycloakExceptions.KeycloakRoleException e) {
        log.error("Keycloak role operation failed: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Failed to manage user roles")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(KeycloakExceptions.KeycloakUserNotFoundException.class)
    public StatusRuntimeException handleKeycloakUserNotFound(KeycloakExceptions.KeycloakUserNotFoundException e) {
        log.warn("User not found in Keycloak: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription("User not found in authentication service")
                .asRuntimeException();
    }

    @GrpcExceptionHandler(KeycloakExceptions.KeycloakInvalidResponseException.class)
    public StatusRuntimeException handleKeycloakInvalidResponse(KeycloakExceptions.KeycloakInvalidResponseException e) {
        log.error("Invalid Keycloak response: {}", e.getMessage());
        return Status.INTERNAL
                .withDescription("Authentication service returned invalid response")
                .asRuntimeException();
    }
}