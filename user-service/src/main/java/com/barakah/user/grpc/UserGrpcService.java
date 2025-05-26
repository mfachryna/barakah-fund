package com.barakah.user.grpc;


import com.barakah.common.proto.v1.PageResponse;
import com.barakah.user.dto.UserResponse;
import com.barakah.user.proto.v1.*;

import com.barakah.user.service.UserService;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

import com.barakah.auth.context.UserContext;
import com.barakah.auth.context.UserContextHolder;

@Slf4j
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    public UserGrpcService(UserService userService) {
        this.userService = userService;
    }

//    @Override
//    public void getUserById(GetUserByIdRequest request, StreamObserver<UserResponse> responseObserver) {
//        log.info("Getting user by ID: {}", request.getUserId());
//
//        try {
//
//            UserResponse response = UserResponse.newBuilder()
//                    .setUserId(request.getUserId())
//                    .setUsername("test-user-" + request.getUserId())
//                    .setEmail("user" + request.getUserId() + "@barakah.com")
//                    .setFirstName("Test")
//                    .setLastName("User")
//                    .setStatus(Status.STATUS_ACTIVE)
//                    .setPhoneNumber("+1234567890")
//                    .build();
//
//            responseObserver.onNext(response);
//            responseObserver.onCompleted();
//
//            log.info("Successfully returned user data for ID: {}", request.getUserId());
//        } catch (Exception e) {
//            log.error("Error getting user by ID: {}", e.getMessage(), e);
//            responseObserver.onError(
//                    io.grpc.Status.INTERNAL
//                            .withDescription("Failed to get user: " + e.getMessage())
//                            .asRuntimeException()
//            );
//        }
//    }
    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        log.info("Creating user: {}", request.getUsername());

        try {

            if (request.getUsername().isEmpty()) {
                responseObserver.onError(
                        io.grpc.Status.INVALID_ARGUMENT
                                .withDescription("Username cannot be empty")
                                .asRuntimeException()
                );
                return;
            }
            ;
            String generatedId = "user_" + System.currentTimeMillis();

            UserResponse result = userService.createUser(request);
            CreateUserResponse response = CreateUserResponse.newBuilder()
                    .setUser(
                            User.newBuilder()
                                    .setUserId(result.getUserId())
                                    .setUsername(result.getUsername())
                                    .setEmail(result.getEmail())
                                    .setFirstName(result.getFirstName())
                                    .setLastName(result.getLastName())
                                    .setPhoneNumber(result.getPhoneNumber() != null ? result.getPhoneNumber() : "")
                                    .setStatus(convertToProtoStatus(result.getStatus()))
                                    .setCreatedAt(result.getCreatedAt() != null ? result.getCreatedAt().toString() : "")
                                    .setUpdatedAt(result.getUpdatedAt() != null ? result.getUpdatedAt().toString() : "")
                                    .build()
                    )
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully created user: {} with ID: {}", request.getUsername(), generatedId);
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to create user: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void updateUser(UpdateUserRequest request, StreamObserver<UpdateUserResponse> responseObserver) {
        log.info("Updating user: {}", request.getUserId());

        try {
            UserResponse result = userService.updateUser(request);

            UpdateUserResponse response = UpdateUserResponse.newBuilder()
                    .setUser(
                            User.newBuilder()
                                    .setUserId(result.getUserId())
                                    .setUsername(result.getUsername())
                                    .setEmail(result.getEmail())
                                    .setFirstName(result.getFirstName())
                                    .setLastName(result.getLastName())
                                    .setPhoneNumber(result.getPhoneNumber() != null ? result.getPhoneNumber() : "")
                                    .setStatus(convertToProtoStatus(result.getStatus()))
                                    .setCreatedAt(result.getCreatedAt() != null ? result.getCreatedAt().toString() : "")
                                    .setUpdatedAt(result.getUpdatedAt() != null ? result.getUpdatedAt().toString() : "")
                                    .build()
                    )
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully updated user: {}", request.getUserId());
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage(), e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to update user: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
        log.info("Deleting user: {}", request.getUserId());

        try {

            DeleteUserResponse response = DeleteUserResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("User deleted successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully deleted user: {}", request.getUserId());
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage(), e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to delete user: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        UserContext currentUser = UserContextHolder.getContext();
        System.out.println("Current user: Anonymous");
        System.out.println(currentUser);
        System.out.println("Current user: Anonymous");
        int page = request.getPageRequest().getPage();
        int size = request.getPageRequest().getSize();
        log.info("Listing users with page: {}, size: {}", page, size);

        try {

            Pageable pageable = PageRequest.of(
                    page > 0 ? page - 1 : 0,
                    size > 0 ? size : 10
            );

            Page<UserResponse> userPage = userService.getAllUsers(pageable);

            ListUsersResponse.Builder responseBuilder = ListUsersResponse.newBuilder();

            for (UserResponse userDto : userPage.getContent()) {
                User protoUser = User.newBuilder()
                        .setUserId(userDto.getUserId())
                        .setUsername(userDto.getUsername())
                        .setEmail(userDto.getEmail())
                        .setFirstName(userDto.getFirstName())
                        .setLastName(userDto.getLastName())
                        .setPhoneNumber(userDto.getPhoneNumber() != null ? userDto.getPhoneNumber() : "")
                        .setStatus(convertToProtoStatus(userDto.getStatus()))
                        .setCreatedAt(userDto.getCreatedAt() != null ? userDto.getCreatedAt().toString() : "")
                        .setUpdatedAt(userDto.getUpdatedAt() != null ? userDto.getUpdatedAt().toString() : "")
                        .build();

                responseBuilder.addUsers(protoUser);
            }

            responseBuilder.setPageResponse(
                    PageResponse.newBuilder().setPage(page)
                            .setSize(size)
                            .setTotalElements(userPage.getTotalElements())
                            .setFirst(userPage.isFirst())
                            .setFirst(userPage.isLast())
                            .setTotalPages(userPage.getTotalPages())
                            .build()
            );
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            log.info("Successfully returned {} users (page {}/{})",
                    userPage.getNumberOfElements(),
                    request.getPageRequest().getPage(),
                    userPage.getTotalPages());

        } catch (Exception e) {
            log.error("Error listing users: {}", e.getMessage(), e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to list users: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void getUserByUsername(GetUserByUsernameRequest request, StreamObserver<GetUserByUsernameResponse> responseObserver) {
        log.info("Getting user by username: {}", request.getUsername());

        try {

            UserResponse user = userService.getUserByUsername(request.getUsername());

            GetUserByUsernameResponse response = GetUserByUsernameResponse.newBuilder()
                    .setUser(
                            User.newBuilder()
                                    .setUserId(user.getUserId())
                                    .setUsername(user.getUsername())
                                    .setEmail(user.getEmail())
                                    .setFirstName(user.getFirstName())
                                    .setLastName(user.getLastName())
                                    .setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                                    .setStatus(convertToProtoStatus(user.getStatus()))
                                    .setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "")
                                    .setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "")
                                    .build()
                    )
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully returned user data for username: {}", request.getUsername());
        } catch (Exception e) {
            log.error("Error getting user by username: {}", e.getMessage(), e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to get user: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void getUserByEmail(GetUserByEmailRequest request, StreamObserver<GetUserByEmailResponse> responseObserver) {
        log.info("Getting user by email: {}", request.getEmail());

        try {

            UserResponse user = userService.getUserByEmail(request.getEmail());

            GetUserByEmailResponse response = GetUserByEmailResponse.newBuilder()
                    .setUser(
                            User.newBuilder()
                                    .setUserId(user.getUserId())
                                    .setUsername(user.getUsername())
                                    .setEmail(user.getEmail())
                                    .setFirstName(user.getFirstName())
                                    .setLastName(user.getLastName())
                                    .setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                                    .setStatus(convertToProtoStatus(user.getStatus()))
                                    .setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "")
                                    .setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "")
                                    .build()
                    )
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully returned user data for email: {}", request.getEmail());
        } catch (Exception e) {
            log.error("Error getting user by email: {}", e.getMessage(), e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to get user: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    private UserStatus convertToProtoStatus(com.barakah.user.entity.UserStatus entityStatus) {
        if (entityStatus == null) {
            return UserStatus.USER_STATUS_ACTIVE;
        }

        switch (entityStatus) {
            case ACTIVE:
                return UserStatus.USER_STATUS_ACTIVE;
            case INACTIVE:
                return UserStatus.USER_STATUS_INACTIVE;
            case SUSPENDED:
                return UserStatus.USER_STATUS_SUSPENDED;
            case LOCKED:
                return UserStatus.USER_STATUS_LOCKED;
            default:
                return UserStatus.USER_STATUS_ACTIVE;
        }
    }
}
