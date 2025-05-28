package com.barakah.account.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LoggingConfig {

    @PostConstruct
    public void initLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(context);
        
        log.info("=== Account Service Logging Configuration Initialized ===");
        log.info("Application started with logging level: {}", 
                 context.getLogger("com.barakah.account").getLevel());
    }
}