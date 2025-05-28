package com.barakah.shared.config;


import com.barakah.shared.interceptor.GrpcAuthInterceptor;
import com.barakah.shared.service.KeycloakAuthService;
import io.grpc.ServerBuilder;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcConfig {

    private static void accept(ServerBuilder<?> serverBuilder) {
        if (serverBuilder != null) {

            ((ServerBuilder<?>) serverBuilder).keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(5, TimeUnit.SECONDS)
                    .permitKeepAliveWithoutCalls(true)
                    .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
                    .maxInboundMetadataSize(8192); // 8KB
        }
    }

    @Bean
    public GrpcServerConfigurer grpcServerConfigurer() {
        return GrpcConfig::accept;
    }

    @Bean
    @GrpcGlobalServerInterceptor
    @Order(1)
    public GrpcAuthInterceptor grpcAuthInterceptor(KeycloakAuthService grpcKeycloakAuthService) {
        return new GrpcAuthInterceptor(grpcKeycloakAuthService);
    }
}
