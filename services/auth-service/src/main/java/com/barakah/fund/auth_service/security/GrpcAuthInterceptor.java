package com.barakah.fund.auth_service.security;

import com.barakah.fund.auth_service.config.KeycloakProperties;
import com.barakah.fund.auth_service.service.abstracts.token.TokenService;
import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.security.check.GrpcSecurityMetadataSource;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GrpcGlobalServerInterceptor
public class GrpcAuthInterceptor implements ServerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(GrpcAuthInterceptor.class);
    private static final Context.Key<AccessToken> TOKEN_CONTEXT_KEY = Context.key("token");
    private static final Context.Key<Authentication> AUTH_CONTEXT_KEY = Context.key("authentication");
    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final TokenService tokenService;
    private final KeycloakProperties keycloakProperties;

    @Autowired
    public GrpcAuthInterceptor(TokenService tokenService, KeycloakProperties keycloakProperties) {
        this.tokenService = tokenService;
        this.keycloakProperties = keycloakProperties;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        String methodName = fullMethodName.substring(fullMethodName.lastIndexOf('/') + 1);

        logger.info("Intercepting gRPC call to method: {}", methodName);

        if (isPublicMethod(methodName)) {
            logger.info("Skipping authentication for public method: {}", methodName);
            return next.startCall(call, headers);
        }

        String authHeader = headers.get(AUTHORIZATION_METADATA_KEY);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Missing or invalid authorization header for method: {}", methodName);
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid authorization header"), headers);
            return new ServerCall.Listener<ReqT>() {
            };
        }

        String token = authHeader.substring(7);

        try {
            boolean isValid = tokenService.validateToken(token);

            if (!isValid) {
                logger.error("Token validation failed for method: {}", methodName);
                call.close(Status.UNAUTHENTICATED.withDescription("Invalid or expired token"), headers);
                return new ServerCall.Listener<ReqT>() {
                };
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "authenticated-user", "(protected)", authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Context context = Context.current().withValue(AUTH_CONTEXT_KEY, authentication);

            logger.info("User successfully authenticated for method: {}", methodName);

            return Contexts.interceptCall(context, call, headers, next);

        } catch (Exception e) {
            logger.error("Authentication error for method {}: {}", methodName, e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Authentication failed: " + e.getMessage()), headers);
            return new ServerCall.Listener<ReqT>() {
            };
        } finally {

            SecurityContextHolder.clearContext();
        }
    }

    private boolean isPublicMethod(String methodName) {
        return "Register".equals(methodName)
                || "Authenticate".equals(methodName)
                || "RefreshToken".equals(methodName);
    }

    private boolean requiresSpecialPermission(String methodName) {
        return "ValidateToken".equals(methodName);
    }

    private boolean hasRequiredPermission(AccessToken token, String methodName) {
        if ("ValidateToken".equals(methodName)) {
            return hasAdminRole(token);
        }
        return true;
    }

    private AccessToken verifyAndParseToken(String tokenString) throws VerificationException {
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class);
        verifier.withChecks(TokenVerifier.IS_ACTIVE);

        return verifier.verify().getToken();
    }

    private boolean hasAdminRole(AccessToken token) {
        return token.getRealmAccess() != null
                && token.getRealmAccess().getRoles() != null
                && token.getRealmAccess().getRoles().contains("admin");
    }

    private Authentication createSpringSecurityAuthentication(AccessToken token) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (token.getRealmAccess() != null && token.getRealmAccess().getRoles() != null) {
            Set<SimpleGrantedAuthority> realmRoles = token.getRealmAccess().getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toSet());
            authorities.addAll(realmRoles);
        }

        if (token.getResourceAccess() != null) {
            token.getResourceAccess().forEach((client, access) -> {
                if (access.getRoles() != null) {
                    access.getRoles().forEach(role -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                    });
                }
            });
        }

        return new UsernamePasswordAuthenticationToken(
                token.getPreferredUsername() != null ? token.getPreferredUsername() : token.getSubject(),
                "(protected)",
                authorities);
    }
}
