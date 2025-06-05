package com.barakah.account.config;

import com.barakah.shared.config.SharedCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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
public class AccountCacheConfig {
    @Bean("cacheManager")
    @Primary
    @ConditionalOnMissingBean(name = "cacheManager")
    public CacheManager accountCacheManager(
            RedisConnectionFactory connectionFactory,
            @Qualifier("baseCacheConfiguration") RedisCacheConfiguration baseConfig) {

        log.info("Creating account-service specific CacheManager with financial data TTLs");

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("accounts",
                baseConfig.entryTtl(Duration.ofMinutes(5))
                        .prefixCacheNameWith("acc:accounts:"));

        cacheConfigurations.put("account-by-number",
                baseConfig.entryTtl(Duration.ofMinutes(5))
                        .prefixCacheNameWith("acc:by-number:"));

        cacheConfigurations.put("account-balances",
                baseConfig.entryTtl(Duration.ofMinutes(1))
                        .prefixCacheNameWith("acc:balances:"));

        cacheConfigurations.put("user-accounts",
                baseConfig.entryTtl(Duration.ofMinutes(10))
                        .prefixCacheNameWith("acc:user-accounts:"));

        cacheConfigurations.put("account-existence",
                baseConfig.entryTtl(Duration.ofMinutes(3))
                        .prefixCacheNameWith("acc:existence:"));

        cacheConfigurations.put("account-validation",
                baseConfig.entryTtl(Duration.ofSeconds(30))
                        .prefixCacheNameWith("acc:validation:"));

        cacheConfigurations.put("account-access",
                baseConfig.entryTtl(Duration.ofMinutes(5))
                        .prefixCacheNameWith("acc:access:"));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig.prefixCacheNameWith("acc:default:"))
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
