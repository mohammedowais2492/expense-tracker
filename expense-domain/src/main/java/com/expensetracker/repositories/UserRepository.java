package com.expensetracker.repositories;

import java.util.List;
import java.util.Optional;

import com.expensetracker.models.User;

/**
 * Defines the persistence operations available for managing users.
 *
 * <p>
 * Implementations are responsible for storing and retrieving users
 * while keeping persistence-specific details hidden from the application.
 */
public interface UserRepository {

    /**
     * Persists a new user.
     *
     * @param user the user to persist
     * @return the persisted user, including its generated identifier
     */
    User save(User user);

    /**
     * Retrieves a user by its identifier.
     *
     * @param id the user identifier
     * @return an Optional containing the user if found, or an empty Optional
     *         otherwise
     */
    Optional<User> findById(long id);

    /**
     * Retrieves all users.
     *
     * @return a list of all users, or an empty list if no users exist
     */
    List<User> findAll();

    /**
     * Updates an existing user.
     *
     * @param user the user containing the updated information
     * @return the updated user
     * @throws IllegalArgumentException if the user does not exist
     */
    User update(User user);

    /**
     * Deletes a user by its identifier.
     *
     * @param id the identifier of the user to delete
     * @return true if the user was deleted, or false if no matching user existed
     */
    boolean deleteById(long id);
}