package com.barakah.shared.util;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcErrorHandler {

    public static void handleFallbackError(String serviceName, String operation, Exception ex) {
        handleFallbackError(serviceName, operation, null, ex);
    }

    public static void handleFallbackError(String serviceName, String operation,
            String customMessage, Exception ex) {
        if (ex instanceof StatusRuntimeException grpcEx) {
            Status.Code code = grpcEx.getStatus().getCode();

            if (isBusinessLogicError(code)) {
                log.debug("{} service - Business logic error in {}: {} - {}",
                        serviceName, operation, code, grpcEx.getStatus().getDescription());
                throw grpcEx;
            }

            log.error("{} service - Service error in {}: {} - {}",
                    serviceName, operation, code, grpcEx.getStatus().getDescription());
        } else {
            log.error("{} service - Unexpected error in {}: {}",
                    serviceName, operation, ex.getMessage());
        }

        String message = customMessage != null ? customMessage
                : String.format("%s service is currently unavailable. Please try again later.", serviceName);

        throw new RuntimeException(message, ex);
    }

    private static boolean isBusinessLogicError(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT, NOT_FOUND, ALREADY_EXISTS, PERMISSION_DENIED, FAILED_PRECONDITION, OUT_OF_RANGE, UNAUTHENTICATED ->
                true;
            default ->
                false;
        };
    }
}
