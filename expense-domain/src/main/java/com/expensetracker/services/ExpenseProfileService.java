package com.expensetracker.services;

import java.util.Optional;

import com.expensetracker.models.ExpenseProfile;

/**
 * Defines application-level operations for managing expense profiles.
 *
 * <p>
 * Provides functionality for creating, retrieving, updating, and deleting
 * expense profiles. Each user may own at most one expense profile.
 *
 * <p>
 * Deleting an expense profile also deletes all financial transactions
 * associated with that profile. This operation is irreversible unless the
 * deleted data has been backed up.
 */
public interface ExpenseProfileService {

    /**
     * Creates a new expense profile for a user.
     *
     * @param profile the profile to create; must not be null and must not
     *                have an ID
     * @return the created profile, including its generated ID
     * @throws NullPointerException     if profile is null
     * @throws IllegalArgumentException if profile already has an ID
     */
    ExpenseProfile createProfile(ExpenseProfile profile);

    /**
     * Retrieves the expense profile belonging to the specified user.
     *
     * @param userId the ID of the user; must be positive
     * @return an Optional containing the user's expense profile if one
     *         exists, or an empty Optional otherwise
     * @throws IllegalArgumentException if userId is not positive
     */
    Optional<ExpenseProfile> getProfileByUserId(long userId);

    /**
     * Updates an existing expense profile.
     *
     * <p>
     * The profile's name may be changed, but its owning user cannot
     * be changed.
     *
     * @param profile the profile containing the updated information;
     *                must not be null and must have an ID
     * @return the updated expense profile
     * @throws NullPointerException     if profile is null
     * @throws IllegalArgumentException if profile has no ID or does not exist
     */
    ExpenseProfile updateProfile(ExpenseProfile profile);

    /**
     * Deletes an expense profile by its ID.
     *
     * <p>
     * All financial transactions belonging to the profile are also
     * permanently deleted. The associated user is not deleted.
     *
     * @param profileId the ID of the profile to delete; must be positive
     * @return true if the profile was deleted, or false if no profile
     *         exists with the specified ID
     * @throws IllegalArgumentException if profileId is not positive
     */
    boolean deleteProfile(long profileId);
}
