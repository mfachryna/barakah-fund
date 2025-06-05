package com.barakah.gateway.security;

import com.barakah.gateway.dto.auth.ValidateTokenRequestDto;
import com.barakah.gateway.dto.auth.ValidateTokenResponseDto;
import com.barakah.gateway.dto.common.ErrorResponse;
import com.barakah.gateway.service.AuthGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final AuthGatewayService authService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    ValidateTokenRequestDto validateRequest = ValidateTokenRequestDto.builder()
                            .token(token)
                            .build();

                    ValidateTokenResponseDto validation = authService.validateToken(validateRequest);

                    if (validation.getValid()) {
                        List<SimpleGrantedAuthority> authorities = validation.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .collect(Collectors.toList());

                        authorities.addAll(validation.getPermissions().stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()));

                        UsernamePasswordAuthenticationToken authentication
                                = new UsernamePasswordAuthenticationToken(
                                        validation.getUserId(),
                                        null,
                                        authorities
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("Successfully authenticated user: {}", validation.getUsername());
                    } else {
                        log.debug("Token validation failed - invalid token for request: {}", request.getRequestURI());
                        handleInvalidToken(request, response, "Invalid or expired token");
                        return;
                    }
                } catch (StatusRuntimeException e) {
                    Status.Code code = e.getStatus().getCode();
                    if (code == Status.Code.UNAUTHENTICATED || code == Status.Code.PERMISSION_DENIED) {
                        log.debug("Token validation failed - authentication error: {}", e.getStatus().getDescription());
                        handleInvalidToken(request, response, "Invalid or expired token");
                        return;
                    } else if (code == Status.Code.UNAVAILABLE) {
                        log.warn("Authentication service unavailable for request: {}", request.getRequestURI());
                        handleServiceUnavailable(request, response);
                        return;
                    } else {
                        log.error("Unexpected error during token validation: {}", e.getMessage());                    }
                } catch (Exception e) {
                    log.error("Unexpected error during token validation: {}", e.getMessage());                }
            }
        } catch (Exception e) {
            log.error("Cannot process authentication", e);
        }

        filterChain.doFilter(request, response);
    }

    private void handleInvalidToken(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {

        log.debug("Sending 401 response for invalid token - Path: {}", request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(message)
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private void handleServiceUnavailable(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        log.warn("Sending 503 response for authentication service unavailable - Path: {}", request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Authentication service is temporarily unavailable")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/webjars/");
    }
}
