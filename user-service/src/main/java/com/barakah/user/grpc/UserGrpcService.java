package com.barakah.user.grpc;

import com.barakah.common.proto.v1.Empty;
import com.barakah.common.proto.v1.PageResponse;
import com.barakah.user.dto.UserResponse;
import com.barakah.user.proto.v1.*;
import com.barakah.user.service.UserService;
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

    @Override
    public void getCurrentUser(Empty request, StreamObserver<GetUserResponse> responseObserver) {
        log.info("Getting current user");

        UserContext currentUser = UserContextHolder.getContext();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new SecurityException("User context not found or invalid");
        }

        log.debug("Current user context: {}", currentUser);

        UserResponse user = userService.getUserByKeycloakId(currentUser.getUserId());

        GetUserResponse response = GetUserResponse.newBuilder()
                .setUser(buildUserProto(user))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully returned current user data for: {}", currentUser.getUsername());
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        log.info("Getting user by id: {}", request.getUserId());


        if (request.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        UserResponse user = userService.getUserById(request.getUserId());

        GetUserResponse response = GetUserResponse.newBuilder()
                .setUser(buildUserProto(user))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully returned user data for id: {}", request.getUserId());
    }

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        log.info("Creating user: {}", request.getUsername());


        validateCreateUserRequest(request);

        UserResponse result = userService.createUser(request);

        CreateUserResponse response = CreateUserResponse.newBuilder()
                .setUser(buildUserProto(result))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully created user: {} with ID: {}", request.getUsername(), result.getUserId());
    }

    @Override
    public void updateUser(UpdateUserRequest request, StreamObserver<UpdateUserResponse> responseObserver) {
        log.info("Updating user");

        UserContext currentUser = UserContextHolder.getContext();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new SecurityException("User context not found or invalid");
        }


        validateUpdateUserRequest(request);

        UserResponse result = userService.updateUser(request, currentUser.getUserId());

        UpdateUserResponse response = UpdateUserResponse.newBuilder()
                .setUser(buildUserProto(result))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully updated user: {}", currentUser.getUserId());
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
        log.info("Deleting user: {}", request.getUserId());


        if (request.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        boolean deleted = userService.deleteUser(request.getUserId());

        if (!deleted) {
            throw new RuntimeException("Failed to delete user");
        }

        DeleteUserResponse response = DeleteUserResponse.newBuilder()
                .setSuccess(true)
                .setMessage("User deleted successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully deleted user: {}", request.getUserId());
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        UserContext currentUser = UserContextHolder.getContext();
        log.info("Listing users requested by: {}",
                currentUser != null ? currentUser.getUsername() : "Anonymous");


        int page = Math.max(request.getPageRequest().getPage(), 0);
        int size = request.getPageRequest().getSize();

        if (size <= 0) {
            size = 10;
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
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
    }

    @Override
    public void getUserByUsername(GetUserByUsernameRequest request, StreamObserver<GetUserResponse> responseObserver) {
        log.info("Getting user by username: {}", request.getUsername());


        if (request.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        UserResponse user = userService.getUserByUsername(request.getUsername());

        GetUserResponse response = GetUserResponse.newBuilder()
                .setUser(buildUserProto(user))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully returned user data for username: {}", request.getUsername());
    }

    @Override
    public void getUserByEmail(GetUserByEmailRequest request, StreamObserver<GetUserResponse> responseObserver) {
        log.info("Getting user by email: {}", request.getEmail());


        if (request.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        UserResponse user = userService.getUserByEmail(request.getEmail());

        GetUserResponse response = GetUserResponse.newBuilder()
                .setUser(buildUserProto(user))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("Successfully returned user data for email: {}", request.getEmail());
    }


    /**
     * Build User proto from UserResponse DTO
     */
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

    /**
     * Validate CreateUserRequest
     */
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

    /**
     * Validate UpdateUserRequest
     */
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

    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        return email != null &&
                email.contains("@") &&
                email.contains(".") &&
                email.length() > 5;
    }

    /**
     * Convert entity status to proto status
     */
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
