package com.barakah.user.service;

import com.barakah.user.dto.UserResponse;
import com.barakah.user.entity.User;
import com.barakah.user.entity.UserStatus;
import com.barakah.user.mapper.UserMapper;
import com.barakah.user.proto.v1.*;
import com.barakah.user.repository.UserRepository;
import com.barakah.user.exception.UserExceptions;
import com.barakah.user.exception.KeycloakExceptions;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
            throw new UserExceptions.UserAlreadyExistsException("username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserExceptions.UserAlreadyExistsException("email", request.getEmail());
        }

        String keycloakId = "";
        try {

            keycloakId = keycloakUserService.createUser(request);

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
            user.setDateOfBirth(
                    !request.getDateOfBirth().isEmpty() ?
                            LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay() : null);
            keycloakUserService.assignRole(keycloakId, "USER");

            User savedUser = userRepository.save(user);

            log.info("User created successfully: {} with Keycloak ID: {}", savedUser.getUsername(), keycloakId);
            return userMapper.toResponse(savedUser);
        } catch (KeycloakExceptions.KeycloakUserCreationException e) {
            this.deleteUser(keycloakId);
            throw e; 
        } catch (Exception e) {
            this.deleteUser(keycloakId);
            log.error("Failed to create user: {}", e.getMessage());
            throw new UserExceptions.UserCreationFailedException(e.getMessage(), e);
        }
    }


    @Transactional
    public void updateLastLoginByKeycloakId(String keycloakId) {
        LocalDateTime loginTime = LocalDateTime.now();
        log.debug("Updating last login by Keycloak ID: {} at {}", keycloakId, loginTime);
        
        try {
            User user = userRepository.findByKeycloakId(keycloakId)
                    .orElseThrow(() -> new UserExceptions.UserNotFoundException("Keycloak ID", keycloakId));
            
            user.setLastLogin(loginTime);
            userRepository.save(user);
            
            log.info("Last login updated for user: {} (Keycloak ID: {}) at {}", 
                    user.getUsername(), keycloakId, loginTime);
            
        } catch (UserExceptions.UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update last login for Keycloak ID {}: {}", keycloakId, e.getMessage());
            throw new UserExceptions.UserUpdateFailedException("last login update: " + e.getMessage(), e);
        }
    }
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("ID", userId));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public User getUserByIdData(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("username", username));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsernameOptional(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("email", email));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmailOptional(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getUserByKeycloakIdData(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("Keycloak ID", keycloakId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("Keycloak ID", keycloakId));

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

    public UserResponse updateUser(UpdateUserRequest request, String userId) {
        User user = getUserByKeycloakIdData(userId);


        if (!request.getEmail().isEmpty()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserExceptions.UserAlreadyExistsException("email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        user.setFirstName(request.getFirstName().isEmpty() ? user.getFirstName() : request.getFirstName());
        user.setLastName(request.getLastName().isEmpty() ? user.getLastName() : request.getLastName()); 
        user.setPhoneNumber(request.getPhoneNumber().isEmpty() ? user.getPhoneNumber() : request.getPhoneNumber()); 
        user.setAddress(request.getAddress().isEmpty() ? user.getAddress() : request.getAddress()); 
        user.setDateOfBirth(
                !request.getDateOfBirth().isEmpty() ?
                        LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay() : user.getDateOfBirth());

        if (user.getKeycloakId() != null) {
            keycloakUserService.updateUser(user.getKeycloakId(), request);
        }

        try {
            User savedUser = userRepository.save(user);
            log.info("User updated successfully: {}", savedUser.getUsername());
            return userMapper.toResponse(savedUser);

        } catch (Exception e) {
            log.error("Failed to update user: {}", e.getMessage());
            throw new UserExceptions.UserUpdateFailedException(e.getMessage(), e);
        }
    }

    public void updateUserStatus(String userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("ID", userId));

        
        if (user.getStatus() == UserStatus.LOCKED && status != UserStatus.ACTIVE) {
            throw new UserExceptions.InvalidUserStatusException(
                    userId, user.getStatus().toString(), "change status to " + status);
        }

        user.setStatus(status);

        try {
            userRepository.save(user);

            if (user.getKeycloakId() != null) {
                keycloakUserService.updateUserStatus(user.getKeycloakId(), status == UserStatus.ACTIVE);
            }

            log.info("User status updated: {} -> {}", user.getUsername(), status);

        } catch (Exception e) {
            log.error("Failed to update user status: {}", e.getMessage());
            throw new UserExceptions.UserUpdateFailedException("status update: " + e.getMessage(), e);
        }
    }

    public boolean deleteUser(String userId) {
        try {
            Optional<User> user = userRepository.findById(userId);

            if (user.isPresent()) {
                
                if (user.get().getKeycloakId() != null) {
                    keycloakUserService.deleteUser(user.get().getKeycloakId());
                }

                
                userRepository.delete(user.get());
            } else {
                
                keycloakUserService.deleteUser(userId);
            }

            log.info("User deleted successfully: {}", userId);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete user: {}", e.getMessage());
            throw new UserExceptions.UserDeletionFailedException(e.getMessage(), e);
        }
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
