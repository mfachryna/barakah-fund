package com.barakah.shared.config;

import com.barakah.shared.interceptor.GrpcAuthClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class GrpcClientConfig {
    
    @Bean
    @GrpcGlobalClientInterceptor
    @Order(1)
    public GrpcAuthClientInterceptor authenticationClientInterceptor() {
        return new GrpcAuthClientInterceptor();
    }
}
