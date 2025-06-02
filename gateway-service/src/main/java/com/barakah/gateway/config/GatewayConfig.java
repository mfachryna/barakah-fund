package com.barakah.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Slf4j
@Configuration
public class GatewayConfig {

    @Bean
    @ConditionalOnProperty(name = "app.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerConfig config) {
        return CircuitBreaker.of("user-service", config);
    }

    @Bean
    @ConditionalOnProperty(name = "app.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreaker accountServiceCircuitBreaker(CircuitBreakerConfig config) {
        return CircuitBreaker.of("account-service", config);
    }

    @Bean
    @ConditionalOnProperty(name = "app.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreaker transactionServiceCircuitBreaker(CircuitBreakerConfig config) {
        return CircuitBreaker.of("transaction-service", config);
    }

    // Development profile configuration
    @Configuration
    @Profile("dev")
    static class DevConfig {
        
        @Bean
        public String devMessage() {
            log.info("Gateway running in DEV mode - using fallback responses for missing services");
            return "Development mode active";
        }
    }

    // Production profile configuration  
    @Configuration
    @Profile("prod")
    static class ProdConfig {
        
        @Bean
        public String prodMessage() {
            log.info("Gateway running in PRODUCTION mode - all services must be available");
            return "Production mode active";
        }
    }
}