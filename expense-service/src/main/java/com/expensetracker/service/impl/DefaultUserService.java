package com.expensetracker.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.expensetracker.models.User;
import com.expensetracker.repositories.UserRepository;
import com.expensetracker.services.UserService;

/**
 * Default implementation of {@link UserService}.
 *
 * <p>
 * Provides application-level user management operations
 * by delegating persistence to {@link UserRepository}.
 */
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;

    /**
     * Creates a user service backed by the specified repository.
     *
     * @param userRepository the repository used to manage users
     * @throws NullPointerException if userRepository is null
     */
    public DefaultUserService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public User addUser(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getId() != null) {
            throw new IllegalArgumentException("New user must not have an id");
        }
        return this.userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        return this.userRepository.findById(userId);
    }

    @Override
    public List<User> getAllUsers() {
        return this.userRepository.findAll();
    }

    @Override
    public User updateUser(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return this.userRepository.update(user);
    }

    @Override
    public boolean deleteUser(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        return this.userRepository.deleteById(userId);
    }
}
