package com.barakah.gateway.service;

import com.barakah.common.proto.v1.Empty;
import com.barakah.gateway.dto.user.*;
import com.barakah.gateway.mapper.UserMapper;
import com.barakah.user.proto.v1.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
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

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetCurrentUser")
    @Retry(name = "user-service")
    public UserResponseDto getCurrentUser() {
        try {
            GetUserResponse response = userServiceStub.getCurrentUser(Empty.newBuilder().build());
            return userMapper.toDto(response.getUser());
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            throw new RuntimeException("Failed to get current user", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetCurrentUser")
    @Retry(name = "user-service")
    public GetUserResponse getCurrentUserGrpc() {
        try {
            return userServiceStub.getCurrentUser(Empty.newBuilder().build());
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            throw new RuntimeException("Failed to get current user", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUserById")
    @Retry(name = "user-service")
    public UserResponseDto getUserById(String userId) {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            GetUserResponse response = userServiceStub.getUser(request);
            return userMapper.toDto(response.getUser());
        } catch (Exception e) {
            log.error("Failed to get user by ID: {}", userId, e);
            throw new RuntimeException("Failed to get user: " + userId, e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUserById")
    @Retry(name = "user-service")
    public GetUserResponse getUserByIdGrpc(GetUserRequest request) {
        try {
            return userServiceStub.getUser(request);
        } catch (Exception e) {
            log.error("Failed to get user by ID: {}", request.getUserId(), e);
            throw new RuntimeException("Failed to get user: " + request.getUserId(), e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackListUsers")
    @Retry(name = "user-service")
    public Page<UserResponseDto> listUsers(Pageable pageable) {
        try {
            ListUsersRequest request = ListUsersRequest.newBuilder()
                    .setPageRequest(userMapper.toPageRequest(pageable))
                    .build();
            ListUsersResponse response = userServiceStub.listUsers(request);

            List<UserResponseDto> users = response.getUsersList().stream()
                    .map(userMapper::toDto)
                    .toList();

            return new PageImpl<>(users, pageable, response.getPageResponse().getTotalElements());
        } catch (Exception e) {
            log.error("Failed to list users", e);
            throw new RuntimeException("Failed to list users", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackCreateUser")
    @Retry(name = "user-service")
    public UserResponseDto createUser(CreateUserRequestDto request) {
        try {
            CreateUserRequest grpcRequest = userMapper.toGrpcCreateRequest(request);
            CreateUserResponse response = userServiceStub.createUser(grpcRequest);
            return userMapper.toDto(response.getUser());
        } catch (Exception e) {
            log.error("Failed to create user", e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackUpdateUser")
    @Retry(name = "user-service")
    public UserResponseDto updateUser(String userId, UpdateUserRequestDto request) {
        try {
            UpdateUserRequest grpcRequest = userMapper.toGrpcUpdateRequest(userId, request);
            UpdateUserResponse response = userServiceStub.updateUser(grpcRequest);
            return userMapper.toDto(response.getUser());
        } catch (Exception e) {
            log.error("Failed to update user: {}", userId, e);
            throw new RuntimeException("Failed to update user: " + userId, e);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackDeleteUser")
    @Retry(name = "user-service")
    public void deleteUser(String userId) {
        try {
            DeleteUserRequest request = DeleteUserRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            userServiceStub.deleteUser(request);
        } catch (Exception e) {
            log.error("Failed to delete user: {}", userId, e);
            throw new RuntimeException("Failed to delete user: " + userId, e);
        }
    }

    // Fallback methods
    public UserResponseDto fallbackGetCurrentUser(Exception ex) {
        log.warn("Fallback: Failed to get current user", ex);
        throw new RuntimeException("User service is currently unavailable", ex);
    }

    public UserResponseDto fallbackGetUserById(String userId, Exception ex) {
        log.warn("Fallback: Failed to get user by ID: {}", userId, ex);
        throw new RuntimeException("User service is currently unavailable", ex);
    }

    public Page<UserResponseDto> fallbackListUsers(Pageable pageable, Exception ex) {
        log.warn("Fallback: Failed to list users", ex);
        throw new RuntimeException("User service is currently unavailable", ex);
    }

    public UserResponseDto fallbackCreateUser(CreateUserRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to create user", ex);
        throw new RuntimeException("User service is currently unavailable", ex);
    }

    public UserResponseDto fallbackUpdateUser(String userId, UpdateUserRequestDto request, Exception ex) {
        log.warn("Fallback: Failed to update user: {}", userId, ex);
        throw new RuntimeException("User service is currently unavailable", ex);
    }

    public void fallbackDeleteUser(String userId, Exception ex) {
        log.warn("Fallback: Failed to delete user: {}", userId, ex);
        throw new RuntimeException("User service is currently unavailable", ex);
    }
}