package com.barakah.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Configuration
public class MetricsConfig {

    @Bean
    public Timer accountCreationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("account.creation.time")
                .description("Time taken to create an account")
                .register(meterRegistry);
    }

    @Bean
    public Timer balanceUpdateTimer(MeterRegistry meterRegistry) {
        return Timer.builder("account.balance.update.time")
                .description("Time taken to update account balance")
                .register(meterRegistry);
    }
}
