package com.expensetracker.services;

import java.util.List;
import java.util.Optional;

import com.expensetracker.models.User;

/**
 * Defines application-level operations for managing users.
 *
 * <p>
 * Provides functionality for creating, retrieving, updating, and deleting
 * users. Implementations coordinate user management through the persistence
 * layer while preserving applicable business rules.
 *
 * <p>
 * A user with an associated expense profile cannot be deleted until
 * the expense profile has been removed.
 */
public interface UserService {

    /**
     * Creates a new user.
     *
     * @param user the user to create; must not be null and must not have an ID
     * @return the created user, including the generated ID
     * @throws NullPointerException     if user is null
     * @throws IllegalArgumentException if user already has an ID
     */
    User addUser(User user);

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the ID of the user to retrieve; must be positive
     * @return an Optional containing the user if found, or an empty Optional
     *         otherwise
     * @throws IllegalArgumentException if userId is not positive
     */
    Optional<User> getUserById(long userId);

    /**
     * Retrieves all users.
     *
     * @return a list of all users, or an empty list if none exist
     */
    List<User> getAllUsers();

    /**
     * Updates an existing user's information.
     *
     * @param user the user containing the updated information; must not be null
     *             and must have an ID
     * @return the updated user
     * @throws NullPointerException     if user is null
     * @throws IllegalArgumentException if user has no ID or does not exist
     */
    User updateUser(User user);

    /**
     * Deletes a user by their ID.
     *
     * <p>
     * Users with associated expense profiles cannot be deleted.
     * The expense profile must be deleted before deleting the user.
     *
     * @param userId the ID of the user to delete; must be positive
     * @return true if the user was deleted, or false if the user did not exist
     * @throws IllegalArgumentException if userId is not positive
     */
    boolean deleteUser(long userId);
}
