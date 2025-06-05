package com.barakah.shared.config;

import com.barakah.shared.exception.BusinessException;
import com.barakah.shared.exception.NonRetryableException;
import com.barakah.shared.exception.RetryableException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.function.Predicate;

@Slf4j
public class ServiceRetryPredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {

        if (throwable instanceof NonRetryableException) {
            log.debug("NOT retrying: marked as NonRetryableException - {}", throwable.getClass().getSimpleName());
            return false;
        }

        if (throwable instanceof BusinessException) {
            log.debug("NOT retrying: business logic exception - {}", throwable.getClass().getSimpleName());
            return false;
        }

        if (throwable instanceof RetryableException) {
            log.debug("RETRYING: marked as RetryableException - {}", throwable.getClass().getSimpleName());
            return true;
        }

        if (isStandardBusinessError(throwable)) {
            log.debug("NOT retrying: standard business error - {}", throwable.getClass().getSimpleName());
            return false;
        }

        if (isDatabaseConstraintViolation(throwable)) {
            log.debug("NOT retrying: database constraint violation");
            return false;
        }

        if (isGrpcBusinessError(throwable)) {
            log.debug("NOT retrying: gRPC business error - {}", throwable.getMessage());
            return false;
        }

        if (isInfrastructureFailure(throwable)) {
            log.debug("RETRYING: infrastructure failure - {}", throwable.getClass().getSimpleName());
            return true;
        }

        log.debug("RETRYING: unknown error (conservative) - {}", throwable.getClass().getSimpleName());
        return true;
    }

    private boolean isStandardBusinessError(Throwable throwable) {
        return throwable instanceof IllegalArgumentException
                || throwable instanceof IllegalStateException
                || throwable instanceof SecurityException
                || throwable instanceof jakarta.validation.ValidationException
                || throwable instanceof org.springframework.web.bind.MethodArgumentNotValidException
                || throwable instanceof org.springframework.security.access.AccessDeniedException;
    }

    private boolean isInfrastructureFailure(Throwable throwable) {
        return throwable instanceof java.net.SocketTimeoutException
                || throwable instanceof java.net.SocketException
                || throwable instanceof java.util.concurrent.TimeoutException
                || throwable instanceof java.sql.SQLTransientConnectionException
                || throwable instanceof org.springframework.dao.TransientDataAccessException
                || throwable instanceof org.springframework.web.client.ResourceAccessException;
    }

    private boolean isDatabaseConstraintViolation(Throwable throwable) {
        if (throwable instanceof DataIntegrityViolationException) {
            return true;
        }

        if (throwable instanceof SQLException sqlEx) {
            String sqlState = sqlEx.getSQLState();
            return "23505".equals(sqlState) || "23503".equals(sqlState)
                    || "23514".equals(sqlState) || "23000".equals(sqlState);
        }

        return false;
    }

    private boolean isGrpcBusinessError(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException grpcException) {
            Status.Code code = grpcException.getStatus().getCode();

            return switch (code) {
                case ALREADY_EXISTS, NOT_FOUND, INVALID_ARGUMENT, PERMISSION_DENIED, UNAUTHENTICATED, FAILED_PRECONDITION, OUT_OF_RANGE, CANCELLED ->
                    true;
                case UNAVAILABLE, DEADLINE_EXCEEDED, RESOURCE_EXHAUSTED, INTERNAL ->
                    false;
                default ->
                    false;
            };
        }
        return false;
    }
}
