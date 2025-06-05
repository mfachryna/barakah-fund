package com.barakah.shared.config;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
public class ServiceFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {

        if (throwable instanceof StatusRuntimeException grpcException) {
            Status.Code code = grpcException.getStatus().getCode();

            boolean isBusinessError = switch (code) {
                case INVALID_ARGUMENT, NOT_FOUND, ALREADY_EXISTS, PERMISSION_DENIED, FAILED_PRECONDITION, OUT_OF_RANGE, UNAUTHENTICATED ->
                    true;
                default ->
                    false;
            };

            if (isBusinessError) {
                log.debug("Not counting business error as circuit breaker failure: {} - {}",
                        code, grpcException.getStatus().getDescription());
                return false;
            }
        }

        log.debug("Counting as circuit breaker failure: {}", throwable.getClass().getSimpleName());
        return true;
    }
}
