package com.barakah.user.config;

import com.barakah.shared.config.BaseResilienceConfig;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class UserResilienceConfig {

    public static class UserFallbacks {

        public static Object fallbackGetUser(String userId, Exception ex) {
            log.error("User service fallback triggered for getUserById - User: {}, Error: {}",
                    userId, ex.getMessage());
            return null;
        }

        public static Object fallbackCreateUser(Object request, Exception ex) {
            log.error("User creation fallback triggered - Error: {}", ex.getMessage());
            throw new RuntimeException("User service is temporarily unavailable. Please try again later.");
        }

        public static Object fallbackUpdateUser(String userId, Object request, Exception ex) {
            log.error("User update fallback triggered - User: {}, Error: {}", userId, ex.getMessage());
            throw new RuntimeException("User update service is temporarily unavailable. Please try again later.");
        }

        public static Object fallbackKeycloakOperation(Object request, Exception ex) {
            log.error("Keycloak operation fallback triggered - Error: {}", ex.getMessage());

            String errorMessage;
            if (ex instanceof java.net.ConnectException || ex.getCause() instanceof java.net.ConnectException) {
                errorMessage = "Authentication service is temporarily unavailable. Please try again later.";
            } else if (ex instanceof java.util.concurrent.TimeoutException) {
                errorMessage = "Authentication request timed out. Please try again.";
            } else {
                errorMessage = "Authentication service is temporarily unavailable. Please try again later.";
            }

            throw new RuntimeException(errorMessage);
        }
    }
}
