package com.barakah.auth.interceptor;

import com.barakah.auth.context.UserContextHolder;
import com.barakah.auth.service.GrpcKeycloakAuthService;

import io.grpc.*;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.barakah.auth.service.KeycloakAuthService;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcAuthInterceptor implements ServerInterceptor {

    @Autowired
    private final KeycloakAuthService authService;

    private static final Metadata.Key<String> AUTH_KEY
            = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String method = call.getMethodDescriptor().getFullMethodName();

        if (isPublicMethod(method)) {
            return next.startCall(call, headers);
        }

        String authHeader = headers.get(AUTH_KEY);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing token"), headers);
            return new ServerCall.Listener<ReqT>() {
            };
        }

        String token = authHeader.substring(7);

        var result = authService.validateToken(token, "access_token");
        if (!result.isValid()) {
            call.close(Status.UNAUTHENTICATED.withDescription(result.getErrorMessage()), headers);
            return new ServerCall.Listener<ReqT>() {
            };
        }

        UserContextHolder.setContext(result.getUserContext());

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {
            @Override
            public void onComplete() {
                UserContextHolder.clear();
                super.onComplete();
            }

            @Override
            public void onCancel() {
                UserContextHolder.clear();
                super.onCancel();
            }
        };
    }

    private boolean isPublicMethod(String method) {
        return method.contains("AuthService")
                || method.contains("Health");
    }
}
