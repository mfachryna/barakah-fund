package com.barakah.user.service;

import com.barakah.user.dto.UserResponse;
import com.barakah.user.entity.User;
import com.barakah.user.entity.UserStatus;
import com.barakah.user.exception.UserAlreadyExistsException;
import com.barakah.user.exception.UserNotFoundException;
import com.barakah.user.mapper.UserMapper;
import com.barakah.user.proto.v1.*;
import com.barakah.user.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakUserService keycloakUserService;

    public UserService(UserRepository userRepository,
            UserMapper userMapper,
            KeycloakUserService keycloakUserService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.keycloakUserService = keycloakUserService;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        String keycloakId = "";
        try {

            keycloakId = keycloakUserService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPassword()
            );

            com.barakah.user.dto.CreateUserRequest createUserRequest
                    = new com.barakah.user.dto.CreateUserRequest(
                            request.getUsername(),
                            request.getEmail(),
                            request.getFirstName(),
                            request.getLastName(),
                            request.getPassword(),
                            request.getPhoneNumber(),
                            null,
                            request.getAddress()
                    );

            User user = new User();
            userMapper.updateEntity(createUserRequest, user);
            user.setKeycloakId(keycloakId);

            keycloakUserService.assignRole(keycloakId, "USER");

            User savedUser = userRepository.save(user);

            log.info("User created successfully: {} with Keycloak ID: {}", savedUser.getUsername(), keycloakId);
            return userMapper.toResponse(savedUser);
        } catch (Exception e) {
            e.printStackTrace();
            this.deleteUser(keycloakId);
            log.error("Failed to create user: {}", e.getMessage());
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("User not found with Keycloak ID: " + keycloakId));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getActiveUsers(Pageable pageable) {
        return userRepository.findActiveUsers(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable)
                .map(userMapper::toResponse);
    }

    public UserResponse updateUser(UpdateUserRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + request.getUserId()));

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        com.barakah.user.dto.CreateUserRequest updateUserRequest
                = new com.barakah.user.dto.CreateUserRequest(
                        "",
                        request.getEmail(),
                        request.getFirstName(),
                        request.getLastName(),
                        "",
                        request.getPhoneNumber(),
                        LocalDateTime.parse(request.getDateOfBirth()),
                        request.getAddress()
                );

        userMapper.updateEntity(updateUserRequest, user);

        if (user.getKeycloakId() != null) {
            keycloakUserService.updateUser(user.getKeycloakId(), request);
        }

        User savedUser = userRepository.save(user);
        log.info("User updated successfully: {}", savedUser.getUsername());

        return userMapper.toResponse(savedUser);
    }

    public void updateUserStatus(String userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setStatus(status);
        userRepository.save(user);

        if (user.getKeycloakId() != null) {
            keycloakUserService.updateUserStatus(user.getKeycloakId(), status == UserStatus.ACTIVE);
        }

        log.info("User status updated: {} -> {}", user.getUsername(), status);
    }

    public void updateLastLogin(String userId) {
        userRepository.updateLastLogin(userId, LocalDateTime.now());
        log.debug("Updated last login for user: {}", userId);
    }

    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        keycloakUserService.deleteUser(userId);

        if (user.getKeycloakId() != null) {
            keycloakUserService.deleteUser(user.getKeycloakId());
        }

        userRepository.delete(user);

        log.info("User deleted successfully: {}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getRecentUsers(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return userMapper.toResponseList(userRepository.findRecentUsers(since));
    }

    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

}
