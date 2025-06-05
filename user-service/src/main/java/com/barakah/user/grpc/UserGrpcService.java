package com.barakah.user.grpc;

import com.barakah.common.proto.v1.Empty;
import com.barakah.common.proto.v1.PageResponse;
import com.barakah.shared.annotation.RateLimit;
import com.barakah.user.dto.UserResponse;
import com.barakah.user.proto.v1.*;
import com.barakah.user.service.UserService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import com.barakah.shared.context.UserContext;
import com.barakah.shared.context.UserContextHolder;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    @RateLimiter(name = "user-queries")
    @Override
    public void getCurrentUser(Empty request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            log.info("Getting current user");

            UserContext currentUser = UserContextHolder.getContext();
            if (currentUser == null || currentUser.getUserId() == null) {
                handleGrpcError(responseObserver,
                        Status.UNAUTHENTICATED.withDescription("User context not found or invalid"));
                return;
            }

            log.debug("Current user context: {}", currentUser);

            UserResponse user = userService.getUserByKeycloakId(currentUser.getUserId());

            GetUserResponse response = GetUserResponse.newBuilder()
                    .setUser(buildUserProto(user))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully returned current user data for: {}", currentUser.getUsername());

        } catch (Exception e) {
            log.error("Failed to get current user: {}", e.getMessage());
            handleGrpcError(responseObserver, e);
        }
    }

    @RateLimiter(name = "user-queries")
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            log.info("Getting user by id: {}", request.getUserId());

            if (request.getUserId().isEmpty()) {
                handleGrpcError(responseObserver,
                        Status.INVALID_ARGUMENT.withDescription("User ID cannot be empty"));
                return;
            }

            UserResponse user = userService.getUserById(request.getUserId());

            GetUserResponse response = GetUserResponse.newBuilder()
                    .setUser(buildUserProto(user))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully returned user data for id: {}", request.getUserId());

        } catch (Exception e) {
            log.error("Failed to get user by ID {}: {}", request.getUserId(), e.getMessage());
            handleGrpcError(responseObserver, e);
        }
    }

    @RateLimit(endpoint = "grpc-create-user")
    @Bulkhead(name = "user-operations")
    @RateLimiter(name = "user-creation")
    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        UserContext currentUser = UserContextHolder.getContext();

        try {
            log.info("Creating user: {} by: {}",
                    request.getUsername(),
                    currentUser != null ? currentUser.getUsername() : "system");

            validateCreateUserRequest(request);

            UserResponse result = userService.createUser(request);

            CreateUserResponse response = CreateUserResponse.newBuilder()
                    .setUser(buildUserProto(result))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully created user: {} with ID: {}", request.getUsername(), result.getUserId());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid create user request: {}", e.getMessage());
            handleGrpcError(responseObserver,
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create user {}: {}", request.getUsername(), e.getMessage());
            handleGrpcError(responseObserver, e);
        }
    }

    @RateLimit(endpoint = "grpc-update-user")
    @Bulkhead(name = "user-operations")
    @RateLimiter(name = "user-queries")
    @Override
    public void updateUser(UpdateUserRequest request, StreamObserver<UpdateUserResponse> responseObserver) {
        try {
            log.info("Updating user");

            UserContext currentUser = UserContextHolder.getContext();
            if (currentUser == null || currentUser.getUserId() == null) {
                handleGrpcError(responseObserver,
                        Status.UNAUTHENTICATED.withDescription("User context not found or invalid"));
                return;
            }

            validateUpdateUserRequest(request);

            UserResponse result = userService.updateUser(request, currentUser.getUserId());

            UpdateUserResponse response = UpdateUserResponse.newBuilder()
                    .setUser(buildUserProto(result))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully updated user: {}", currentUser.getUserId());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid update user request: {}", e.getMessage());
            handleGrpcError(responseObserver,
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update user: {}", e.getMessage());
            handleGrpcError(responseObserver, e);
        }
    }

    @RateLimit(endpoint = "grpc-delete-user")
    @Bulkhead(name = "user-operations")
    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
        UserContext currentUser = UserContextHolder.getContext();

        try {
            log.info("Deleting user: {} by: {}",
                    request.getUserId(),
                    currentUser != null ? currentUser.getUsername() : "system");

            if (request.getUserId().isEmpty()) {
                handleGrpcError(responseObserver,
                        Status.INVALID_ARGUMENT.withDescription("User ID cannot be empty"));
                return;
            }

            boolean deleted = userService.deleteUser(request.getUserId());

            if (!deleted) {
                handleGrpcError(responseObserver,
                        Status.INTERNAL.withDescription("Failed to delete user"));
                return;
            }

            DeleteUserResponse response = DeleteUserResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("User deleted successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully deleted user: {}", request.getUserId());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid delete user request: {}", e.getMessage());
            handleGrpcError(responseObserver,
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", request.getUserId(), e.getMessage());
            handleGrpcError(responseObserver, e);
        }
    }

    @RateLimiter(name = "user-queries")
    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        try {
            UserContext currentUser = UserContextHolder.getContext();
            log.info("Listing users requested by: {}",
                    currentUser != null ? currentUser.getUsername() : "Anonymous");

            int page = Math.max(request.getPageRequest().getPage(), 0);
            int size = request.getPageRequest().getSize();

            if (size <= 0) {
                size = 10;
            }
            if (size > 100) {
                handleGrpcError(responseObserver,
                        Status.INVALID_ARGUMENT.withDescription("Page size cannot exceed 100"));
                return;
            }

            log.info("Listing users with page: {}, size: {}", page, size);

            Pageable pageable = PageRequest.of(
                    page > 0 ? page - 1 : 0,
                    size
            );

            Page<UserResponse> userPage = userService.getAllUsers(pageable);

            ListUsersResponse.Builder responseBuilder = ListUsersResponse.newBuilder();

            for (UserResponse userDto : userPage.getContent()) {
                responseBuilder.addUsers(buildUserProto(userDto));
            }

            responseBuilder.setPageResponse(
                    PageResponse.newBuilder()
                            .setPage(page)
                            .setSize(size)
                            .setTotalElements(userPage.getTotalElements())
                            .setFirst(userPage.isFirst())
                            .setLast(userPage.isLast())
                            .setTotalPages(userPage.getTotalPages())
                            .build()
            );

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            log.info("Successfully returned {} users (page {}/{})",
                    userPage.getNumberOfElements(),
                    page,
                    userPage.getTotalPages());

        } catch (Exception e) {
            log.error("Failed to list users: {}", e.getMessage());
            handleGrpcError(responseObserver, e);
        }
    }

    @RateLimiter(name = "user-queries")
    @Override
    public void getUserByUsername(GetUserByUsernameRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            log.info("Getting user by username: {}", request.getUsername());

            if (request.getUsername().isEmpty()) {
                handleGrpcError(responseObserver,
                        Status.INVALID_ARGUMENT.withDescription("Username cannot be empty"));
                return;
            }

            UserResponse user = userService.getUserByUsername(request.getUsername());

            GetUserResponse response = GetUserResponse.newBuilder()
                    .setUser(buildUserProto(user))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully returned user data for username: {}", request.getUsername());

        } catch (Exception e) {
            log.error("Failed to get user by username {}: {}", request.getUsername(), e.getMessage());
            handleGrpcError(responseObserver, e);
        }
    }

    @RateLimiter(name = "user-queries")
    @Override
    public void getUserByEmail(GetUserByEmailRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            log.info("Getting user by email: {}", request.getEmail());

            if (request.getEmail().isEmpty()) {
                handleGrpcError(responseObserver,
                        Status.INVALID_ARGUMENT.withDescription("Email cannot be empty"));
                return;
            }

            if (!isValidEmail(request.getEmail())) {
                handleGrpcError(responseObserver,
                        Status.INVALID_ARGUMENT.withDescription("Invalid email format"));
                return;
            }

            UserResponse user = userService.getUserByEmail(request.getEmail());

            GetUserResponse response = GetUserResponse.newBuilder()
                    .setUser(buildUserProto(user))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully returned user data for email: {}", request.getEmail());

        } catch (Exception e) {
            log.error("Failed to get user by email {}: {}", request.getEmail(), e.getMessage());
            handleGrpcError(responseObserver, e);
        }
    }

    private void handleGrpcError(StreamObserver<?> responseObserver, Exception exception) {
        Status status = determineGrpcStatus(exception);
        String message = exception.getMessage() != null ? exception.getMessage() : "An error occurred";

        responseObserver.onError(status.withDescription(message).withCause(exception).asRuntimeException());
    }

    private void handleGrpcError(StreamObserver<?> responseObserver, Status status) {
        responseObserver.onError(status.asRuntimeException());
    }

    private Status determineGrpcStatus(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";

        if (exceptionName.contains("NotFound") || message.contains("not found")) {
            return Status.NOT_FOUND;
        } else if (exceptionName.contains("AlreadyExists") || message.contains("already exists")) {
            return Status.ALREADY_EXISTS;
        } else if (exceptionName.contains("InvalidArgument") || exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT;
        } else if (exceptionName.contains("Security") || exceptionName.contains("Unauthorized")
                || exception instanceof SecurityException) {
            return Status.UNAUTHENTICATED;
        } else if (exceptionName.contains("AccessDenied") || exceptionName.contains("Forbidden")) {
            return Status.PERMISSION_DENIED;
        } else if (exceptionName.contains("Timeout") || message.contains("timeout")) {
            return Status.DEADLINE_EXCEEDED;
        } else if (exceptionName.contains("Unavailable") || exceptionName.contains("Connection")
                || message.contains("unavailable") || message.contains("connection")) {
            return Status.UNAVAILABLE;
        } else if (message.contains("rate limit") || message.contains("too many requests")) {
            return Status.RESOURCE_EXHAUSTED;
        } else {
            return Status.INTERNAL;
        }
    }

    private User buildUserProto(UserResponse userDto) {
        return User.newBuilder()
                .setUserId(userDto.getUserId())
                .setUsername(userDto.getUsername())
                .setEmail(userDto.getEmail())
                .setFirstName(userDto.getFirstName())
                .setLastName(userDto.getLastName())
                .setPhoneNumber(userDto.getPhoneNumber() != null ? userDto.getPhoneNumber() : "")
                .setAddress(userDto.getAddress() != null ? userDto.getAddress() : "")
                .setDateOfBirth(userDto.getDateOfBirth() != null ? userDto.getDateOfBirth().toString() : "")
                .setStatus(convertToProtoStatus(userDto.getStatus()))
                .setCreatedAt(userDto.getCreatedAt() != null ? userDto.getCreatedAt().toString() : "")
                .setUpdatedAt(userDto.getUpdatedAt() != null ? userDto.getUpdatedAt().toString() : "")
                .build();
    }

    private void validateCreateUserRequest(CreateUserRequest request) {
        if (request.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (request.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (request.getFirstName().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }

        if (request.getLastName().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }

        if (request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
    }

    private void validateUpdateUserRequest(UpdateUserRequest request) {
        if (request.getEmail() != null && !request.getEmail().isEmpty() && !isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (request.getFirstName() != null && request.getFirstName().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }

        if (request.getLastName() != null && request.getLastName().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null
                && email.contains("@")
                && email.contains(".")
                && email.length() > 5;
    }

    private UserStatus convertToProtoStatus(com.barakah.user.entity.UserStatus entityStatus) {
        if (entityStatus == null) {
            return UserStatus.ACTIVE;
        }

        switch (entityStatus) {
            case ACTIVE:
                return UserStatus.ACTIVE;
            case INACTIVE:
                return UserStatus.INACTIVE;
            case SUSPENDED:
                return UserStatus.SUSPENDED;
            case LOCKED:
                return UserStatus.LOCKED;
            default:
                return UserStatus.ACTIVE;
        }
    }
}
