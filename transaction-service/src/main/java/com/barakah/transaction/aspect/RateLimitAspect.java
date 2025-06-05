package com.barakah.transaction.aspect;

import com.barakah.transaction.config.RateLimitConfig;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitConfig.RateLimitService rateLimitService;

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RateLimit {

        String endpoint() default "";

        String userIdParameter() default "userId";
    }

    @Around("@annotation(rateLimit)")
    public Object handleRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String endpoint = rateLimit.endpoint().isEmpty()
                ? joinPoint.getSignature().getName() : rateLimit.endpoint();

        String userId = extractUserId(joinPoint, rateLimit.userIdParameter());

        if (!rateLimitService.isAllowed(userId, endpoint)) {
            log.warn("Rate limit exceeded for user {} on endpoint {}", userId, endpoint);
            throw new StatusRuntimeException(
                    Status.RESOURCE_EXHAUSTED
                            .withDescription("Rate limit exceeded. Please try again later.")
            );
        }

        log.debug("Rate limit check passed for user {} on endpoint {}", userId, endpoint);
        return joinPoint.proceed();
    }

    private String extractUserId(ProceedingJoinPoint joinPoint, String userIdParameter) {

        return "default-user";
    }
}
