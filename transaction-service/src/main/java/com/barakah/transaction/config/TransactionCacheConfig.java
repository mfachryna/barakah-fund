package com.barakah.transaction.config;

import com.barakah.shared.config.SharedCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class TransactionCacheConfig {
    @Bean("cacheManager")
    @Primary
    @ConditionalOnMissingBean(name = "cacheManager")
    public CacheManager transactionCacheManager(
            RedisConnectionFactory connectionFactory,
            @Qualifier("baseCacheConfiguration") RedisCacheConfiguration baseConfig) {

        log.info("Creating transaction-service specific CacheManager with real-time financial TTLs");

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("transactions",
                baseConfig.entryTtl(Duration.ofSeconds(30))
                        .prefixCacheNameWith("txn:transactions:"));

        cacheConfigurations.put("user-transactions",
                baseConfig.entryTtl(Duration.ofMinutes(2))
                        .prefixCacheNameWith("txn:user-txns:"));

        cacheConfigurations.put("account-transactions",
                baseConfig.entryTtl(Duration.ofMinutes(2))
                        .prefixCacheNameWith("txn:acc-txns:"));

        cacheConfigurations.put("transaction-summary",
                baseConfig.entryTtl(Duration.ofMinutes(1))
                        .prefixCacheNameWith("txn:summary:"));

        cacheConfigurations.put("user-statistics",
                baseConfig.entryTtl(Duration.ofMinutes(1))
                        .prefixCacheNameWith("txn:stats:"));

        cacheConfigurations.put("balances",
                baseConfig.entryTtl(Duration.ofSeconds(30))
                        .prefixCacheNameWith("txn:balances:"));

        cacheConfigurations.put("categories",
                baseConfig.entryTtl(Duration.ofMinutes(30))
                        .prefixCacheNameWith("txn:categories:"));

        cacheConfigurations.put("transaction-types",
                baseConfig.entryTtl(Duration.ofHours(1))
                        .prefixCacheNameWith("txn:types:"));

        cacheConfigurations.put("account-validation",
                baseConfig.entryTtl(Duration.ofSeconds(15))
                        .prefixCacheNameWith("txn:validation:"));

        cacheConfigurations.put("transaction-logs",
                baseConfig.entryTtl(Duration.ofMinutes(5))
                        .prefixCacheNameWith("txn:logs:"));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig.prefixCacheNameWith("txn:default:"))
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
