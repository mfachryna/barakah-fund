package com.barakah.user.config;

import com.barakah.auth.interceptor.GrpcAuthInterceptor;
import com.barakah.auth.service.GrpcKeycloakAuthService;
import com.barakah.auth.service.KeycloakAuthService;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class GrpcConfig {

//    @Bean
//    @GrpcGlobalServerInterceptor
//    @Order(1) // Validate first
//    public ValidationInterceptor validationInterceptor() {
//        return new ValidationInterceptor();
//    }
//
    @Bean
    @GrpcGlobalServerInterceptor
    @Order(1) // Then authenticate
    public GrpcAuthInterceptor grpcAuthInterceptor() {
        KeycloakAuthService grpcKeycloakAuthService = new KeycloakAuthService();
        return new GrpcAuthInterceptor(grpcKeycloakAuthService);
    }
}