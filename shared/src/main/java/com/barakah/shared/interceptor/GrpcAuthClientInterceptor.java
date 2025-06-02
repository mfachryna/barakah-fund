package com.barakah.shared.interceptor;

import com.barakah.shared.context.UserContext;
import com.barakah.shared.context.UserContextHolder;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcAuthClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> USER_ID_KEY =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USERNAME_KEY =
            Metadata.Key.of("x-username", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ROLES_KEY =
            Metadata.Key.of("x-roles", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TOKEN_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> SERVICE_AUTH_KEY =
            Metadata.Key.of("x-service-auth", Metadata.ASCII_STRING_MARSHALLER);

    @Value("${vault:service-auth#internal-key:barakah-service-key-2024}")
    private String serviceAuthToken;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                headers.put(SERVICE_AUTH_KEY, serviceAuthToken);
                UserContext currentUser = UserContextHolder.getContext();
                log.debug("Current User in client interceptor: {}", currentUser);
                
                if (currentUser != null) {
                    headers.put(USER_ID_KEY, currentUser.getUserId());
                    headers.put(USERNAME_KEY, currentUser.getUsername());

                    if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
                        headers.put(ROLES_KEY, String.join(",", currentUser.getRoles()));
                    }

                    if (currentUser.getToken() != null) {
                        headers.put(TOKEN_KEY, "Bearer " + currentUser.getToken());
                    }

                    log.debug("Added auth metadata for user: {}", currentUser.getUsername());
                }

                super.start(responseListener, headers);
            }
        };
    }
}
