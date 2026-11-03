
package com.expensetracker.repositories;

import com.expensetracker.models.ExpenseProfile;
import java.util.Optional;

/**
 * Defines persistence operations for {@link ExpenseProfile} entities.
 *
 * <p>
 * This interface abstracts the underlying persistence mechanism from
 * the application and service layers.
 * </p>
 */
public interface ExpenseProfileRepository {

    /**
     * Persists a new expense profile.
     *
     * @param profile the expense profile to persist
     * @return the persisted profile, including its generated ID
     * @throws IllegalArgumentException if a profile for the user already exists
     */
    ExpenseProfile save(ExpenseProfile profile);

    /**
     * Finds the expense profile associated with a user.
     *
     * @param userId the ID of the user
     * @return an {@link Optional} containing the profile if one exists,
     *         or an empty {@link Optional} otherwise
     */
    Optional<ExpenseProfile> findByUserId(long userId);

    /**
     * Updates an existing expense profile.
     *
     * @param profile the expense profile containing the updated values
     * @return the updated expense profile
     * @throws IllegalArgumentException if the profile does not exist
     */
    ExpenseProfile update(ExpenseProfile profile);

    /**
     * Deletes an expense profile by its ID.
     *
     * @param id the ID of the profile to delete
     * @return {@code true} if the profile was deleted, or {@code false}
     *         if no profile with the given ID exists
     */
    boolean deleteById(long id);
}
