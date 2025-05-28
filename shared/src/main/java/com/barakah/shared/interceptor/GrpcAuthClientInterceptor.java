package com.barakah.shared.interceptor;

import com.barakah.shared.context.UserContext;
import com.barakah.shared.context.UserContextHolder;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcAuthClientInterceptor implements ClientInterceptor {
    
    private static final Metadata.Key<String> USER_ID_KEY = 
            Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USERNAME_KEY = 
            Metadata.Key.of("username", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ROLES_KEY = 
            Metadata.Key.of("roles", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TOKEN_KEY = 
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                UserContext currentUser = UserContextHolder.getContext();
                
                if (currentUser != null) {

                    headers.put(USER_ID_KEY, currentUser.getUserId());
                    headers.put(USERNAME_KEY, currentUser.getUsername());
                    headers.put(ROLES_KEY, String.join(",", currentUser.getRoles()));
                    
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
