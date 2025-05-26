package com.barakah.auth.aspect;

import com.barakah.auth.annotation.RequireRole;
import com.barakah.auth.context.UserContextHolder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class AuthorizationAspect {
    
    @Around("@annotation(requireRole)")
    public Object authorize(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        var context = UserContextHolder.getContext();
        
        if (context == null) {
            throw new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Not authenticated"));
        }
        
        if (!context.hasRole(requireRole.value())) {
            throw new StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("Insufficient privileges"));
        }
        
        return joinPoint.proceed();
    }
}