package com.barakah.shared.interceptor;

import com.barakah.shared.context.UserContext;
import com.barakah.shared.context.UserContextHolder;
import com.barakah.shared.exception.AuthExceptions;
import com.barakah.shared.service.KeycloakAuthService;
import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcAuthInterceptor implements ServerInterceptor {

    private final KeycloakAuthService authService;

    private static final Metadata.Key<String> AUTH_KEY
            = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> SERVICE_AUTH_KEY
            = Metadata.Key.of("x-service-auth", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USER_ID_KEY
            = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USERNAME_KEY
            = Metadata.Key.of("x-username", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ROLES_KEY
            = Metadata.Key.of("x-roles", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String method = call.getMethodDescriptor().getFullMethodName();
        log.debug("Intercepting gRPC call: {}", method);

        if (isPublicMethod(method)) {
            log.debug("Public method detected, skipping authentication: {}", method);
            return next.startCall(call, headers);
        }

        try {

            String serviceAuth = headers.get(SERVICE_AUTH_KEY);
            if (serviceAuth != null && isValidServiceAuth(serviceAuth)) {
                return handleServiceToServiceCall(call, headers, next, method);
            }
            return handleBearerTokenAuth(call, headers, next, method);

        } catch (AuthExceptions.InvalidTokenException | AuthExceptions.TokenExpiredException e) {
            log.warn("Authentication failed for method {}: {}", method, e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()), headers);
            return new ServerCall.Listener<ReqT>() {
            };

        } catch (Exception e) {
            log.error("Unexpected authentication error for method {}: {}", method, e.getMessage(), e);
            call.close(Status.INTERNAL.withDescription("Authentication service error"), headers);
            return new ServerCall.Listener<ReqT>() {
            };
        }
    }

    private <ReqT, RespT> ServerCall.Listener<ReqT> handleServiceToServiceCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next,
            String method) {

        String userId = headers.get(USER_ID_KEY);
        String username = headers.get(USERNAME_KEY);
        String rolesStr = headers.get(ROLES_KEY);
        String token = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));

        System.out.println("Service-to-service authentication details: " +
                "userId=" + userId + ", username=" + username + ", roles=" + rolesStr + ", token=" + token);

        UserContext userContext = UserContext.builder()
                .userId(userId != null ? userId : "system")
                .username(username != null ? username : "service-call")
                .roles(rolesStr != null ? Set.of(rolesStr.split(",")) : Set.of("SERVICE"))
                .token(token != null ? token.replace("Bearer ", "").trim() : null)
                .build();

        UserContextHolder.setContext(userContext);
        log.debug("Service-to-service authentication successful for method: {}", method);

        return createContextCleanupListener(next.startCall(call, headers));
    }

    private <ReqT, RespT> ServerCall.Listener<ReqT> handleBearerTokenAuth(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next,
            String method) {    

        String authHeader = headers.get(AUTH_KEY);
        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw new AuthExceptions.InvalidTokenException("Missing authorization header");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new AuthExceptions.InvalidTokenException("Invalid authorization header format. Expected: Bearer <token>");
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new AuthExceptions.InvalidTokenException("Empty token in authorization header");
        }

        var validationResult = authService.validateToken(token, "access_token");
        if (!validationResult.isValid()) {
            String errorMessage = validationResult.getErrorMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("expired") || errorMessage.contains("Expired")) {
                    throw new AuthExceptions.TokenExpiredException();
                } else if (errorMessage.contains("not active") || errorMessage.contains("invalid")) {
                    throw new AuthExceptions.InvalidTokenException(errorMessage);
                }
            }
            throw new AuthExceptions.InvalidTokenException(errorMessage != null ? errorMessage : "Token validation failed");
        }

        UserContext userContext = validationResult.getUserContext();
        userContext.setToken(token);
        UserContextHolder.setContext(userContext);
        log.debug("Bearer token authentication successful for user: {} in method: {}",
                userContext.getUsername(), method);
        log.debug("Bearer token authentication successful for user: {} in method: {}",
                validationResult.getUserContext().getUsername(), method);

        return createContextCleanupListener(next.startCall(call, headers));
    }

    private <ReqT> ServerCall.Listener<ReqT> createContextCleanupListener(ServerCall.Listener<ReqT> delegate) {
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } finally {
                    UserContextHolder.clear();
                }
            }

            @Override
            public void onCancel() {
                try {
                    super.onCancel();
                } finally {
                    UserContextHolder.clear();
                }
            }

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Exception e) {
                    UserContextHolder.clear();
                    throw e;
                }
            }
        };
    }

    private boolean isValidServiceAuth(String serviceAuth) {

        return "barakah-service-key-2024".equals(serviceAuth);
    }

    private boolean isPublicMethod(String method) {
        return method.contains("AuthService")
                || method.contains("Health")
                || method.contains("Register")
                || method.contains("Login")
                || method.contains("RefreshToken")
                || method.contains("ForgotPassword")
                || method.contains("ResetPassword");
    }

}
