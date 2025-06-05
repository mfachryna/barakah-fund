package com.barakah.gateway.service;

import com.barakah.common.proto.v1.Empty;
import com.barakah.gateway.dto.user.*;
import com.barakah.gateway.mapper.UserMapper;
import com.barakah.shared.annotation.RateLimit;
import com.barakah.shared.util.GrpcErrorHandler;
import com.barakah.user.proto.v1.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserGatewayService {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    private final UserMapper userMapper;

    @RateLimit(endpoint = "gateway-get-current-user")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetCurrentUser")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-queries")
    @Cacheable(value = "gateway-current-user", key = "T(com.barakah.shared.context.UserContextHolder).getCurrentUserId()")
    public UserResponseDto getCurrentUser() {
        GetUserResponse response = userServiceStub.getCurrentUser(Empty.newBuilder().build());
        return userMapper.toDto(response.getUser());
    }

    @RateLimit(endpoint = "gateway-get-user-by-id")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUserById")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-queries")
    @Cacheable(value = "gateway-users", key = "#userId")
    public UserResponseDto getUserById(String userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(userId)
                .build();
        GetUserResponse response = userServiceStub.getUser(request);
        return userMapper.toDto(response.getUser());
    }

    @RateLimit(endpoint = "gateway-list-users")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackListUsers")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-queries")
    @Cacheable(value = "gateway-user-lists",
            key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
    public Page<UserResponseDto> listUsers(Pageable pageable) {
        ListUsersRequest request = ListUsersRequest.newBuilder()
                .setPageRequest(userMapper.toPageRequest(pageable))
                .build();
        ListUsersResponse response = userServiceStub.listUsers(request);

        List<UserResponseDto> users = response.getUsersList().stream()
                .map(userMapper::toDto)
                .toList();

        return new PageImpl<>(users, pageable, response.getPageResponse().getTotalElements());
    }

    @RateLimit(endpoint = "gateway-create-user")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackCreateUser")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-mutations")
    @Bulkhead(name = "gateway-user-creation")
    @CacheEvict(value = {"gateway-user-lists", "gateway-users"}, allEntries = true)
    public UserResponseDto createUser(CreateUserRequestDto request) {
        CreateUserRequest grpcRequest = userMapper.toGrpcCreateRequest(request);
        CreateUserResponse response = userServiceStub.createUser(grpcRequest);
        return userMapper.toDto(response.getUser());
    }

    @RateLimit(endpoint = "gateway-update-user")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackUpdateUser")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-mutations")
    @Bulkhead(name = "gateway-user-mutations")
    @Caching(evict = {
            @CacheEvict(value = "gateway-users", key = "#userId"),
            @CacheEvict(value = "gateway-current-user", key = "#userId"),
            @CacheEvict(value = "gateway-user-lists", allEntries = true)
    })
    public UserResponseDto updateUser(String userId, UpdateUserRequestDto request) {
        UpdateUserRequest grpcRequest = userMapper.toGrpcUpdateRequest(userId, request);
        UpdateUserResponse response = userServiceStub.updateUser(grpcRequest);
        return userMapper.toDto(response.getUser());
    }

    @RateLimit(endpoint = "gateway-delete-user")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackDeleteUser")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-mutations")
    @Bulkhead(name = "gateway-user-mutations")
    @Caching(evict = {
            @CacheEvict(value = "gateway-users", key = "#userId"),
            @CacheEvict(value = "gateway-current-user", key = "#userId"),
            @CacheEvict(value = "gateway-user-lists", allEntries = true)
    })
    public void deleteUser(String userId) {
        DeleteUserRequest request = DeleteUserRequest.newBuilder()
                .setUserId(userId)
                .build();
        userServiceStub.deleteUser(request);
    }

    @RateLimit(endpoint = "grpc-get-current-user")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetCurrentUserGrpc")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-queries")
    public GetUserResponse getCurrentUserGrpc() {
        return userServiceStub.getCurrentUser(Empty.newBuilder().build());
    }

    @RateLimit(endpoint = "grpc-get-user-by-id")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUserByIdGrpc")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-queries")
    public GetUserResponse getUserByIdGrpc(GetUserRequest request) {
        return userServiceStub.getUser(request);
    }

    @RateLimit(endpoint = "grpc-list-users")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackListUsersGrpc")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-queries")
    public ListUsersResponse listUsersGrpc(ListUsersRequest request) {
        return userServiceStub.listUsers(request);
    }

    @RateLimit(endpoint = "grpc-create-user")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackCreateUserGrpc")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-mutations")
    @Bulkhead(name = "gateway-user-creation")
    public CreateUserResponse createUserGrpc(CreateUserRequest request) {
        return userServiceStub.createUser(request);
    }

    @RateLimit(endpoint = "grpc-update-user")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackUpdateUserGrpc")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-mutations")
    @Bulkhead(name = "gateway-user-mutations")
    public UpdateUserResponse updateUserGrpc(UpdateUserRequest request) {
        return userServiceStub.updateUser(request);
    }

    @RateLimit(endpoint = "grpc-delete-user")
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackDeleteUserGrpc")
    @Retry(name = "user-service")
    @RateLimiter(name = "gateway-user-mutations")
    @Bulkhead(name = "gateway-user-mutations")
    public DeleteUserResponse deleteUserGrpc(DeleteUserRequest request) {
        return userServiceStub.deleteUser(request);
    }

    public UserResponseDto fallbackGetCurrentUser(Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "get current user",
                "User service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }

    public UserResponseDto fallbackGetUserById(String userId, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "get user by ID: " + userId,
                ex
        );
        return null;
    }

    public Page<UserResponseDto> fallbackListUsers(Pageable pageable, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "list users",
                ex
        );
        return null;
    }

    public UserResponseDto fallbackCreateUser(CreateUserRequestDto request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "create user",
                "User service is currently unavailable. Please try again later.",
                ex
        );
        return null;
    }

    public UserResponseDto fallbackUpdateUser(String userId, UpdateUserRequestDto request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "update user: " + userId,
                ex
        );
        return null;
    }

    public void fallbackDeleteUser(String userId, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "delete user: " + userId,
                ex
        );

    }

    public GetUserResponse fallbackGetCurrentUserGrpc(Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "get current user gRPC",
                ex
        );
        return null;
    }

    public GetUserResponse fallbackGetUserByIdGrpc(GetUserRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "get user by ID gRPC: " + request.getUserId(),
                ex
        );
        return null;
    }

    public ListUsersResponse fallbackListUsersGrpc(ListUsersRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "list users gRPC",
                ex
        );
        return null;
    }

    public CreateUserResponse fallbackCreateUserGrpc(CreateUserRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "create user gRPC",
                ex
        );
        return null;
    }

    public UpdateUserResponse fallbackUpdateUserGrpc(UpdateUserRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "update user gRPC: " + request.getEmail(),
                ex
        );
        return null;
    }

    public DeleteUserResponse fallbackDeleteUserGrpc(DeleteUserRequest request, Exception ex) {
        GrpcErrorHandler.handleFallbackError(
                "User",
                "delete user gRPC: " + request.getUserId(),
                ex
        );
        return null;
    }
}
