package com.barakah.user.service;

import com.barakah.shared.annotation.RateLimit;
import com.barakah.user.dto.UserResponse;
import com.barakah.user.entity.User;
import com.barakah.user.entity.UserStatus;
import com.barakah.user.mapper.UserMapper;
import com.barakah.user.proto.v1.*;
import com.barakah.user.repository.UserRepository;
import com.barakah.user.exception.UserExceptions;
import com.barakah.user.exception.KeycloakExceptions;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    @RateLimit(endpoint = "create-user")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackCreateUser")
    @Bulkhead(name = "user-operations")
    @RateLimiter(name = "user-creation")
    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "user-stats", allEntries = true),
        @CacheEvict(value = "active-users", allEntries = true)
    })
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        validateUserCreation(request);

        String keycloakId = "";
        try {

            keycloakId = createKeycloakUserWithResilience(request);

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
                    !request.getDateOfBirth().isEmpty()
                    ? LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay() : null);

            assignKeycloakRoleWithResilience(keycloakId, "USER");

            User savedUser = saveUserWithRetry(user);

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

    @CircuitBreaker(name = "database", fallbackMethod = "fallbackUpdateLastLogin")
    @RateLimiter(name = "user-queries")
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#keycloakId"),
        @CacheEvict(value = "user-by-keycloak", key = "#keycloakId")
    })
    @Transactional
    public void updateLastLoginByKeycloakId(String keycloakId) {
        LocalDateTime loginTime = LocalDateTime.now();
        log.debug("Updating last login by Keycloak ID: {} at {}", keycloakId, loginTime);

        try {
            User user = userRepository.findByKeycloakId(keycloakId)
                    .orElseThrow(() -> new UserExceptions.UserNotFoundException("Keycloak ID", keycloakId));

            user.setLastLogin(loginTime);
            saveUserWithRetry(user);

            log.info("Last login updated for user: {} (Keycloak ID: {}) at {}",
                    user.getUsername(), keycloakId, loginTime);

        } catch (UserExceptions.UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update last login for Keycloak ID {}: {}", keycloakId, e.getMessage());
            throw new UserExceptions.UserUpdateFailedException("last login update: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "database", fallbackMethod = "fallbackGetUser")
    @RateLimiter(name = "user-queries")
    @Cacheable(value = "users", key = "#userId")
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("ID", userId));

        return userMapper.toResponse(user);
    }

    @CircuitBreaker(name = "database")
    @Cacheable(value = "user-data", key = "#userId")
    @Transactional(readOnly = true)
    public User getUserByIdData(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }

    @CircuitBreaker(name = "database", fallbackMethod = "fallbackGetUserByUsername")
    @RateLimiter(name = "user-queries")
    @Cacheable(value = "user-by-username", key = "#username")
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("username", username));

        return userMapper.toResponse(user);
    }

    @CircuitBreaker(name = "database")
    @Cacheable(value = "user-optional-username", key = "#username")
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsernameOptional(String username) {
        return userRepository.findByUsername(username);
    }

    @CircuitBreaker(name = "database", fallbackMethod = "fallbackGetUserByEmail")
    @RateLimiter(name = "user-queries")
    @Cacheable(value = "user-by-email", key = "#email")
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("email", email));

        return userMapper.toResponse(user);
    }

    @CircuitBreaker(name = "database")
    @Cacheable(value = "user-optional-email", key = "#email")
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmailOptional(String email) {
        return userRepository.findByEmail(email);
    }

    @CircuitBreaker(name = "database")
    @Cacheable(value = "user-by-keycloak", key = "#keycloakId")
    @Transactional(readOnly = true)
    public User getUserByKeycloakIdData(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("Keycloak ID", keycloakId));
    }

    @CircuitBreaker(name = "database", fallbackMethod = "fallbackGetUserByKeycloakId")
    @RateLimiter(name = "user-queries")
    @Cacheable(value = "users", key = "'keycloak:' + #keycloakId")
    @Transactional(readOnly = true)
    public UserResponse getUserByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("Keycloak ID", keycloakId));

        return userMapper.toResponse(user);
    }

    @RateLimiter(name = "user-queries")
    @CircuitBreaker(name = "database")
    @Cacheable(value = "users", key = "'all-page:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @RateLimiter(name = "user-queries")
    @CircuitBreaker(name = "database")
    @Cacheable(value = "active-users", key = "'active-page:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<UserResponse> getActiveUsers(Pageable pageable) {
        return userRepository.findActiveUsers(pageable)
                .map(userMapper::toResponse);
    }

    @RateLimiter(name = "user-queries")
    @CircuitBreaker(name = "database")
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable)
                .map(userMapper::toResponse);
    }

    @RateLimit(endpoint = "update-user")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackUpdateUser")
    @Bulkhead(name = "user-operations")
    @RateLimiter(name = "user-queries")
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#userId"),
        @CacheEvict(value = "user-by-keycloak", key = "#userId"),
        @CacheEvict(value = "user-by-email", allEntries = true),
        @CacheEvict(value = "user-by-username", allEntries = true),
        @CacheEvict(value = "users", allEntries = true)
    })
    public UserResponse updateUser(UpdateUserRequest request, String userId) {
        User user = getUserByKeycloakIdData(userId);

        validateUserUpdate(request, user);

        updateUserFields(user, request);

        if (user.getKeycloakId() != null) {
            updateKeycloakUserWithResilience(user.getKeycloakId(), request);
        }

        try {
            User savedUser = saveUserWithRetry(user);
            log.info("User updated successfully: {}", savedUser.getUsername());
            return userMapper.toResponse(savedUser);

        } catch (Exception e) {
            log.error("Failed to update user: {}", e.getMessage());
            throw new UserExceptions.UserUpdateFailedException(e.getMessage(), e);
        }
    }

    @RateLimit(endpoint = "update-user-status")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackUpdateUserStatus")
    @Bulkhead(name = "user-operations")
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#userId"),
        @CacheEvict(value = "active-users", allEntries = true),
        @CacheEvict(value = "user-stats", allEntries = true)
    })
    public void updateUserStatus(String userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserExceptions.UserNotFoundException("ID", userId));

        validateStatusChange(user, status);

        user.setStatus(status);

        try {
            saveUserWithRetry(user);

            if (user.getKeycloakId() != null) {
                updateKeycloakUserStatusWithResilience(user.getKeycloakId(), status == UserStatus.ACTIVE);
            }

            log.info("User status updated: {} -> {}", user.getUsername(), status);

        } catch (Exception e) {
            log.error("Failed to update user status: {}", e.getMessage());
            throw new UserExceptions.UserUpdateFailedException("status update: " + e.getMessage(), e);
        }
    }

    @RateLimit(endpoint = "delete-user")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackDeleteUser")
    @Bulkhead(name = "user-operations")
    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "user-by-email", allEntries = true),
        @CacheEvict(value = "user-by-username", allEntries = true),
        @CacheEvict(value = "user-by-keycloak", allEntries = true),
        @CacheEvict(value = "active-users", allEntries = true),
        @CacheEvict(value = "user-stats", allEntries = true)
    })
    public boolean deleteUser(String userId) {
        try {
            Optional<User> user = userRepository.findById(userId);

            if (user.isPresent()) {

                if (user.get().getKeycloakId() != null) {
                    deleteKeycloakUserWithResilience(user.get().getKeycloakId());
                }

                deleteUserWithRetry(user.get());
            } else {

                deleteKeycloakUserWithResilience(userId);
            }

            log.info("User deleted successfully: {}", userId);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete user: {}", e.getMessage());
            throw new UserExceptions.UserDeletionFailedException(e.getMessage(), e);
        }
    }

    @RateLimiter(name = "user-queries")
    @CircuitBreaker(name = "database")
    @Cacheable(value = "recent-users", key = "#days")
    @Transactional(readOnly = true)
    public List<UserResponse> getRecentUsers(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return userMapper.toResponseList(userRepository.findRecentUsers(since));
    }

    @CircuitBreaker(name = "database")
    @Cacheable(value = "user-stats", key = "'total-count'")
    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }

    @CircuitBreaker(name = "database")
    @Cacheable(value = "user-stats", key = "'active-count'")
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackKeycloakCreateUser")
    @RateLimiter(name = "keycloak-operations")
    @Retry(name = "keycloak")
    private String createKeycloakUserWithResilience(CreateUserRequest request) {
        return keycloakUserService.createUser(request);
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackKeycloakAssignRole")
    @RateLimiter(name = "keycloak-operations")
    @Retry(name = "keycloak")
    private void assignKeycloakRoleWithResilience(String keycloakId, String role) {
        keycloakUserService.assignRole(keycloakId, role);
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackKeycloakUpdateUser")
    @RateLimiter(name = "keycloak-operations")
    @Retry(name = "keycloak")
    private void updateKeycloakUserWithResilience(String keycloakId, UpdateUserRequest request) {
        keycloakUserService.updateUser(keycloakId, request);
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackKeycloakUpdateStatus")
    @RateLimiter(name = "keycloak-operations")
    @Retry(name = "keycloak")
    private void updateKeycloakUserStatusWithResilience(String keycloakId, boolean enabled) {
        keycloakUserService.updateUserStatus(keycloakId, enabled);
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallbackKeycloakDeleteUser")
    @RateLimiter(name = "keycloak-operations")
    private void deleteKeycloakUserWithResilience(String keycloakId) {
        keycloakUserService.deleteUser(keycloakId);
    }

    @Retry(name = "database")
    private User saveUserWithRetry(User user) {
        return userRepository.save(user);
    }

    @Retry(name = "database")
    private void deleteUserWithRetry(User user) {
        userRepository.delete(user);
    }

    private void validateUserCreation(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserExceptions.UserAlreadyExistsException("username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserExceptions.UserAlreadyExistsException("email", request.getEmail());
        }
    }

    private void validateUserUpdate(UpdateUserRequest request, User user) {
        if (!request.getEmail().isEmpty() && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserExceptions.UserAlreadyExistsException("email", request.getEmail());
            }
        }
    }

    private void validateStatusChange(User user, UserStatus status) {
        if (user.getStatus() == UserStatus.LOCKED && status != UserStatus.ACTIVE) {
            throw new UserExceptions.InvalidUserStatusException(
                    user.getUserId(), user.getStatus().toString(), "change status to " + status);
        }
    }

    private void updateUserFields(User user, UpdateUserRequest request) {
        if (!request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }
        user.setFirstName(request.getFirstName().isEmpty() ? user.getFirstName() : request.getFirstName());
        user.setLastName(request.getLastName().isEmpty() ? user.getLastName() : request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber().isEmpty() ? user.getPhoneNumber() : request.getPhoneNumber());
        user.setAddress(request.getAddress().isEmpty() ? user.getAddress() : request.getAddress());
        user.setDateOfBirth(
                !request.getDateOfBirth().isEmpty()
                ? LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay() : user.getDateOfBirth());
    }

    public UserResponse fallbackCreateUser(CreateUserRequest request, Exception ex) {
        log.error("🚨 User creation fallback triggered - User: {}, Error: {}", request.getUsername(), ex.getMessage());
        throw new UserExceptions.UserCreationFailedException(
                "User service is temporarily unavailable. Please try again later.", ex);
    }

    public UserResponse fallbackGetUser(String userId, Exception ex) {
        log.error("🚨 Get user fallback triggered - User: {}, Error: {}", userId, ex.getMessage());
        return null;
    }

    public UserResponse fallbackGetUserByUsername(String username, Exception ex) {
        log.error("🚨 Get user by username fallback triggered - Username: {}, Error: {}", username, ex.getMessage());
        return null;
    }

    public UserResponse fallbackGetUserByEmail(String email, Exception ex) {
        log.error("🚨 Get user by email fallback triggered - Email: {}, Error: {}", email, ex.getMessage());
        return null;
    }

    public UserResponse fallbackGetUserByKeycloakId(String keycloakId, Exception ex) {
        log.error("🚨 Get user by Keycloak ID fallback triggered - ID: {}, Error: {}", keycloakId, ex.getMessage());
        return null;
    }

    public UserResponse fallbackUpdateUser(UpdateUserRequest request, String userId, Exception ex) {
        log.error("🚨 Update user fallback triggered - User: {}, Error: {}", userId, ex.getMessage());
        throw new UserExceptions.UserUpdateFailedException(
                "User update service is temporarily unavailable. Please try again later.", ex);
    }

    public void fallbackUpdateUserStatus(String userId, UserStatus status, Exception ex) {
        log.error("🚨 Update user status fallback triggered - User: {}, Status: {}, Error: {}",
                userId, status, ex.getMessage());
        throw new UserExceptions.UserUpdateFailedException(
                "User status update service is temporarily unavailable. Please try again later.", ex);
    }

    public void fallbackUpdateLastLogin(String keycloakId, Exception ex) {
        log.warn("🚨 Update last login fallback triggered - Keycloak ID: {}, Error: {}",
                keycloakId, ex.getMessage());

    }

    public boolean fallbackDeleteUser(String userId, Exception ex) {
        log.error("🚨 Delete user fallback triggered - User: {}, Error: {}", userId, ex.getMessage());
        throw new UserExceptions.UserDeletionFailedException(
                "User deletion service is temporarily unavailable. Please try again later.", ex);
    }

    public String fallbackKeycloakCreateUser(CreateUserRequest request, Exception ex) {
        log.error("🚨 Keycloak create user fallback triggered - User: {}, Error: {}",
                request.getUsername(), ex.getMessage());
        throw new KeycloakExceptions.KeycloakConnectionException(
                "Authentication service is temporarily unavailable. Please try again later.");
    }

    public void fallbackKeycloakAssignRole(String keycloakId, String role, Exception ex) {
        log.error("🚨 Keycloak assign role fallback triggered - User: {}, Role: {}, Error: {}",
                keycloakId, role, ex.getMessage());

    }

    public void fallbackKeycloakUpdateUser(String keycloakId, UpdateUserRequest request, Exception ex) {
        log.error("🚨 Keycloak update user fallback triggered - User: {}, Error: {}",
                keycloakId, ex.getMessage());

    }

    public void fallbackKeycloakUpdateStatus(String keycloakId, boolean enabled, Exception ex) {
        log.error("🚨 Keycloak update status fallback triggered - User: {}, Enabled: {}, Error: {}",
                keycloakId, enabled, ex.getMessage());

    }

    public void fallbackKeycloakDeleteUser(String keycloakId, Exception ex) {
        log.error("🚨 Keycloak delete user fallback triggered - User: {}, Error: {}",
                keycloakId, ex.getMessage());

    }
}
