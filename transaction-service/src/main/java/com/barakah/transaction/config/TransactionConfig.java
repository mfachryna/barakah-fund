package com.barakah.transaction.config;

import com.barakah.transaction.service.TransactionCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@Configuration
@EnableTransactionManagement
@RequiredArgsConstructor
public class TransactionConfig {
    
    private final TransactionCategoryService categoryService;
    
    @Bean
    public ApplicationRunner initializeSystemCategories() {
        return args -> {
            try {
                log.info("Initializing system transaction categories...");
                categoryService.initializeSystemCategories();
                log.info("System transaction categories initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize system categories", e);
            }
        };
    }
}