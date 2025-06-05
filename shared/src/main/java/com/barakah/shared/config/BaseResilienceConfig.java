package com.barakah.shared.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class BaseResilienceConfig {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                log.info("Circuit breaker {} added", circuitBreaker.getName());

                circuitBreaker.getEventPublisher()
                        .onSuccess(event -> log.debug("Circuit breaker {} recorded success", circuitBreaker.getName()))
                        .onError(event -> {

                            Throwable throwable = event.getThrowable();

                            if (isBusinessError(throwable)) {

                                log.debug("Circuit breaker {} ignored business error: {}",
                                        circuitBreaker.getName(), throwable.getMessage());
                            } else {

                                log.warn("Circuit breaker {} recorded service failure: {}",
                                        circuitBreaker.getName(), throwable.getMessage());
                            }
                        })
                        .onStateTransition(event -> log.warn("Circuit breaker {} state transition: {} -> {}",
                        circuitBreaker.getName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                log.info("Circuit breaker {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("Circuit breaker {} replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }

    private boolean isBusinessError(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException grpcException) {
            Status.Code code = grpcException.getStatus().getCode();

            return switch (code) {
                case ALREADY_EXISTS, NOT_FOUND, INVALID_ARGUMENT, PERMISSION_DENIED, UNAUTHENTICATED, FAILED_PRECONDITION, OUT_OF_RANGE, CANCELLED ->
                    true;
                default ->
                    false;
            };
        }

        if (throwable instanceof IllegalArgumentException
                || throwable instanceof IllegalStateException
                || throwable instanceof SecurityException) {
            return true;
        }

        return false;
    }
}
