package com.barakah.user.exception;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GrpcExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GrpcExceptionHandler.class);

    public StatusRuntimeException handleException(Exception exception) {
        log.error("Handling gRPC exception: {}", exception.getMessage(), exception);

        if (exception instanceof UserNotFoundException) {
            return Status.NOT_FOUND
                    .withDescription(exception.getMessage())
                    .asRuntimeException();
        }

        if (exception instanceof UserAlreadyExistsException) {
            return Status.ALREADY_EXISTS
                    .withDescription(exception.getMessage())
                    .asRuntimeException();
        }

        if (exception instanceof InvalidUserDataException) {
            InvalidUserDataException ex = (InvalidUserDataException) exception;
            String description = ex.getMessage();
            if (ex.hasFieldErrors()) {
                description += " Field errors: " + ex.getFieldErrors();
            }
            return Status.INVALID_ARGUMENT
                    .withDescription(description)
                    .asRuntimeException();
        }

        if (exception instanceof UserOperationException) {
            UserOperationException ex = (UserOperationException) exception;
            return Status.FAILED_PRECONDITION
                    .withDescription("Operation '" + ex.getOperation() + "' failed: " + ex.getMessage())
                    .asRuntimeException();
        }

        if (exception instanceof UserServiceException) {
            UserServiceException ex = (UserServiceException) exception;
            return Status.INTERNAL
                    .withDescription("Service error [" + ex.getErrorCode() + "]: " + ex.getMessage())
                    .asRuntimeException();
        }

        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException();
        }

        if (exception instanceof SecurityException) {
            return Status.PERMISSION_DENIED
                    .withDescription("Access denied: " + exception.getMessage())
                    .asRuntimeException();
        }

        return Status.INTERNAL
                .withDescription("An unexpected error occurred")
                .asRuntimeException();
    }
}
