package com.barakah.fund.auth_service.util;

import com.barakah.fund.proto.shared.ErrorInfo;
import com.barakah.fund.proto.shared.ErrorCode;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;

import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;

import java.util.Map;

public class GrpcErrorUtil {

    /**
     * Creates a StatusRuntimeException with detailed error information
     *
     * @param grpcCode The gRPC status code (from Code class)
     * @param errorCode The application-specific error code (from ErrorCode
     * enum)
     * @param message The error message
     * @param domain The error domain
     * @param metadata Additional error metadata (optional)
     * @return A StatusRuntimeException with error details
     */
    public static StatusRuntimeException createStatusException(
            int grpcCode,
            ErrorCode errorCode,
            String message,
            String domain,
            Map<String, String> metadata) {

        ErrorInfo.Builder errorInfoBuilder = ErrorInfo.newBuilder()
                .setCode(errorCode)
                .setDomain(domain)
                .setMessage(message);

        if (metadata != null) {
            errorInfoBuilder.putAllMetadata(metadata);
        }

        Status status = Status.newBuilder()
                .setCode(grpcCode)
                .setMessage(message)
                .addDetails(Any.pack(errorInfoBuilder.build()))
                .build();

        return StatusProto.toStatusRuntimeException(status);
    }

    public static StatusRuntimeException userAlreadyExists(String username) {
        return createStatusException(
                Code.ALREADY_EXISTS_VALUE,
                ErrorCode.USERNAME_ALREADY_EXISTS,
                "Username already exists: " + username,
                "auth.registration",
                null
        );
    }

    public static StatusRuntimeException emailAlreadyExists(String email) {
        return createStatusException(
                Code.ALREADY_EXISTS_VALUE,
                ErrorCode.EMAIL_ALREADY_EXISTS,
                "Email already exists: " + email,
                "auth.registration",
                null
        );
    }

    public static StatusRuntimeException authenticationFailed() {
        return createStatusException(
                Code.UNAUTHENTICATED_VALUE,
                ErrorCode.INVALID_CREDENTIALS,
                "Invalid username or password",
                "auth.authentication",
                null
        );
    }

    public static StatusRuntimeException internalError(String message) {
        return createStatusException(
                Code.INTERNAL_VALUE,
                ErrorCode.INTERNAL_SERVER_ERROR,
                message,
                "auth.internal",
                null
        );
    }

    public static StatusRuntimeException invalidToken() {
        return createStatusException(
                Code.UNAUTHENTICATED_VALUE,
                ErrorCode.INVALID_TOKEN,
                "Invalid or expired token",
                "auth.token",
                null
        );
    }
}
