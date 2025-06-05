package com.barakah.transaction.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Configuration
public class RateLimitConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${rate-limit.per-user.requests-per-minute:100}")
    private int userRequestsPerMinute;

    @Value("${rate-limit.global.requests-per-minute:1000}")
    private int globalRequestsPerMinute;

    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Bean
    public LettuceBasedProxyManager proxyManager() {
        RedisClient redisClient = RedisClient.create(
                RedisURI.Builder.redis(redisHost, redisPort).build()
        );
        return LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofMinutes(10)))
                .build();
    }

    @Bean
    public RateLimitService rateLimitService(LettuceBasedProxyManager proxyManager) {
        return new RateLimitService(proxyManager, userRequestsPerMinute, globalRequestsPerMinute);
    }

    public static class RateLimitService {
        private final LettuceBasedProxyManager proxyManager;
        private final int userRequestsPerMinute;
        private final int globalRequestsPerMinute;

        public RateLimitService(LettuceBasedProxyManager proxyManager,
                              int userRequestsPerMinute, int globalRequestsPerMinute) {
            this.proxyManager = proxyManager;
            this.userRequestsPerMinute = userRequestsPerMinute;
            this.globalRequestsPerMinute = globalRequestsPerMinute;
        }

        public boolean isAllowed(String userId, String endpoint) {
            return isUserAllowed(userId) && isEndpointAllowed(endpoint) && isGlobalAllowed();
        }

        private boolean isUserAllowed(String userId) {
            String key = "rate_limit:user:" + userId;
            Bucket bucket = proxyManager.builder().build(key.getBytes(), getUserBucketConfiguration());
            return bucket.tryConsume(1);
        }

        private boolean isEndpointAllowed(String endpoint) {
            String key = "rate_limit:endpoint:" + endpoint;
            Bucket bucket = proxyManager.builder().build(key.getBytes(), getEndpointBucketConfiguration(endpoint));
            return bucket.tryConsume(1);
        }

        private boolean isGlobalAllowed() {
            String key = "rate_limit:global";
            Bucket bucket = proxyManager.builder().build(key.getBytes(), getGlobalBucketConfiguration());
            return bucket.tryConsume(1);
        }

        private Supplier<BucketConfiguration> getUserBucketConfiguration() {
            return () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(userRequestsPerMinute, Refill.intervally(userRequestsPerMinute, Duration.ofMinutes(1))))
                    .build();
        }

        private Supplier<BucketConfiguration> getEndpointBucketConfiguration(String endpoint) {
            int limit = switch (endpoint) {
                case "create-transaction" -> 20;
                case "query-transaction" -> 50;
                case "list-transactions" -> 30;
                default -> 100;
            };
            
            return () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1))))
                    .build();
        }

        private Supplier<BucketConfiguration> getGlobalBucketConfiguration() {
            return () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(globalRequestsPerMinute, Refill.intervally(globalRequestsPerMinute, Duration.ofMinutes(1))))
                    .build();
        }
    }
}